import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Peer implements Remote {
    private static final int localSocketPort = 4040;
    private final DatagramSocket localSocket;

    private final String version;
    private final int id;

    private InetSocketAddress controlAddress;
    private InetSocketAddress dataBroadcastAddress;
    private InetSocketAddress dataRecoveryAddress;

    public Peer(
            String version,
            int id,
            InetSocketAddress controlAddress,
            InetSocketAddress dataBroadcastAddress,
            InetSocketAddress dataRecoveryAddress
    ) throws SocketException {
        localSocket = new DatagramSocket(localSocketPort);

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
}
