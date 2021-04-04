import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;
import java.io.File;
import java.util.Arrays;
import java.util.Random;

public class RemovedMessage extends Message {
    private final int chunkNo;
    private String fileID;
    private static final int WAIT_MILLIS = 1000;

    public RemovedMessage(String version, int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress) {
        super(version, "REMOVED", senderId, fileId, inetSocketAddress);
        this.chunkNo = chunkNo;
        this.fileID = fileID;
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
        peer.getFileTable().decrementActualRepDegree(fileID + "-" + chunkNo);// update local count

        if(peer.getFileTable().getActualRepDegree(fileID + "-" + chunkNo) < peer.getFileTable().getChunkDesiredRepDegree(fileID + "-" + chunkNo)){

            // sleep random 0-400
            Random rand = new Random();
            int wait_time = rand.nextInt(400);

            try {
                peer.inRemovedProcess = true;
                sleep(wait_time);
                peer.inRemovedProcess = false;
            } catch (InterruptedException ignored) {}

            // if receive PutChunk of this chunkID -> abort
            for(String _fileID: peer.putChunkFileIDs)
                if(_fileID.equals(fileID)) {
                    peer.putChunkFileIDs.clear();
                    return;
                }
            peer.putChunkFileIDs.clear();

            // Open chunk

            File file = new File(peer.getStorageManager().getPath() + "/" + fileID);

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
                    fileID, chunkNo,
                    peer.getFileTable().getChunkDesiredRepDegree(fileID), chunk, peer.getDataBroadcastAddress()
            );
            do {
                try {
                    peer.send(message);
                    System.out.println("    Sent chunk " + chunkNo);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(WAIT_MILLIS);
                } catch (InterruptedException ignored) {}
            } while(peer.getFileTable().getActualRepDegree(fileID + "-" + chunkNo)  < peer.getFileTable().getChunkDesiredRepDegree(fileID + "-" + chunkNo));

        }
    }
}
