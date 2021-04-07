import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;
import java.io.File;
import java.util.Arrays;

public class RemovedMessage extends Message {
    private final int chunkNo;
    private static final int WAIT_MILLIS = 1000;

    public RemovedMessage(String version, int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress) {
        super(version, "REMOVED", senderId, fileId, inetSocketAddress);
        this.chunkNo = chunkNo;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] chunkNo_bytes = (" " + chunkNo + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + chunkNo_bytes.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(chunkNo_bytes, 0, ret, header.length, chunkNo_bytes.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {

        System.out.println("Got Removed!");

        peer.getFileTable().decrementActualRepDegree(getFileId() + "-" + chunkNo);// update local count

        if(!peer.getStorageManager().hasChunk(getFileId() + "-" + chunkNo))
            return;

        if(peer.getFileTable().getActualRepDegree(getFileId() + "-" + chunkNo) < peer.getFileTable().getChunkDesiredRepDegree(getFileId() + "-" + chunkNo)){

            // checks if in a random interval a PutChunk message for this chunkID was received
            if(peer.getDataBroadcastSocketHandler().sense(this, 400)) return;

            // Open chunk

            File file = new File(peer.getStorageManager().getPath() + "/" + getFileId() + "-" + chunkNo);

            byte[] chunk = new byte[(int) file.length()];
            try {
                FileInputStream fileStream = new FileInputStream(file);

                int size = 0;
                try {
                    size = fileStream.read(chunk);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                chunk =  Arrays.copyOf(chunk, size);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            PutchunkMessage message = new PutchunkMessage(peer.getVersion(), peer.getId(),
                    getFileId(), chunkNo,
                    peer.getFileTable().getChunkDesiredRepDegree(getFileId()), chunk, peer.getDataBroadcastAddress()
            );
            try {
                peer.send(message);
                System.out.println("    Sent chunk " + chunkNo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                sleep(WAIT_MILLIS);
            } catch (InterruptedException ignored) {}
        }
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public String getChunkID() {
        return getFileId() + "-" + getChunkNo();
    }

}
