package sdis.Messages;

import sdis.Peer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;
import java.io.File;
import java.util.Arrays;

public class RemovedMessage extends MessageWithChunkNo {
    private static final int WAIT_MILLIS = 1000;
    private static final int ATTEMPTS = 5;

    public RemovedMessage(String version, int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress) {
        super(version, "REMOVED", senderId, fileId, chunkNo, inetSocketAddress);
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] term = ("\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + term.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(term         , 0, ret, header.length, term.length);
        return ret;
    }

    @Override
    public void process(Peer peer) {

        peer.getFileTable().decrementActualRepDegree(getChunkID());// update local count

        if(!peer.getStorageManager().hasChunk(getChunkID()))
            return;

        if(peer.getFileTable().getActualRepDegree(getChunkID()) < peer.getFileTable().getChunkDesiredRepDegree(getChunkID())){

            // checks if in a random interval a PutChunk message for this chunkID was received
            if(peer.getDataBroadcastSocketHandler().sense(this, 400)) return;

            // Open chunk

            File file = new File(peer.getStorageManager().getPath() + "/" + getChunkID());

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
                    getFileId(), getChunkNo(),
                    peer.getFileTable().getChunkDesiredRepDegree(getFileId()), chunk, peer.getDataBroadcastAddress()
            );
            int numStored, attempts = 0;
            do {
                try {
                    peer.send(message);
                    System.out.println("    Sent chunk " + getChunkID());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(WAIT_MILLIS * (long) Math.pow(2, attempts));
                } catch (InterruptedException ignored) {
                }
                numStored = peer.getControlSocketHandler().popStoredMessages(message);
                System.out.println("    Got " + numStored + " stored messages");
                attempts++;
            }while( numStored < peer.getFileTable().getChunkDesiredRepDegree(getChunkID())
                    && attempts < ATTEMPTS);
        }
    }
}
