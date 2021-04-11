package sdis.Callables;

import sdis.Exceptions.BackupProtocolException;
import sdis.Messages.PutchunkMessage;
import sdis.Messages.UnstoreMessage;
import sdis.Peer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BackupChunkSupplier extends ProtocolSupplier<Void> {
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

    public BackupChunkSupplier(Peer peer, PutchunkMessage message, int replicationDegree){

        this.peer = peer;
        this.message = message;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public Void get() {
        peer.getFileTable().setChunkDesiredRepDegree(message.getChunkID(), replicationDegree);

        Set<Integer> peersThatStored = null;
        int numStored=0, attempts=0;
        int wait_millis = WAIT_MILLIS;
        do {
            Future<Set<Integer>> f = peer.getControlSocketHandler().checkWhichPeersStored(message, wait_millis);
            try {
                peer.send(message);
                System.out.println(message.getChunkID() + "\t| Sent chunk");
            } catch (IOException e) {
                System.err.println(message.getChunkID() + "\t| Failed to send chunk");
                e.printStackTrace();
                continue;
            }
            try {
                peersThatStored = f.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println(message.getChunkID() + "\t| checkStored future failed; ignoring");
                e.printStackTrace();
                continue;
            }
            numStored = peersThatStored.size();
            System.out.println(message.getChunkID() + "\t| Perceived replication degree is " + numStored);
            attempts++;
            wait_millis *= 2;
        } while(numStored < replicationDegree && attempts < ATTEMPTS);

        if(numStored < replicationDegree || peersThatStored == null)
            throw new CompletionException(new BackupProtocolException("Failed to backup chunk " + message.getChunkID()));

        // Send UNSTORE to whoever stored the chunk and didn't need to
        if(peer.requireVersion("1.2")) {
            if (numStored > replicationDegree) {
                int numUnstoreMessages = peersThatStored.size() - replicationDegree;
                System.out.println(message.getChunkID() + "\t| About to send " + numUnstoreMessages + " UNSTORE messages");
                Iterator<Integer> it = peersThatStored.iterator();
                for (int j = 0; j < numUnstoreMessages; ++j) {
                    UnstoreMessage m = new UnstoreMessage(peer.getId(), message.getFileId(), message.getChunkNo(), it.next(), peer.getControlAddress());
                    try {
                        peer.send(m);
                    } catch (IOException e) {
                        System.err.println(m.getChunkID() + "\t| Failed to send UNSTORE message; ignoring");
                        e.printStackTrace();
                    }
                }
            }
        }

        return null;
    }
}
