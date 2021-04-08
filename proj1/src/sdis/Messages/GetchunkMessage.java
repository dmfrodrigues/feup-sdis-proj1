package sdis.Messages;

import sdis.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class GetchunkMessage extends MessageWithChunkNo {
    /**
     * How much a peer receiving this message should wait (and sense MDR) before answering
     */
    private static final int RESPONSE_TIMEOUT_MILLIS = 400;
    private final int chunkNo;

    public GetchunkMessage(String version, int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress){
        super(version, "GETCHUNK", senderId, fileId, chunkNo, inetSocketAddress);
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
        System.out.println("sdis.Peer " + getSenderId() + " requested chunk " + getChunkID());
        if(!peer.getStorageManager().hasChunk(getChunkID())) return;
        if(peer.getDataRecoverySocketHandler().sense(this, 400)) return;
        byte[] chunk;
        try {
            chunk = peer.getStorageManager().getChunk(getChunkID());
        } catch (IOException e) {
            System.err.println("Failed to ask if this peer has chunk with ID " + getChunkID());
            e.printStackTrace();
            return;
        }
        ChunkMessage message = new ChunkMessage(getVersion(), peer.getId(), getFileId(), getChunkNo(), chunk, peer.getDataRecoveryAddress());
        try {
            peer.send(message);
        } catch (IOException e) {
            System.err.println("Failed to answer sdis.Messages.GetchunkMessage with a sdis.Messages.ChunkMessage");
            e.printStackTrace();
        }
        System.out.println("Sent chunk " + message.getChunkID());
    }
}
