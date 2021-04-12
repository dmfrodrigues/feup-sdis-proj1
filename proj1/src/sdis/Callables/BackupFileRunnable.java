package sdis.Callables;

import sdis.Messages.PutchunkMessage;
import sdis.Peer;
import sdis.Storage.FileChunkIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BackupFileRunnable extends ProtocolRunnable {
    /**
     * Maximum amount of chunk backup futures that can be running at a given time.
     *
     * This also serves the purpose of avoiding backup from taking too many threads.
     * But most importantly, it is to conserve memory as each running chunk backup future requires its chunk while
     * running, which can exhaust memory.
     */
    private final int MAX_FUTURE_QUEUE_SIZE;

    private final Peer peer;
    private final FileChunkIterator fileChunkIterator;
    private final int replicationDegree;

    public BackupFileRunnable(Peer peer, FileChunkIterator fileChunkIterator, int replicationDegree){
        this.peer = peer;
        this.fileChunkIterator = fileChunkIterator;
        this.replicationDegree = replicationDegree;

        MAX_FUTURE_QUEUE_SIZE = (peer.requireVersion("1.5") ? 30 : 1);
    }

    @Override
    public void run() {
        int n = fileChunkIterator.length();

        peer.getFileTable().setFileDesiredRepDegree(fileChunkIterator.getFileId(), replicationDegree);

        LinkedList<CompletableFuture<Void>> futureList = new LinkedList<>();

        for(int i = 0; i < n; ++i){
            // Resolve the futures that are already done
            Iterator<CompletableFuture<Void>> it = futureList.iterator();
            while(it.hasNext()){
                CompletableFuture<Void> f = it.next();
                if(!f.isDone()) continue;
                it.remove();
                if (!popFutureList(f)) return;
            }
            // If the queue still has too many elements, pop the first
            while(futureList.size() >= MAX_FUTURE_QUEUE_SIZE) {
                if (!popFutureList(futureList.remove())) return;
            }
            // Add new future
            int finalI = i;
            futureList.add(
                fileChunkIterator.next()
                .thenApplyAsync(chunk -> {
                    PutchunkMessage message = new PutchunkMessage(peer.getId(), fileChunkIterator.getFileId(), finalI, replicationDegree, chunk, peer.getDataBroadcastAddress());
                    BackupChunkSupplier backupChunkCallable = new BackupChunkSupplier(peer, message, replicationDegree);
                    backupChunkCallable.get();
                    return null;
                }, Peer.getExecutor())
            );
        }
        // Empty the futures list
        while(!futureList.isEmpty()) {
            if (!popFutureList(futureList.remove())) return;
        }

        System.out.println(fileChunkIterator.getFileId() + "\t| Done backing-up file");

        try {
            fileChunkIterator.close();
        } catch (IOException e) {
            System.err.println(fileChunkIterator.getFileId() + "\t| Failed to close file");
        }
    }

    private boolean popFutureList(CompletableFuture<Void> f){
        try {
            f.get();
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(fileChunkIterator.getFileId() + "\t| Aborting backup of file");
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
