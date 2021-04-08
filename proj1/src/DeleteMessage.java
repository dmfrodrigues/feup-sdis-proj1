import java.io.IOException;
import java.net.InetSocketAddress;

public class DeleteMessage extends Message{

    public DeleteMessage(String version, int senderId, String fileId, InetSocketAddress inetSocketAddress) {
        super(version, "DELETE", senderId, fileId, inetSocketAddress);
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] term = (" " + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + term.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(term, 0, ret, header.length, term.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        System.out.println("Peer " + getSenderId() + " requested file " + getFileId() + " to be deleted");
        peer.getStorageManager().deleteFile(this.getFileId());

        // TODO change this Enhancement
        if(peer.getVersion().equals("1.0")){
            DeletedMessage message = new DeletedMessage(peer.getVersion(), peer.getId(),
                    getFileId(), getSenderId(), peer.getControlAddress());
            try {
                peer.send(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
