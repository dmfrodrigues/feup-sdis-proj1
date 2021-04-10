package sdis.Runnables;

import sdis.Messages.GetchunkMessage;
import sdis.Peer;
import sdis.Storage.FileChunkOutput;
import sdis.Utils.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @brief Runnable to restore a file.
 */
public class RestoreFileCallable extends BaseProtocolCallable {
    /**
     * Maximum amount of chunk backup futures that can be running at a given time.
     *
     * This also serves the purpose of avoiding restore from taking too many threads.
     * But most importantly, it is to conserve memory as each running chunk restoring future requires its chunk while
     * running, which can exhaust memory.
     */
    private final int MAX_FUTURE_QUEUE_SIZE;

    private final Peer peer;
    private final String filename;

    private final FileChunkOutput fileChunkOutput;
    private final String fileId;

    /**
     * @brief Construct sdis.Runnables.RestoreRunnable.
     *
     * @param peer      sdis.Peer asking to restore a file
     * @param filename  File name of the file to be restored
     * @throws FileNotFoundException    If file is not found
     */
    public RestoreFileCallable(Peer peer, String filename) throws FileNotFoundException {
        this.peer = peer;
        this.filename = filename;
        fileChunkOutput = new FileChunkOutput(new File(filename));
        fileId = peer.getFileTable().getFileID(filename);

        MAX_FUTURE_QUEUE_SIZE = (peer.requireVersion("1.5") ? 10 : 1);
    }

    @Override
    public Void call() {
        Integer numberChunks = peer.getFileTable().getNumberChunks(filename);

        Queue<Future<Pair<Integer,byte[]>>> futureList = new LinkedList<>();

        for(int i = 0; i < numberChunks; ++i){
            // Resolve the futures that are already done
            while(!futureList.isEmpty() && futureList.peek().isDone()){
                if (!popFutureList(futureList.remove())) return null;
            }
            // If the queue still has too many elements, pop the first
            while(futureList.size() >= MAX_FUTURE_QUEUE_SIZE) {
                if (!popFutureList(futureList.remove())) return null;
            }
            // Add new future
            GetchunkMessage message = new GetchunkMessage(peer.getId(), fileId, i, peer.getControlAddress());
            RestoreChunkCallable restoreChunkCallable = new RestoreChunkCallable(peer, message);
            futureList.add(peer.getExecutor().submit(restoreChunkCallable));
        }
        // Empty the futures list
        while(!futureList.isEmpty()) {
            if (!popFutureList(futureList.remove())) return null;
        }

        try {
            fileChunkOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private boolean popFutureList(Future<Pair<Integer, byte[]>> f) {
        try {
            Pair<Integer,byte[]> chunk = f.get();
            fileChunkOutput.set(chunk.first, chunk.second);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println(fileId + " | Aborting restore of file");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.err.println(fileId + " | Failed to save chunk to fileChunkOutput");
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
