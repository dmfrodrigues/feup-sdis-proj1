import java.net.InetSocketAddress;

public class GetchunkMessage extends Message {
    private int chunkNo;

    public GetchunkMessage(String version, int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress){
        super(version, "GETCHUNK", senderId, fileId, inetSocketAddress);
        this.chunkNo = chunkNo;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] chunkNo_bytes = (" " + chunkNo + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + chunkNo_bytes.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(chunkNo_bytes, 0, ret, header.length, chunkNo_bytes.length);
        return ret;
    }
}
