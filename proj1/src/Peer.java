import java.io.File;
import java.io.IOException;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

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
        FileChunkIterator fileChunkIterator = new FileChunkIterator(new File(pathname));
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
    public void restore(String pathname) {
    }

    /**
     * Delete file specified by pathname.
     *
     * @param pathname  Pathname of file to be deleted over all peers
     */
    public void delete(String pathname) {
    }

    /**
     * Set space the peer may use to backup chunks from other machines.
     *
     * @param space_kbytes  Amount of space, in kilobytes (KB, K=1000)
     */
    public void reclaim(int space_kbytes) {
    }

    /**
     * Get state information on the peer.
     */
    public void state() {
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
                } catch (IOException e) {
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
            if (message instanceof StoredMessage || message instanceof GetchunkMessage) {
                if (message instanceof StoredMessage  ) System.out.println("STORED"  );
                if (message instanceof GetchunkMessage) System.out.println("GETCHUNK");

                message.process(getPeer());
            }
        }
    }

    public class DataBroadcastSocketHandler extends SocketHandler {
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
    }

    public class DataRecoverySocketHandler extends SocketHandler {
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
    }
}
