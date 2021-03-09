import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class Peer {
    private static final int localSocketPort = 4040;
    private final DatagramSocket localSocket;

    private final String version;
    private final int id;

    private String serviceRMIAccessPoint;

    private InetSocketAddress controlAddress;
    private InetSocketAddress dataBroadcastAddress;
    private InetSocketAddress dataRecoveryAddress;

    public Peer(
            String version,
            int id,
            String serviceRMIAccessPoint,
            InetSocketAddress controlAddress,
            InetSocketAddress dataBroadcastAddress,
            InetSocketAddress dataRecoveryAddress,
    ) throws SocketException {
        localSocket = new DatagramSocket(localSocketPort);

        this.version = version;
        this.id = id;
        this.serviceRMIAccessPoint = serviceRMIAccessPoint;

        this.controlAddress = controlAddress;
        this.dataBroadcastAddress = dataBroadcastAddress;
        this.dataRecoveryAddress = dataRecoveryAddress;
    }
}
