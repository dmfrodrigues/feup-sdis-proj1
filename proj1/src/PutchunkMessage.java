import java.io.IOException;
import java.net.InetSocketAddress;

public class PutchunkMessage extends Message {
    private final int chunkNo;
    private final int replicationDeg;
    private final byte[] body;

    public PutchunkMessage(String version, int senderId, String fileId, int chunkNo, int replicationDeg, byte[] body, InetSocketAddress inetSocketAddress){
        super(version, "PUTCHUNK", senderId, fileId, inetSocketAddress);

        if(body == null) throw new NullPointerException("body");

        this.chunkNo = chunkNo;
        this.replicationDeg = replicationDeg;
        this.body = body;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] chunkNo_replicationDeg_bytes = (" " + chunkNo + " " + replicationDeg + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + chunkNo_replicationDeg_bytes.length + body.length];
        System.arraycopy(header                      , 0, ret, 0, header.length);
        System.arraycopy(chunkNo_replicationDeg_bytes, 0, ret, header.length, chunkNo_replicationDeg_bytes.length);
        System.arraycopy(body                        , 0, ret, header.length + chunkNo_replicationDeg_bytes.length, body.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        String chunkId = getFileId() + "-" + getChunkNo();
        peer.getStorageManager().saveChunk(chunkId, body);
        Message response = new StoredMessage(getVersion(), getSenderId(), getFileId(), getChunkNo(), getSocketAddress());
        try {
            peer.send(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getChunkNo() {
        return chunkNo;
    }
}
