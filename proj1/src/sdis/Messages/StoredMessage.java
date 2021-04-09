package sdis.Messages;

import sdis.Peer;

import java.net.InetSocketAddress;

public class StoredMessage extends MessageWithChunkNo {

    public StoredMessage(String version, int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress){
        super(version, "STORED", senderId, fileId, chunkNo, inetSocketAddress);
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
        peer.getControlSocketHandler().register(this);
        peer.getFileTable().incrementActualRepDegree(getChunkID());
    }
}
