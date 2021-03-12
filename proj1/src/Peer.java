import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class Peer implements Remote {
    private static final int LOCAL_SOCKET_PORT = 4040;
    private static final int BUFFER_LENGTH = 80000;
    private static final String STORAGE_PATH = "storage/backup";
    /**
     * Initially reserved storage for backing up chunks (in bytes).
     */
    private static final int INITIAL_STORAGE_SIZE = 1000000000;

    private final DatagramSocket localSocket;

    private final String version;
    private final int id;

    private final InetSocketAddress controlAddress;
    private final InetSocketAddress dataBroadcastAddress;
    private final InetSocketAddress dataRecoveryAddress;

    private final ChunkStorageManager storageManager;

    public Peer(
            String version,
            int id,
            InetSocketAddress controlAddress,
            InetSocketAddress dataBroadcastAddress,
            InetSocketAddress dataRecoveryAddress
    ) throws SocketException {
        localSocket = new DatagramSocket(LOCAL_SOCKET_PORT);

        this.version = version;
        this.id = id;

        this.controlAddress = controlAddress;
        this.dataBroadcastAddress = dataBroadcastAddress;
        this.dataRecoveryAddress = dataRecoveryAddress;

        storageManager = new ChunkStorageManager(STORAGE_PATH, INITIAL_STORAGE_SIZE);

        PacketHandler packetHandler = new PacketHandler(this, localSocket);
        Thread packetHandlerThread = new Thread(packetHandler);
        packetHandlerThread.start();
    }

    public void bindAsRemoteObject(String remote_obj_name) throws RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.getRegistry();
        registry.bind(remote_obj_name, this);
    }

    public String getVersion() {
        return version;
    }

    public int getId() {
        return id;
    }

    public InetSocketAddress getDataBroadcastAddress() {
        return dataBroadcastAddress;
    }

    public ChunkStorageManager getStorageManager() {
        return storageManager;
    }

    /**
     * Backup file specified by pathname, with a certain replication degree.
     *
     * @param pathname          Pathname of file to be backed up
     * @param replicationDegree Replication degree (number of copies of each file chunk over all machines in the network)
     */
    public void backup(String pathname, int replicationDegree) throws IOException {
        ChunkedFile chunkedFile = new ChunkedFile(new File(pathname));
        chunkedFile.readChunks();
        Runnable runnable = new BackupRunnable(this, chunkedFile, replicationDegree);
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
    public void restore(String pathname){
        throw new NoSuchMethodException("restore; yet to come");
    }

    /**
     * Delete file specified by pathname.
     *
     * @param pathname  Pathname of file to be deleted over all peers
     */
    public void delete(String pathname){
        throw new NoSuchMethodException("delete; yet to come");
    }

    /**
     * Set space the peer may use to backup chunks from other machines.
     *
     * @param space_kbytes  Amount of space, in kilobytes (KB, K=1000)
     */
    public void reclaim(int space_kbytes){
        throw new NoSuchMethodException("reclaim; yet to come");
    }

    /**
     * Get state information on the peer.
     */
    public void state(){
        throw new NoSuchMethodException("state; yet to come");
    }

    public void send(Message message) throws IOException {
        DatagramPacket packet = message.getPacket();
        localSocket.send(packet);
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
        int ret = storedMessageMap.get(key).size();
        storedMessageMap.get(key).clear();
        return ret;
    }

    public class PacketHandler implements Runnable {
        private final Peer peer;
        private final DatagramSocket socket;
        private final MessageFactory messageFactory;

        public PacketHandler(Peer peer, DatagramSocket socket){
            this.peer = peer;
            this.socket = socket;

            messageFactory = new MessageFactory();
        }

        @Override
        public void run() {
            byte[] buf = new byte[BUFFER_LENGTH];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while(true){
                try {
                    socket.receive(packet);
                    Message message = messageFactory.factoryMethod(packet);
                    message.process(peer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
