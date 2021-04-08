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
}
