package sdis.Runnables;

import sdis.Messages.GetchunkMessage;
import sdis.Peer;
import sdis.Storage.FileChunkOutput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @brief Runnable to restore a file.
 */
public class RestoreRunnable implements Runnable {
    /**
     * Timeout of waiting for a CHUNK response to a GETCHUNK message, in milliseconds.
     */
    private final static long TIMEOUT_MILLIS = 1000;
    /**
     * Number of attempts before giving up to receive CHUNK.
     */
    private final static int ATTEMPTS = 5;

    private final Peer peer;
    private final String filename;

    private final FileChunkOutput fileChunkOutput;

    /**
     * @brief Construct sdis.Runnables.RestoreRunnable.
     *
     * @param peer      sdis.Peer asking to restore a file
     * @param filename  File name of the file to be restored
     * @throws FileNotFoundException    If file is not found
     */
    public RestoreRunnable(Peer peer, String filename) throws FileNotFoundException {
        this.peer = peer;
        this.filename = filename;

        fileChunkOutput = new FileChunkOutput(new File(filename));
    }

    @Override
    public void run() {
        String fileId = peer.getFileTable().getFileID(filename);
        Integer numberChunks = peer.getFileTable().getNumberChunks(filename);
        for(int i = 0; i < numberChunks; ++i){
            GetchunkMessage message = new GetchunkMessage(peer.getId(), fileId, i, peer.getControlAddress());
            byte[] chunk = null;
            for(int attempt = 0; attempt < ATTEMPTS && chunk == null; ++attempt) {
                // Make request
                Future<byte[]> f;
                try {
                    f = peer.getDataRecoverySocketHandler().request(message);
                } catch (IOException e) {
                    System.err.println("Failed to make request, trying again");
                    e.printStackTrace();
                    continue;
                }
                System.out.println("Asked for chunk " + message.getChunkID());

                // Wait for request to be satisfied
                try {
                    chunk = f.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    System.err.println("Future execution interrupted, trying again");
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    System.err.println("Future execution caused an exception, trying again");
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    f.cancel(true);
                    System.err.println("Timed out waiting for CHUNK, trying again");
                    e.printStackTrace();
                }
            }
            System.out.println("Promise completed, received chunk " + message.getChunkID());
            try {
                fileChunkOutput.set(i, chunk);
            } catch (IOException e) {
                System.err.println("Failed to set chunk to file chunk output");
                e.printStackTrace();
                break;
            }
        }
        try {
            fileChunkOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
