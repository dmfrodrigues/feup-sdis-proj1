package sdis.Messages;

import sdis.Peer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RemovedMessage extends MessageWithChunkNo {
    private static final int WAIT_MILLIS = 1000;
    private static final int ATTEMPTS = 5;

    public RemovedMessage(int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress) {
        super("1.0", "REMOVED", senderId, fileId, chunkNo, inetSocketAddress);
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

            int replicationDegree = peer.getFileTable().getChunkDesiredRepDegree(getFileId());

            PutchunkMessage message = new PutchunkMessage(peer.getId(),
                    getFileId(), getChunkNo(),
                    replicationDegree, chunk, peer.getDataBroadcastAddress()
            );

            int numStored, attempts = 0;
            int wait_millis = WAIT_MILLIS;
            do {
                Future<Integer> f = peer.getControlSocketHandler().checkStored(message, wait_millis);
                try {
                    peer.send(message);
                    System.out.println(getChunkID() + "\t| Sent chunk");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    numStored = f.get();
                } catch (InterruptedException | ExecutionException e) {
                    f.cancel(true);
                    System.err.println(getChunkID() + "\t| checkStored future failed; aborting");
                    e.printStackTrace();
                    return;
                }
                System.out.println(message.getChunkID() + "\t| Perceived replication degree is " + numStored);
                attempts++;
                wait_millis *= 2;
            } while(numStored < replicationDegree && attempts < ATTEMPTS);
        }
    }
}
