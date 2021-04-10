package sdis.Runnables;

import sdis.Messages.PutchunkMessage;
import sdis.Peer;
import sdis.Storage.FileChunkIterator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BackupFileCallable extends BaseProtocolCallable {
    /**
     * Maximum amount of chunk backup futures that can be running at a given time.
     *
     * This also serves the purpose of avoiding backup from taking too many threads.
     * But most importantly, it is to conserve memory as each running chunk backup future requires its chunk while
     * running, which can exhaust memory.
     */
    private final static int MAX_FUTURE_QUEUE_SIZE = 10;

    private final Peer peer;
    private final FileChunkIterator fileChunkIterator;
    private final int replicationDegree;

    public BackupFileCallable(Peer peer, FileChunkIterator fileChunkIterator, int replicationDegree){
        this.peer = peer;
        this.fileChunkIterator = fileChunkIterator;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public Void call() {
        int n = fileChunkIterator.length();

        peer.getFileTable().setFileDesiredRepDegree(fileChunkIterator.getFileId(), replicationDegree);

        LinkedList<Future<Void>> futureList = new LinkedList<>();

        for(int i = 0; i < n; ++i){
            // Resolve the futures that are already done
            Iterator<Future<Void>> it = futureList.iterator();
            while(it.hasNext()){
                Future<Void> f = it.next();
                if(!f.isDone()) continue;
                it.remove();
                try {
                    futureList.remove().get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println(fileChunkIterator.getFileId() + " | Aborting backup of file");
                    e.printStackTrace();
                    return null;
                }
            }
            // If the queue still has too many elements, pop the first
            while(futureList.size() >= MAX_FUTURE_QUEUE_SIZE) {
                try {
                    futureList.remove().get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println(fileChunkIterator.getFileId() + " | Aborting backup of file");
                    e.printStackTrace();
                    return null;
                }
            }
            // Add new future
            byte[] chunk = fileChunkIterator.next();
            PutchunkMessage message = new PutchunkMessage(peer.getId(), fileChunkIterator.getFileId(), i, replicationDegree, chunk, peer.getDataBroadcastAddress());
            BackupChunkCallable backupChunkCallable = new BackupChunkCallable(peer, message, replicationDegree);
            futureList.add(peer.getExecutor().submit(backupChunkCallable));
        }
        // Empty the futures list
        while(!futureList.isEmpty()) {
            try {
                futureList.remove().get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println(fileChunkIterator.getFileId() + " | Aborting backup of file");
                e.printStackTrace();
                return null;
            }
        }

        System.out.println(fileChunkIterator.getFileId() + "\t| Done backing-up file");

        return null;
    }
}
