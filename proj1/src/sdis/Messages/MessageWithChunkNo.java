package sdis.Messages;

import java.net.InetSocketAddress;

abstract public class MessageWithChunkNo extends Message {
    private final int chunkNo;

    public MessageWithChunkNo(String version, String messageType, int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress) {
        super(version, messageType, senderId, fileId, inetSocketAddress);
        this.chunkNo = chunkNo;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public String getChunkID() {
        return getFileId() + "-" + getChunkNo();
    }

    @Override
    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] chunkNo_bytes = (" " + chunkNo).getBytes();
        byte[] ret = new byte[header.length + chunkNo_bytes.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(chunkNo_bytes, 0, ret, header.length, chunkNo_bytes.length);
        return ret;
    }
}
