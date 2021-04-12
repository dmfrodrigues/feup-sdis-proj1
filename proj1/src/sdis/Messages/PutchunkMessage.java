package sdis.Messages;

import sdis.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
        byte[] replicationDeg_bytes = (" " + getReplicationDegree() + "\r\n\r\n").getBytes();
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

        // Wait a random amount of time between 0-400ms, and collect all STORED messages.
        int wait_time = peer.getRandom().nextInt(400);
        int numStored;
        Future<Integer> f = peer.getControlSocketHandler().checkStored(this, wait_time);
        try {
            numStored = f.get();
        } catch (InterruptedException | ExecutionException e) {
            f.cancel(true);
            System.err.println("checkStored future failed; aborting");
            e.printStackTrace();
            return;
        }

        // If there are enough perceived STORED messages, don't store; just ignore
        if(peer.requireVersion("1.3")) {
            if (numStored >= getReplicationDegree()) {
                System.out.println(getChunkID() + "\t| Perceived replication degree is " + numStored + ", ignoring");
                return;
            } else {
                System.out.println(getChunkID() + "\t| Perceived replication degree is " + numStored + ", storing");
            }
        }

        // If the execution is here, then this peer did not get enough STORED messages, so it will store the chunk itself
        if(!peer.getStorageManager().hasChunk(chunkId)) {
            try {
                if (!peer.getStorageManager().saveChunk(chunkId, getBody())) return;
            } catch (IOException e) {
                System.err.println(getChunkID() + "\t| Failed to save chunk");
                e.printStackTrace();
                return;
            }
            System.out.println(chunkId + "\t| Saved chunk");

            peer.getFileTable().setChunkDesiredRepDegree(chunkId, replicationDeg);
            peer.getFileTable().incrementActualRepDegree(chunkId);

            peer.getDataBroadcastSocketHandler().register(getChunkID(), getBody());
        }

        Message response = new StoredMessage(peer.getVersion(), peer.getId(), getFileId(), getChunkNo(), peer.getControlAddress());

        try {
            peer.send(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
