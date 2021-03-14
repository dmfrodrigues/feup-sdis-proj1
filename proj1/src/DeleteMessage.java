import java.net.InetSocketAddress;

public class DeleteMessage extends Message{

    public DeleteMessage(String version, int senderId, String fileId, InetSocketAddress inetSocketAddress) {
        super(version, "DELETE", senderId, fileId, inetSocketAddress);
    }

    @Override
    public void process(Peer peer) {

    }
}
