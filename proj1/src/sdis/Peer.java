package sdis;

import sdis.Messages.*;
import sdis.Runnables.*;
import sdis.Storage.ChunkStorageManager;
import sdis.Storage.FileChunkIterator;
import sdis.Storage.FileTable;

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

    private final FileTable fileTable;
    private final ChunkStorageManager storageManager;
    private final ControlSocketHandler controlSocketHandler;
    private final DataBroadcastSocketHandler dataBroadcastSocketHandler;
    private final DataRecoverySocketHandler dataRecoverySocketHandler;

    private final Random random = new Random(System.currentTimeMillis());

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(30);

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

        fileTable = new FileTable("../build/"+id);
        fileTable.load();

        // Create sockets
        MulticastSocket controlSocket = new MulticastSocket(this.controlAddress.getPort());
        MulticastSocket dataBroadcastSocket = new MulticastSocket(this.dataBroadcastAddress.getPort());
        MulticastSocket dataRecoverySocket = new MulticastSocket(this.dataRecoveryAddress.getPort());
        // Have sockets join corresponding groups
        controlSocket.      joinGroup(this.controlAddress      .getAddress());
        dataBroadcastSocket.joinGroup(this.dataBroadcastAddress.getAddress());
        dataRecoverySocket .joinGroup(this.dataRecoveryAddress .getAddress());

        // Create socket handlers
        controlSocketHandler       = new ControlSocketHandler      (this, controlSocket);
        dataBroadcastSocketHandler = new DataBroadcastSocketHandler(this, dataBroadcastSocket);
        dataRecoverySocketHandler  = new DataRecoverySocketHandler (this, dataRecoverySocket);

        Thread controlSocketHandlerThread       = new Thread(controlSocketHandler);
        Thread dataBroadcastSocketHandlerThread = new Thread(dataBroadcastSocketHandler);
        Thread dataRecoverySocketHandlerThread  = new Thread(dataRecoverySocketHandler);
        controlSocketHandlerThread      .start();
        dataBroadcastSocketHandlerThread.start();
        dataRecoverySocketHandlerThread .start();
    }

    public boolean requireVersion(String requiredVersion){
        return (getVersion().compareTo(requiredVersion) >= 0);
    }

    public Random getRandom() {
        return random;
    }

    public ScheduledExecutorService getExecutor(){
        return executor;
    }

    public static class CleanupRemoteObjectRunnable implements Runnable {
        private final String remoteObjName;

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
        FileChunkIterator fileChunkIterator;
        try {
            fileChunkIterator = new FileChunkIterator(this, new File(pathname));
        } catch (FileNotFoundException e) {
            System.err.println("File " + pathname + " not found");
            return;
        }
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
     * @param space_kb  Amount of space, in kilobytes (KB, K=1000)
     */
    public void reclaim(int space_kb) {
        Runnable runnable = new ReclaimRunnable(this, space_kb);
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


        // Starts the delete process for one of the pending files
        if(!this.getFileTable().getPendingDelete().isEmpty()) {
            System.out.println(this.getFileTable().getPendingDelete());
            Iterator<String> i = this.getFileTable().getPendingDelete().iterator();
            if(!i.hasNext()) return;
            String path = i.next();
            i.remove();
            System.out.println("Trying to delete " + path + " again");
            delete(path);
        }
    }

    public abstract static class SocketHandler implements Runnable {
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

    public static class ControlSocketHandler extends SocketHandler {
        /**
         * @brief Map to store which peers stored which chunks.
         */
        private final Map<String, Set<Integer>> storedMessageMap = new HashMap<>();


        public ControlSocketHandler(Peer peer, DatagramSocket socket) {
            super(peer, socket);
        }

        @Override
        protected void handle(Message message) {

            if (
                message instanceof StoredMessage ||
                message instanceof GetchunkMessage ||
                message instanceof RemovedMessage  ||
                message instanceof DeleteMessage
            ) {
                message.process(getPeer());
            }
            if(getPeer().requireVersion("1.2") && (
                message instanceof UnstoreMessage
            )) {
                message.process(getPeer());
            }else if(message instanceof DeletedMessage && getPeer().requireVersion("1.1"))
                message.process(getPeer());
        }

        public void register(DeletedMessage deletedMessage) {
            synchronized( getPeer().getFileTable().getFileStoredByPeers(deletedMessage.getFileId())) {
                getPeer().getFileTable().removePeerFromFileStored(deletedMessage.getFileId(), deletedMessage.getSenderId());
                getPeer().getFileTable().getFileStoredByPeers(deletedMessage.getFileId()).notifyAll();
            }
        }

        public Future<Integer> checkDeleted(DeleteMessage deleteMessage, int millis){
            synchronized(getPeer().getFileTable().getFileStoredByPeers(deleteMessage.getFileId())){
                return executor.submit(() -> {
                    Future<Integer> f = resolveWhenAllPeersDeleted(getPeer().getFileTable().getFileStoredByPeers(deleteMessage.getFileId()));
                    Integer ret;
                    try {
                        ret = f.get(millis, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        f.cancel(true);
                        synchronized (getPeer().getFileTable().getFileStoredByPeers(deleteMessage.getFileId())){
                            ret = getPeer().getFileTable().getFileStoredByPeers(deleteMessage.getFileId()).size();
                        }
                    }
                    return ret;
                });
            }
        }

        private Future<Integer> resolveWhenAllPeersDeleted(Set<Integer> peersThatNotDeleted){
            return executor.submit(() -> {
                synchronized(peersThatNotDeleted){
                    while(peersThatNotDeleted.size() > 0) {
                        try {
                            peersThatNotDeleted.wait();
                        } catch (InterruptedException ignored) {}
                    }
                    return 0; // No peers left to delete
                }
            });
        }

        /**
         * @brief Register that a STORED message was received.
         *
         * @param storedMessage STORED message to be registered
         */
        public void register(StoredMessage storedMessage) {
            if(!storedMessage.getVersion().equals("1.0"))
                getPeer().getFileTable().addPeerToFileStored(storedMessage.getFileId(), storedMessage.getSenderId());
            String chunkId = storedMessage.getChunkID();
            synchronized(storedMessageMap) {
                if (storedMessageMap.containsKey(chunkId)){
                    Set<Integer> peersThatStored = storedMessageMap.get(chunkId);
                    synchronized (peersThatStored) {
                        peersThatStored.add(storedMessage.getSenderId());
                        peersThatStored.notifyAll();
                    }
                }
            }
        }

        /**
         * @brief Get a future relative to how many STORED messages answering a given PUTCHUNK message were collected
         * over a period of time specified in milliseconds.
         *
         * If the required number of STORED messages is met earlier than that time period, the future resolves immediately.
         *
         * Useful when processing a received PUTCHUNK message, as the peer only needs to know how many peers stored
         * that chunk, not who exactly stored that chunk.
         *
         * @param m         PUTCHUNK message to check answers for
         * @param millis    Maximum time to wait for the required number of STORED
         * @return
         */
        public Future<Integer> checkStored(PutchunkMessage m, int millis){
            synchronized(storedMessageMap){
                String chunkId = m.getChunkID();
                Set<Integer> peersThatStored = new HashSet<>();
                storedMessageMap.put(chunkId, peersThatStored);
                return getPeer().getExecutor().submit(() -> {
                    Future<Integer> f = resolveWhenReplicationDegreeIsMet(m, peersThatStored);
                    Integer ret;
                    try {
                        ret = f.get(millis, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException e) {
                        f.cancel(true);
                        synchronized (peersThatStored){
                            ret = peersThatStored.size();
                        }
                    }
                    synchronized (storedMessageMap){
                        storedMessageMap.remove(chunkId);
                    }
                    return ret;
                });
            }
        }

        /**
         * @brief Get a future relative to which peers sent STORED messages answering a given PUTCHUNK message which
         * were collected over a period of time specified in milliseconds.
         *
         * Only returns after exactly a period of millis milliseconds.
         *
         * Useful when running as initiator peer during backup, as we want to know all peers who reported STORED over a
         * certain period, so that we can later determine if too many of those peers have stored a chunk, and pick a
         * certain number of them to send UNSTORE to.
         *
         * @param m         PUTCHUNK message to check answers for
         * @param millis    Maximum time to wait for the required number of STORED
         * @return
         */
        public Future<Set<Integer>> checkWhichPeersStored(PutchunkMessage m, int millis){
            String chunkId = m.getChunkID();
            HashSet<Integer> peersThatStored = new HashSet<>();
            synchronized(storedMessageMap) {
                storedMessageMap.put(chunkId, peersThatStored);
            }
            return getPeer().getExecutor().schedule(() -> {
                synchronized (peersThatStored){
                    return (HashSet<Integer>) peersThatStored.clone();
                }
            }, millis, TimeUnit.MILLISECONDS);
        }

        /**
         * @brief Get a future that will only resolve when replication degree required for a chunk is met.
         *
         * The future is relative to how many peers have confirmed to be storing the chunk.
         *
         * @param m                 PUTCHUNK message to check answers for
         * @param peersThatStored   Set of peers that will report to have STORED the chunk
         * @return
         */
        private Future<Integer> resolveWhenReplicationDegreeIsMet(PutchunkMessage m, Set<Integer> peersThatStored){
            return getPeer().getExecutor().submit(() -> {
                synchronized(peersThatStored){
                    while(peersThatStored.size() < m.getReplicationDegree()) {
                        try {
                            peersThatStored.wait();
                        } catch (InterruptedException ignored) {}
                    }
                    return peersThatStored.size();
                }
            });
        }
    }

    public static class DataBroadcastSocketHandler extends SocketHandler {

        final Map<String, ArrayList<Byte>> map = new HashMap<>();

        public DataBroadcastSocketHandler(Peer peer, DatagramSocket socket) {
            super(peer, socket);
        }

        @Override
        protected void handle(Message message) {
            if (message instanceof PutchunkMessage) {
                message.process(getPeer());
            }
        }

        public synchronized void register(String chunkId, byte[] data){
            if (map.containsKey(chunkId)) {
                ArrayList<Byte> dataList = map.get(chunkId);
                synchronized(dataList) {
                    dataList.clear();
                    for (int i = 0; i < data.length; ++i) dataList.add(data[i]);
                    dataList.notifyAll();
                }
            }
        }

        private synchronized Future<byte[]> getPutChunkPromise(String chunkId){
            ArrayList<Byte> retList = new ArrayList<>();
            map.put(chunkId, retList);
            return getPeer().getExecutor().submit(() -> {
                synchronized (retList) {
                    while(retList.size() == 0) retList.wait();
                    byte[] ret;
                    ret = new byte[retList.size()];
                    for (int i = 0; i < retList.size(); ++i)
                        ret[i] = retList.get(i);
                    map.remove(chunkId);
                    return ret;
                }
            });
        }
        public boolean sense(RemovedMessage removedMessage, int millis) {
            map.clear();
            int timeout = getPeer().getRandom().nextInt(millis);
            Future<byte[]> f = getPutChunkPromise(removedMessage.getChunkID());
            try {
                f.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Future failed, returning false");
                e.printStackTrace();
                return false;
            } catch (TimeoutException e) {
                f.cancel(true);
                return false;
            }

            return true;
        }

    }

    public static class DataRecoverySocketHandler extends SocketHandler {
        /**
         * Map of already-received chunks;
         * chunks are stored in this map by DataRecoverySocketHandler#register(String, byte[]),
         * and the futures returned by DataRecoverySocketHandler#request(GetchunkMessage) periodically check this map
         * for the desired chunk.
         */
        final Map<String, ArrayList<Byte>> map = new HashMap<>();

        public DataRecoverySocketHandler(Peer peer, DatagramSocket socket) {
            super(peer, socket);
        }

        @Override
        protected void handle(Message message) {
            if (message instanceof ChunkMessage){
                message.process(getPeer());
            }
        }

        /**
         * @brief Register incoming chunk.
         *
         * Will complete the future obtained from DataRecoverySocketHandler#request(GetchunkMessage)
         * if such request was made.
         *
         * @param chunkId   Chunk ID (file ID + chunk sequential number)
         * @param data      Contents of that chunk
         */
        public synchronized void register(String chunkId, byte[] data){
            if (map.containsKey(chunkId)) {
                System.out.println("Registering chunk " + chunkId);
                ArrayList<Byte> dataList = map.get(chunkId);
                synchronized(dataList) {
                    dataList.clear();
                    for (int i = 0; i < data.length; ++i) dataList.add(data[i]);
                    dataList.notifyAll();
                }
            }
        }

        /**
         * @brief Request a chunk.
         *
         * @param message   GetChunkMessage that will be broadcast, asking for a chunk
         * @return          Promise of a chunk
         * @throws IOException  If send fails
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
        private synchronized Future<byte[]> getChunkPromise(String chunkId){
            ArrayList<Byte> retList = new ArrayList<>();
            map.put(chunkId, retList);
            return getPeer().getExecutor().submit(() -> {
                synchronized (retList) {
                    while(retList.size() == 0) retList.wait();
                    byte[] ret;
                    ret = new byte[retList.size()];
                    for (int i = 0; i < retList.size(); ++i)
                        ret[i] = retList.get(i);
                    map.remove(chunkId);
                    return ret;
                }
            });
        }

        /**
         * @brief Senses data recovery channel for an answer to a GetchunkMessage.
         *
         * @param getchunkMessage   Message to check if there is an answer to
         * @param millis            Milliseconds to wait for
         * @return                  True if channel was sensed busy with a message replying to getchunkMessage, false otherwise
         */
        public boolean sense(GetchunkMessage getchunkMessage, int millis) {
            int timeout = getPeer().getRandom().nextInt(millis);
            Future<byte[]> f = getChunkPromise(getchunkMessage.getChunkID());
            try {
                f.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Future failed, returning false");
                e.printStackTrace();
                return false;
            } catch (TimeoutException e) {
                f.cancel(true);
                return false;
            }
            return true;
        }
    }
}
