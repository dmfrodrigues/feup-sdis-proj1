package sdis.Messages;

import sdis.Peer;

import java.net.InetSocketAddress;

public class StoredMessage extends MessageWithChunkNo {
    private final int chunkNo;

    public StoredMessage(String version, int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress){
        super(version, "STORED", senderId, fileId, chunkNo, inetSocketAddress);
        this.chunkNo = chunkNo;
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
        peer.pushStoredMessage(this);
        peer.getFileTable().incrementActualRepDegree(getFileId() + "-" + chunkNo);
    }
}
