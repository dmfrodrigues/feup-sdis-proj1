package sdis.Runnables;

import sdis.Exceptions.BackupProtocolException;
import sdis.Messages.PutchunkMessage;
import sdis.Messages.UnstoreMessage;
import sdis.Peer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BackupChunkCallable extends ProtocolCallable {
    /**
     * Time to wait before resending a backup request, in milliseconds.
     */
    private static final int WAIT_MILLIS = 1000;
    /**
     * Maximum attempts to transmit backup messages per chunk.
     */
    private static final int ATTEMPTS = 5;

    private final Peer peer;
    private final PutchunkMessage message;
    private final int replicationDegree;

    public BackupChunkCallable(Peer peer, PutchunkMessage message, int replicationDegree){

        this.peer = peer;
        this.message = message;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public Void call() throws BackupProtocolException {
        peer.getFileTable().setChunkDesiredRepDegree(message.getChunkID(), replicationDegree);

        Set<Integer> peersThatStored = null;
        int numStored=0, attempts=0;
        int wait_millis = WAIT_MILLIS;
        do {
            Future<Set<Integer>> f = peer.getControlSocketHandler().checkWhichPeersStored(message, wait_millis);
            try {
                peer.send(message);
                System.out.println("Sent chunk " + message.getChunkID());
            } catch (IOException e) {
                System.err.println("Failed to send chunk" + message.getChunkID());
                e.printStackTrace();
                continue;
            }
            try {
                peersThatStored = f.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("checkStored future failed; ignoring");
                e.printStackTrace();
                continue;
            }
            numStored = peersThatStored.size();
            System.out.println("Perceived replication degree of " + message.getChunkID() + " is " + numStored);
            attempts++;
            wait_millis *= 2;
        } while(numStored < replicationDegree && attempts < ATTEMPTS);

        if(numStored < replicationDegree || peersThatStored == null)
            throw new BackupProtocolException("Failed to backup chunk " + message.getChunkID());

        // Send UNSTORE to whoever stored the chunk and didn't need to
        if(peer.requireVersion("1.2")) {
            if (numStored > replicationDegree) {
                int numUnstoreMessages = peersThatStored.size() - replicationDegree;
                System.out.println("About to send " + numUnstoreMessages + " UNSTORE messages");
                Iterator<Integer> it = peersThatStored.iterator();
                for (int j = 0; j < numUnstoreMessages; ++j) {
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

        return null;
    }
}
