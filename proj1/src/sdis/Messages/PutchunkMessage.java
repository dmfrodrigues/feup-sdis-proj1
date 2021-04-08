package sdis.Messages;

import sdis.Peer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PutchunkMessage extends MessageWithChunkNo {
    private final int chunkNo;
    private final int replicationDeg;
    private final byte[] body;

    public PutchunkMessage(String version, int senderId, String fileId, int chunkNo, int replicationDeg, byte[] body, InetSocketAddress inetSocketAddress){
        super(version, "PUTCHUNK", senderId, fileId, chunkNo, inetSocketAddress);

        if(body == null) throw new NullPointerException("body");

        this.chunkNo = chunkNo;
        this.replicationDeg = replicationDeg;
        this.body = body;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] chunkNo_replicationDeg_bytes = (" " + chunkNo + " " + replicationDeg + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + chunkNo_replicationDeg_bytes.length + body.length];
        System.arraycopy(header                      , 0, ret, 0, header.length);
        System.arraycopy(chunkNo_replicationDeg_bytes, 0, ret, header.length, chunkNo_replicationDeg_bytes.length);
        System.arraycopy(body                        , 0, ret, header.length + chunkNo_replicationDeg_bytes.length, body.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {
        String chunkId = getChunkID();

        if(peer.getFileTable().getFileIDs().contains(getFileId())) // Checks if peer initiated this chunk
            return;
        if(!peer.getStorageManager().saveChunk(chunkId, body))
            return;
        System.out.println("Saved chunk " + chunkId);

        peer.getFileTable().setChunkDesiredRepDegree(chunkId, replicationDeg);
        peer.getFileTable().incrementActualRepDegree(chunkId);

        peer.getDataBroadcastSocketHandler().register(getChunkID(), body);

        int wait_time = peer.getRandom().nextInt(400);
        try {
            Thread.sleep(wait_time);
        } catch (InterruptedException e) {
            System.err.println("Sleep got interrupted; resuming");
            e.printStackTrace();
        }

        Message response = new StoredMessage(peer.getVersion(), peer.getId(), getFileId(), getChunkNo(), peer.getControlAddress());
        try {
            peer.send(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
