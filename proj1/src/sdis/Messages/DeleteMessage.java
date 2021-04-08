package sdis.Messages;

import sdis.Peer;

import java.net.InetSocketAddress;

public class DeleteMessage extends Message{

    public DeleteMessage(String version, int senderId, String fileId, InetSocketAddress inetSocketAddress) {
        super(version, "DELETE", senderId, fileId, inetSocketAddress);
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] term = ("\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + term.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(term         , 0, ret, header.length, term.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        System.out.println("sdis.Peer " + getSenderId() + " requested file " + getFileId() + " to be deleted");
        peer.getStorageManager().deleteFile(this.getFileId());
    }
}
