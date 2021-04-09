package sdis.Messages;

import java.io.IOException;

import sdis.Peer;

import java.net.InetSocketAddress;

public class DeleteMessage extends Message {

    public DeleteMessage(int senderId, String fileId, InetSocketAddress inetSocketAddress) {
        super("1.0", "DELETE", senderId, fileId, inetSocketAddress);
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
        System.out.println("Peer " + getSenderId() + " requested file " + getFileId() + " to be deleted");
        peer.getStorageManager().deleteFile(this.getFileId());
        // Delete Enhancement
        if(!peer.getVersion().equals("1.0")){
            DeletedMessage message = new DeletedMessage(peer.getId(),
                    getFileId(), getSenderId(), peer.getControlAddress());
            try {
                System.out.println("Sending Deleted!");
                peer.send(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
