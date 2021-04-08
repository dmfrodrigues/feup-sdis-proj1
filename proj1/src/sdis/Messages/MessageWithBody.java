package sdis.Messages;

import java.net.InetSocketAddress;

public abstract class MessageWithBody extends MessageWithChunkNo {
    private final byte[] body;

    public MessageWithBody(String version, String messageType, int senderId, String fileId, int chunkNo, byte[] body, InetSocketAddress inetSocketAddress) {
        super(version, messageType, senderId, fileId, chunkNo, inetSocketAddress);
        if(body == null) throw new NullPointerException("body");
        this.body = body;
    }

    public byte[] getBody(){
        return body;
    }
}
