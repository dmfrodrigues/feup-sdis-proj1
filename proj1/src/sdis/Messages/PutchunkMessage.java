package sdis.Messages;

import sdis.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PutchunkMessage extends MessageWithBody {

    private final int replicationDeg;

    public PutchunkMessage(int senderId, String fileId, int chunkNo, int replicationDeg, byte[] body, InetSocketAddress inetSocketAddress){
        super("1.0", "PUTCHUNK", senderId, fileId, chunkNo, body, inetSocketAddress);

        this.replicationDeg = replicationDeg;
    }
    
    public int getReplicationDegree() {
        return replicationDeg;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] replicationDeg_bytes = (" " + replicationDeg + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + replicationDeg_bytes.length + getBody().length];
        System.arraycopy(header                      , 0, ret, 0, header.length);
        System.arraycopy(replicationDeg_bytes        , 0, ret, header.length, replicationDeg_bytes.length);
        System.arraycopy(getBody()                   , 0, ret, header.length + replicationDeg_bytes.length, getBody().length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        String chunkId = getChunkID();

        if(peer.getFileTable().getFileIDs().contains(getFileId())) // Checks if peer initiated this chunk
            return;
        if(!peer.getStorageManager().hasChunk(chunkId)) {
            if (!peer.getStorageManager().saveChunk(chunkId, getBody()))
                return;
            System.out.println("Saved chunk " + chunkId);

            peer.getFileTable().setChunkDesiredRepDegree(chunkId, replicationDeg);
            peer.getFileTable().incrementActualRepDegree(chunkId);

            peer.getDataBroadcastSocketHandler().register(getChunkID(), getBody());
        }

        int wait_time = peer.getRandom().nextInt(400);
        try {
            Thread.sleep(wait_time);
        } catch (InterruptedException e) {
            System.err.println("Sleep got interrupted; resuming");
            e.printStackTrace();
        }


        Message response = new StoredMessage(peer.getId(), getFileId(), getChunkNo(), peer.getControlAddress());
        try {
            peer.send(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
