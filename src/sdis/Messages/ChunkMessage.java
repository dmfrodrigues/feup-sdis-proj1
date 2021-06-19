package sdis.Messages;

import sdis.Peer;

import java.net.InetSocketAddress;

public class ChunkMessage extends MessageWithBody {
    public ChunkMessage(int senderId, String fileId, int chunkNo, byte[] body, InetSocketAddress inetSocketAddress){
        super("1.0", "CHUNK", senderId, fileId, chunkNo, body, inetSocketAddress);
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] term = ("\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + term.length + getBody().length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(term         , 0, ret, header.length, term.length);
        System.arraycopy(getBody()    , 0, ret, header.length + term.length, getBody().length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        peer.getDataRecoverySocketHandler().register(getChunkID(), getBody());
    }
}
