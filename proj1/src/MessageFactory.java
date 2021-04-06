import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class MessageFactory {
    public MessageFactory(){}

    public Message factoryMethod(DatagramPacket datagramPacket) throws ClassNotFoundException {
        byte[] data = Arrays.copyOf(datagramPacket.getData(), datagramPacket.getLength());
        InetSocketAddress inetSocketAddress = (InetSocketAddress) datagramPacket.getSocketAddress();

        int headerSize = Utils.find_nth(data, new byte[]{'\r', '\n'}, 1);
        String header = new String(Arrays.copyOfRange(data, 0, headerSize));
        String[] headerSplit = header.split("[ ]+");

        String version = headerSplit[0];
        String messageType = headerSplit[1];
        int senderId = Integer.parseInt(headerSplit[2]);
        String fileId = headerSplit[3];

        // Messages without body
        switch (messageType) {
            case "DELETE": return new DeleteMessage(version, senderId, fileId, inetSocketAddress);
            default: break;
        };

        int chunkNo = Integer.parseInt(headerSplit[4]);
        switch (messageType) {
            case "STORED": return new StoredMessage(version, senderId, fileId, chunkNo, inetSocketAddress);
            case "GETCHUNK": return new GetchunkMessage(version, senderId, fileId, chunkNo, inetSocketAddress);
            case "REMOVED": return new RemovedMessage(version, senderId, fileId, chunkNo, inetSocketAddress);
            default: break;
        };

        // Messages with body
        int bodyOffset = Utils.find_nth(data, new byte[]{'\r', '\n'}, 2)+2;
        byte[] body = Arrays.copyOfRange(data, bodyOffset, data.length);
        switch (messageType) {
            case "CHUNK": return new ChunkMessage(version, senderId, fileId, chunkNo, body, inetSocketAddress);
            default: break;
        };

        int replicationDeg = Integer.parseInt(headerSplit[5]);
        switch (messageType) {
            case "PUTCHUNK": return new PutchunkMessage(version, senderId, fileId, chunkNo, replicationDeg, body, inetSocketAddress);
            default: break;
        };

        throw new ClassNotFoundException(messageType);
    }
}
