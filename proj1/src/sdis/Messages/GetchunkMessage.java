package sdis.Messages;

import sdis.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class GetchunkMessage extends MessageWithChunkNo {
    /**
     * How much a peer receiving this message should wait (and sense MDR) before answering
     */
    private static final int RESPONSE_TIMEOUT_MILLIS = 400;

    public GetchunkMessage(int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress){
        super("1.0", "GETCHUNK", senderId, fileId, chunkNo, inetSocketAddress);
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
        System.out.println(getChunkID() + "\t| Peer " + getSenderId() + " requested chunk");
        if(!peer.getStorageManager().hasChunk(getChunkID())) return;
        if(peer.getDataRecoverySocketHandler().sense(this, 400)) return;
        try {
            peer.getStorageManager().getChunk(getChunkID())
            .thenApplyAsync(chunk -> {
                ChunkMessage message = new ChunkMessage(peer.getId(), getFileId(), getChunkNo(), chunk, peer.getDataRecoveryAddress());
                try {
                    peer.send(message);
                } catch (IOException e) {
                    System.err.println(getChunkID() + "\t| Failed to answer GetchunkMessage with a ChunkMessage");
                    e.printStackTrace();
                }
                System.out.println(message.getChunkID() + "\t| Sent chunk");
                return null;
            }, Peer.getExecutor());
        } catch (IOException e) {
            System.err.println(getChunkID() + "\t| Failed to find file, although file existance was already checked (INVESTIGATE)");
            e.printStackTrace();
        }
    }
}
