import java.io.IOException;

import static java.lang.Thread.sleep;

public class BackupRunnable implements Runnable {
    /**
     * Time to wait before resending a backup request, in milliseconds.
     */
    private static final int WAIT_MILLIS = 1000;

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
            System.out.println("Sending chunk "+i);
            byte[] chunk = fileChunkIterator.next();
            PutchunkMessage message = new PutchunkMessage(peer.getVersion(), peer.getId(), fileChunkIterator.getFileId(), i, replicationDegree, chunk, peer.getDataBroadcastAddress());

            peer.getFileTable().setChunkDesiredRepDegree(fileChunkIterator.getFileId() + "-" + i, replicationDegree);

            int numStored;
            do {
                try {
                    peer.send(message);
                    System.out.println("    Sent chunk "+i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(WAIT_MILLIS);
                } catch (InterruptedException ignored) {}
                numStored = peer.popStoredMessages(message);
                System.out.println("    Got " + numStored + " stored messages");
            } while(numStored < replicationDegree);
        }
    }
}
