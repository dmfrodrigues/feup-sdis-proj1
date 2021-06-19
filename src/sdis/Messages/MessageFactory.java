package sdis.Messages;

import sdis.Utils.Utils;

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
        String fileId = headerSplit[3].toUpperCase();

        // Messages without body
        switch(messageType) {
            case "DELETE": return new DeleteMessage(senderId, fileId, inetSocketAddress);
            case "DELETED": return new DeletedMessage(senderId, fileId, Integer.parseInt(headerSplit[4]), inetSocketAddress);
            default: break;
        }

        int chunkNo = Integer.parseInt(headerSplit[4]);
        switch (messageType) {
            case "STORED":  return new StoredMessage(version, senderId, fileId, chunkNo, inetSocketAddress);
            case "GETCHUNK":return new GetchunkMessage(senderId, fileId, chunkNo, inetSocketAddress);
            case "REMOVED": return new RemovedMessage(senderId, fileId, chunkNo, inetSocketAddress);
            case "UNSTORE": return new UnstoreMessage(senderId, fileId, chunkNo, Integer.parseInt(headerSplit[5]), inetSocketAddress);
            case "GETCHUNKTCP": return new GetchunkTCPMessage(senderId, fileId, chunkNo, headerSplit[5], inetSocketAddress);
            default: break;
        }

        // Messages with body
        int bodyOffset = Utils.find_nth(data, new byte[]{'\r', '\n'}, 2)+2;
        byte[] body = Arrays.copyOfRange(data, bodyOffset, data.length);
        if (messageType.equals("CHUNK")) {
            return new ChunkMessage(senderId, fileId, chunkNo, body, inetSocketAddress);
        }

        int replicationDeg = Integer.parseInt(headerSplit[5]);
        if (messageType.equals("PUTCHUNK")) {
            return new PutchunkMessage(senderId, fileId, chunkNo, replicationDeg, body, inetSocketAddress);
        }

        throw new ClassNotFoundException(messageType);
    }
}
