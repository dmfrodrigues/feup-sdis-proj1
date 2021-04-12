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
        System.out.println(getFileId() + "\t| Peer " + getSenderId() + " requested file to be deleted");
        peer.getStorageManager().deleteFile(this.getFileId());
        // Delete Enhancement
        if(peer.requireVersion("1.1")){
            DeletedMessage message = new DeletedMessage(peer.getId(),
                    getFileId(), getSenderId(), peer.getControlAddress());
            try {
                peer.send(message);
                System.out.println(getFileId() + "\t| Sent DELETED");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
