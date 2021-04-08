import java.net.InetSocketAddress;

public class ChunkMessage extends MessageWithChunkNo {
    private final int chunkNo;
    private final byte[] body;

    public ChunkMessage(String version, int senderId, String fileId, int chunkNo, byte[] body, InetSocketAddress inetSocketAddress){
        super(version, "CHUNK", senderId, fileId, chunkNo, inetSocketAddress);

        if(body == null) throw new NullPointerException("body");

        this.chunkNo = chunkNo;
        this.body = body;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] chunkNo_bytes = (" " + chunkNo + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + chunkNo_bytes.length + body.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(chunkNo_bytes, 0, ret, header.length, chunkNo_bytes.length);
        System.arraycopy(body         , 0, ret, header.length + chunkNo_bytes.length, body.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        System.out.println("Received chunk " + getChunkID() + " upon request");
        peer.getDataRecoverySocketHandler().register(getChunkID(), body);
    }
}
