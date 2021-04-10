package sdis.Messages;

import sdis.Peer;

import java.net.InetSocketAddress;

/**
 * <Version> DELETED  <SenderId> <FileId> <InitiatorId> <CRLF><CRLF>
 */

public class DeletedMessage extends Message{

    private final int initiatorId;

    public DeletedMessage(int senderId, String fileId, int initiatorId, InetSocketAddress inetSocketAddress) {
        super("1.1", "DELETED", senderId, fileId, inetSocketAddress);
        this.initiatorId = initiatorId;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] initiatorID_bytes = (" " + initiatorId + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + initiatorID_bytes.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(initiatorID_bytes, 0, ret, header.length, initiatorID_bytes.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        if(peer.getId() != initiatorId) return;
        System.out.println(getFileId() + "\t| Peer " + getSenderId() + " deleted file");

        peer.getControlSocketHandler().register(this);
    }
}
