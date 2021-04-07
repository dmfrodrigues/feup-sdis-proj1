import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;

public class Peer implements PeerInterface {
    /**
     * Initially reserved storage for backing up chunks (in bytes).
     */
    private static final int INITIAL_STORAGE_SIZE = 1000000000;

    private final DatagramSocket sendSocket;

    private final String version;
    private final int id;

    private final InetSocketAddress controlAddress;
    private final InetSocketAddress dataBroadcastAddress;
    private final InetSocketAddress dataRecoveryAddress;

    private final MulticastSocket controlSocket;
    private final MulticastSocket dataBroadcastSocket;
    private final MulticastSocket dataRecoverySocket;

    private final FileTable fileTable;
    private final ChunkStorageManager storageManager;
    private final ControlSocketHandler controlSocketHandler;
    private final DataBroadcastSocketHandler dataBroadcastSocketHandler;
    private final DataRecoverySocketHandler dataRecoverySocketHandler;

    public Peer(
            String version,
            int id,
            InetSocketAddress controlAddress,
            InetSocketAddress dataBroadcastAddress,
            InetSocketAddress dataRecoveryAddress
    ) throws IOException {
        // Store arguments
        this.version = version;
        this.id = id;

        this.controlAddress = controlAddress;
        this.dataBroadcastAddress = dataBroadcastAddress;
        this.dataRecoveryAddress = dataRecoveryAddress;

        // Initializations that do not require arguments
        sendSocket = new DatagramSocket();

        // Initialize storage space
        String storagePath = id + "/storage/chunks";
        storageManager = new ChunkStorageManager(storagePath, INITIAL_STORAGE_SIZE);

        fileTable = new FileTable("../bin/"+id);
        fileTable.load();

        // Create sockets
        controlSocket       = new MulticastSocket(this.controlAddress      .getPort());
        dataBroadcastSocket = new MulticastSocket(this.dataBroadcastAddress.getPort());
        dataRecoverySocket  = new MulticastSocket(this.dataRecoveryAddress .getPort());
        // Have sockets join corresponding groups
        controlSocket      .joinGroup(this.controlAddress      .getAddress());
        dataBroadcastSocket.joinGroup(this.dataBroadcastAddress.getAddress());
        dataRecoverySocket .joinGroup(this.dataRecoveryAddress .getAddress());

        // Create socket handlers
        controlSocketHandler       = new ControlSocketHandler      (this, controlSocket      );
        dataBroadcastSocketHandler = new DataBroadcastSocketHandler(this, dataBroadcastSocket);
        dataRecoverySocketHandler  = new DataRecoverySocketHandler (this, dataRecoverySocket );

        Thread controlSocketHandlerThread       = new Thread(controlSocketHandler);
        Thread dataBroadcastSocketHandlerThread = new Thread(dataBroadcastSocketHandler);
        Thread dataRecoverySocketHandlerThread  = new Thread(dataRecoverySocketHandler);
        controlSocketHandlerThread      .start();
        dataBroadcastSocketHandlerThread.start();
        dataRecoverySocketHandlerThread .start();
    }

    public class CleanupRemoteObjectRunnable implements Runnable {
        private String remoteObjName;

        public CleanupRemoteObjectRunnable(String remoteObjName) {
            this.remoteObjName = remoteObjName;
        }

        @Override
        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry();
                registry.unbind(remoteObjName);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }
    }
    public void bindAsRemoteObject(String remoteObjName) throws RemoteException, AlreadyBoundException {
        PeerInterface stub = (PeerInterface) UnicastRemoteObject.exportObject(this, 0);

        Registry registry = LocateRegistry.getRegistry();
        registry.bind(remoteObjName, stub);

        CleanupRemoteObjectRunnable rmiCleanupRunnable = new CleanupRemoteObjectRunnable(remoteObjName);
        Thread rmiCleanupThread = new Thread(rmiCleanupRunnable);
        Runtime.getRuntime().addShutdownHook(rmiCleanupThread);
    }

    public String getVersion() {
        return version;
    }

    public int getId() {
        return id;
    }

    public InetSocketAddress getControlAddress() {
        return controlAddress;
    }

    public InetSocketAddress getDataBroadcastAddress() {
        return dataBroadcastAddress;
    }

    public InetSocketAddress getDataRecoveryAddress(){
        return dataRecoveryAddress;
    }

    public ChunkStorageManager getStorageManager() {
        return storageManager;
    }

    public FileTable getFileTable() {
        return fileTable;
    }

    public ControlSocketHandler getControlSocketHandler(){
        return controlSocketHandler;
    }

    public DataBroadcastSocketHandler getDataBroadcastSocketHandler(){
        return dataBroadcastSocketHandler;
    }

    public DataRecoverySocketHandler getDataRecoverySocketHandler(){
        return dataRecoverySocketHandler;
    }

    /**
     * Backup file specified by pathname, with a certain replication degree.
     *
     * @param pathname          Pathname of file to be backed up
     * @param replicationDegree Replication degree (number of copies of each file chunk over all machines in the network)
     */
    public void backup(String pathname, int replicationDegree) throws IOException {
        FileChunkIterator fileChunkIterator = new FileChunkIterator(this, new File(pathname));
        Runnable runnable = new BackupRunnable(this, fileChunkIterator, replicationDegree);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Restore file specified by pathname.
     *
     * That file's chunks are retrieved from peers, assembled and then saved to the provided pathname.
     *
     * @param pathname  Pathname of file to be restored
     */
    public void restore(String pathname) throws FileNotFoundException {
        Runnable runnable = new RestoreRunnable(this, pathname);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Delete file specified by pathname.
     *
     * @param pathname  Pathname of file to be deleted over all peers
     */
    public void delete(String pathname) {
        Runnable runnable = new DeleteRunnable(this, pathname);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Set space the peer may use to backup chunks from other machines.
     *
     * @param space_kbytes  Amount of space, in kilobytes (KB, K=1000)
     */
    public void reclaim(int space_kbytes) {
        Runnable runnable = new ReclaimRunnable(this, space_kbytes);
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * Get state information on the peer.
     */
    public String state() {
        StateRunnable runnable = new StateRunnable(this, storageManager);
        Thread thread = new Thread(runnable);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return runnable.getStatus();
    }

    public void send(Message message) throws IOException {
        DatagramPacket packet = message.getPacket();
        sendSocket.send(packet);
    }

    Map<Pair<String, Integer>, Set<Integer>> storedMessageMap =
            new HashMap<>();
    public void pushStoredMessage(StoredMessage storedMessage) {
        Pair<String, Integer> key = new Pair<>(storedMessage.getFileId(), storedMessage.getChunkNo());
        if(!storedMessageMap.containsKey(key))
            storedMessageMap.put(key, new HashSet<>());
        storedMessageMap.get(key).add(storedMessage.getSenderId());
    }
    public int popStoredMessages(PutchunkMessage putchunkMessage){
        Pair<String, Integer> key = new Pair<>(putchunkMessage.getFileId(), putchunkMessage.getChunkNo());
        Set<Integer> storedMessages = storedMessageMap.get(key);
        int ret = (storedMessages != null ? storedMessages.size() : 0);
        if(storedMessages != null) storedMessages.clear();
        return ret;
    }

    public abstract class SocketHandler implements Runnable {
        private static final int BUFFER_LENGTH = 80000;

        private final Peer peer;
        private final DatagramSocket socket;

        private final MessageFactory messageFactory;

        public SocketHandler(Peer peer, DatagramSocket socket) {
            this.peer = peer;
            this.socket = socket;

            messageFactory = new MessageFactory();
        }

        public DatagramSocket getSocket() {
            return socket;
        }

        public Peer getPeer() {
            return peer;
        }

        abstract protected void handle(Message message);

        @Override
        public void run() {
            byte[] buf = new byte[BUFFER_LENGTH];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (true) {
                try {
                    socket.receive(packet);
                    Message message = messageFactory.factoryMethod(packet);
                    if(message.getSenderId() != peer.getId())
                        handle(message);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ControlSocketHandler extends SocketHandler {
        public ControlSocketHandler(Peer peer, DatagramSocket socket) {
            super(peer, socket);
        }

        @Override
        protected void handle(Message message) {
            if (message instanceof StoredMessage || message instanceof GetchunkMessage || message instanceof RemovedMessage  || message instanceof DeleteMessage) {
                if (message instanceof StoredMessage  ) System.out.println("STORED"  );
                if (message instanceof GetchunkMessage) System.out.println("GETCHUNK");
                if (message instanceof RemovedMessage) System.out.println("REMOVED");
                if (message instanceof DeleteMessage) System.out.println("DELETE");
                message.process(getPeer());
            }
        }
    }

    public class DataBroadcastSocketHandler extends SocketHandler {

        private final ExecutorService executor = Executors.newFixedThreadPool(4);

        final Map<String, byte[]> map = new HashMap<>();

        public DataBroadcastSocketHandler(Peer peer, DatagramSocket socket) {
            super(peer, socket);
        }

        @Override
        protected void handle(Message message) {
            if (message instanceof PutchunkMessage) {
                System.out.println("PUTCHUNK");

                message.process(getPeer());
            }
        }

        public void register(String id, byte[] data){
            synchronized(map){
                map.put(id, data);
            }
        }

        private Future<byte[]> getPutChunkPromise(String chunkId){
            return executor.submit(() -> {
                byte[] ret;
                do {
                    synchronized (map) {
                        ret = map.remove(chunkId);
                    }
                } while(ret == null);
                return ret;
            });
        }
        public boolean sense(RemovedMessage removedMessage, int millis) {
            map.clear();
            int timeout = ThreadLocalRandom.current().nextInt(0, millis);
            Future<byte[]> f = getPutChunkPromise(removedMessage.getChunkID());
            try {
                f.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Future failed, returning false");
                e.printStackTrace();
                return false;
            } catch (TimeoutException e) {
                return false;
            }

            return true;
        }

    }

    public class DataRecoverySocketHandler extends SocketHandler {
        /**
         * Executor; is used to execute the promises.
         *
         * Will use a thread pool with 4 threads. This means there are always 4 running threads, even if they are not
         * being used.
         */
        private final ExecutorService executor = Executors.newFixedThreadPool(4);
        /**
         * Map of already-received chunks;
         * chunks are stored in this map by DataRecoverySocketHandler#register(String, byte[]),
         * and the futures returned by DataRecoverySocketHandler#request(GetchunkMessage) periodically check this map
         * for the desired chunk.
         */
        final Map<String, byte[]> map = new HashMap<>();

        public DataRecoverySocketHandler(Peer peer, DatagramSocket socket) {
            super(peer, socket);
        }

        @Override
        protected void handle(Message message) {
            if (message instanceof ChunkMessage){
                System.out.println("CHUNK");

                message.process(getPeer());
            }
        }

        /**
         * @brief Register incoming chunk.
         *
         * Will complete the future obtained from DataRecoverySocketHandler#request(GetchunkMessage)
         * if such request was made.
         *
         * @param id    Chunk ID (file ID + chunk sequential number)
         * @param data  Contents of that chunk
         */
        public void register(String id, byte[] data){
            synchronized(map){
                map.put(id, data);
            }
        }

        /**
         * @brief Request a chunk.
         *
         * @param message   GetChunkMessage that will be broadcast, asking for a chunk
         * @return          Promise of a chunk
         * @throws IOException
         */
        public Future<byte[]> request(GetchunkMessage message) throws IOException {
            getPeer().send(message);
            String id = message.getChunkID();
            return getChunkPromise(id);
        }

        /**
         * @brief Gets promise for a CHUNK message.
         *
         * This promise represents the CHUNK message that will be received after asking by the chunk with a certain ID.
         *
         * When the intended CHUNK message is found in the map, it is removed from the map and returned.
         *
         * @param chunkId   ID of the chunk
         * @return          Future of the chunk
         */
        private Future<byte[]> getChunkPromise(String chunkId){
            return executor.submit(() -> {
                byte[] ret;
                do {
                    synchronized (map) {
                        ret = map.remove(chunkId);
                    }
                } while(ret == null);
                return ret;
            });
        }

        /**
         * @brief Senses data recovery channel for an answer to a GetchunkMessage.
         *
         * @param getchunkMessage   Message to check if there is an answer to
         * @param millis            Milliseconds to wait for
         * @return
         */
        public boolean sense(GetchunkMessage getchunkMessage, int millis) {
            int timeout = ThreadLocalRandom.current().nextInt(0, millis);
            Future<byte[]> f = getChunkPromise(getchunkMessage.getChunkID());
            try {
                f.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Future failed, returning false");
                e.printStackTrace();
                return false;
            } catch (TimeoutException e) {
                return false;
            }
            return true;
        }
    }
}
