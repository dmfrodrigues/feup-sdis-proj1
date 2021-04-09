package sdis.Runnables;

import sdis.Messages.PutchunkMessage;
import sdis.Messages.UnstoreMessage;
import sdis.Peer;
import sdis.Storage.FileChunkIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

            peer.getFileTable().setChunkDesiredRepDegree(message.getChunkID(), replicationDegree);

            Set<Integer> peersThatStored;
            int numStored, attempts=0;
            int wait_millis = WAIT_MILLIS;
            do {
                Future<Set<Integer>> f = peer.getControlSocketHandler().checkWhichPeersStored(message, wait_millis);
                try {
                    peer.send(message);
                    System.out.println("Sent chunk " + message.getChunkID());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    peersThatStored = f.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("checkStored future failed; aborting");
                    e.printStackTrace();
                    return;
                }
                numStored = peersThatStored.size();
                System.out.println("Perceived replication degree of " + message.getChunkID() + " is " + numStored);
                attempts++;
                wait_millis *= 2;
            } while(numStored < replicationDegree && attempts < ATTEMPTS);

            // Send UNSTORE to whoever stored the chunk and didn't need to
            if(numStored > replicationDegree){
                int numUnstoreMessages = peersThatStored.size() - replicationDegree;
                System.out.println("About to send " + numUnstoreMessages + "UNSTORE messages");
                Iterator<Integer> it = peersThatStored.iterator();
                for(int j = 0; j < numUnstoreMessages; ++j){
                    UnstoreMessage m = new UnstoreMessage(peer.getId(), message.getFileId(), message.getChunkNo(), it.next(), peer.getControlAddress());
                    try {
                        peer.send(m);
                    } catch (IOException e) {
                        System.err.println("Failed to send UNSTORE message; ignoring");
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
