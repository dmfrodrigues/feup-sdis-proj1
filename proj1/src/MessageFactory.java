import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.Arrays;

public class MessageFactory {
    public MessageFactory(){}

    public Message factoryMethod(DatagramPacket datagramPacket){
        byte[] data = Arrays.copyOf(datagramPacket.getData(), datagramPacket.getLength());
        InetSocketAddress inetSocketAddress = (InetSocketAddress) datagramPacket.getSocketAddress();

        int headerSize = Utils.find_nth(data, new byte[]{'\r', '\n'}, 1);
        String header = new String(Arrays.copyOfRange(data, 0, headerSize));
        String[] headerSplit = header.split("[ ]+");

        String version = headerSplit[0];
        String messageType = headerSplit[1];
        int senderId = Integer.parseInt(headerSplit[2]);
        String fileId = headerSplit[3];
        int chunkNo = (headerSplit.length >= 5 ? Integer.parseInt(headerSplit[4]) : null);
        int replicationDeg = (headerSplit.length >= 6 ? Integer.parseInt(headerSplit[5]) : null);

        // Messages without body
        Message ret = switch (messageType) {
            case "STORED" -> new StoredMessage(version, senderId, fileId, chunkNo, inetSocketAddress);
            case "GETCHUNK" -> new GetchunkMessage(version, senderId, fileId, chunkNo, inetSocketAddress);
            default -> null;
        };
        if(ret != null) return ret;

        // Messages with body
        int bodyOffset = Utils.find_nth(data, new byte[]{'\r', '\n'}, 2)+2;
        byte[] body = Arrays.copyOfRange(data, bodyOffset, data.length);
        ret = switch (messageType) {
            case "PUTCHUNK" -> new PutchunkMessage(version, senderId, fileId, chunkNo, replicationDeg, body, inetSocketAddress);
            case "CHUNK" -> new ChunkMessage(version, senderId, fileId, chunkNo, body, inetSocketAddress);
            default -> null;
        };
        return ret;
    }
}
