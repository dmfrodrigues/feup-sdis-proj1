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

    private final DatagramSocket localSocket;

    private final String version;
    private final int id;

    private final InetSocketAddress controlAddress;
    private final InetSocketAddress dataBroadcastAddress;
    private final InetSocketAddress dataRecoveryAddress;

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
}
