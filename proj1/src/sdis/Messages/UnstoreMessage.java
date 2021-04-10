package sdis.Messages;

import sdis.Peer;

import java.net.InetSocketAddress;

public class UnstoreMessage extends MessageWithChunkNo {

    /**
     * ID of the peer the message is meant to.
     */
    private final int destinationId;

    public UnstoreMessage(int senderId, String fileId, int chunkNo, int destinationId, InetSocketAddress inetSocketAddress){
        super("1.2", "UNSTORE", senderId, fileId, chunkNo, inetSocketAddress);
        this.destinationId = destinationId;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] destinationId_bytes = (" " + getDestinationId() + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + destinationId_bytes.length];
        System.arraycopy(header             , 0, ret, 0, header.length);
        System.arraycopy(destinationId_bytes, 0, ret, header.length, destinationId_bytes.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        if(getDestinationId() != peer.getId()) return;
        peer.getStorageManager().deleteChunk(getChunkID());
        peer.getFileTable().decrementActualRepDegree(getChunkID());
        System.out.println(getChunkID() + "\t| Unstored chunk");
    }
}
