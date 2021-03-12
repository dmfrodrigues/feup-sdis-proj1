import java.io.IOException;

import static java.lang.Thread.sleep;

public class BackupRunnable implements Runnable {
    /**
     * Time to wait before resending a backup request, in milliseconds.
     */
    private static final int WAIT_MILLIS = 1000;

    private Peer peer;
    private ChunkedFile chunkedFile;
    private int replicationDegree;

    public BackupRunnable(Peer peer, ChunkedFile chunkedFile, int replicationDegree){
        this.peer = peer;
        this.chunkedFile = chunkedFile;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public void run() {
        int n = chunkedFile.getNumberChunks();
        for(int i = 0; i < n; ++i){
            System.out.println("Sending chunk "+i);
            byte[] chunk = chunkedFile.getChunk(i);
            PutchunkMessage message = new PutchunkMessage(peer.getVersion(), peer.getId(), chunkedFile.getFileId(), i, replicationDegree, chunk, peer.getDataBroadcastAddress());

            int numStored = 0;
            do {
                try {
                    peer.send(message);
                    System.out.println("    Sent chunk "+i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(WAIT_MILLIS);
                } catch (InterruptedException e) {}
                numStored = peer.popStoredMessages(message);
                System.out.println("    Got " + numStored + " stored messages");
            } while(numStored < replicationDegree);
        }
    }
}
