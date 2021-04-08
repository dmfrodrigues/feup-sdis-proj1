package sdis.Runnables;

import sdis.Messages.PutchunkMessage;
import sdis.Peer;
import sdis.Storage.FileChunkIterator;

import java.io.IOException;

import static java.lang.Thread.sleep;

public class BackupRunnable implements Runnable {
    /**
     * Time to wait before resending a backup request, in milliseconds.
     */
    private static final int WAIT_MILLIS = 1000;
    /**
     * Maximum attempts to transmit backup messages per chunk.
     */
    private static final int ATTEMPTS = 5;

    private final Peer peer;
    private final FileChunkIterator fileChunkIterator;
    private final int replicationDegree;

    public BackupRunnable(Peer peer, FileChunkIterator fileChunkIterator, int replicationDegree){
        this.peer = peer;
        this.fileChunkIterator = fileChunkIterator;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public void run() {
        int n = fileChunkIterator.length();

        peer.getFileTable().setFileDesiredRepDegree(fileChunkIterator.getFileId(), replicationDegree);

        for(int i = 0; i < n; ++i){
            byte[] chunk = fileChunkIterator.next();
            PutchunkMessage message = new PutchunkMessage(peer.getId(), fileChunkIterator.getFileId(), i, replicationDegree, chunk, peer.getDataBroadcastAddress());

            peer.getFileTable().setChunkDesiredRepDegree(fileChunkIterator.getFileId() + "-" + i, replicationDegree);

            int numStored, attempts=0;
            do {
                try {
                    peer.send(message);
                    System.out.println("Sent chunk " + message.getChunkID());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(WAIT_MILLIS * (long) Math.pow(2, attempts));
                } catch (InterruptedException ignored) {}
                numStored = peer.getControlSocketHandler().popStoredMessages(message);
                System.out.println("Perceived replication degree of " + message.getChunkID() + " is " + numStored);
                attempts++;
            } while(numStored < replicationDegree && attempts < ATTEMPTS);
        }
    }
}
