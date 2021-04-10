package sdis.Runnables;

import sdis.Exceptions.RestoreProtocolException;
import sdis.Messages.GetchunkMessage;
import sdis.Messages.GetchunkTCPMessage;
import sdis.Peer;
import sdis.Storage.FileChunkOutput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @brief Runnable to restore a file.
 */
public class RestoreFileCallable extends BaseProtocolCallable {

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
    public RestoreFileCallable(Peer peer, String filename) throws FileNotFoundException {
        this.peer = peer;
        this.filename = filename;

        fileChunkOutput = new FileChunkOutput(new File(filename));
    }

    @Override
    public Void call() {
        String fileId = peer.getFileTable().getFileID(filename);
        Integer numberChunks = peer.getFileTable().getNumberChunks(filename);
        for(int i = 0; i < numberChunks; ++i){
            GetchunkMessage message = new GetchunkMessage(peer.getId(), fileId, i, peer.getControlAddress());
            RestoreChunkCallable callable = new RestoreChunkCallable(peer, message);
            byte[] chunk;
            try {
                chunk = callable.call();
            } catch (RestoreProtocolException e) {
                e.printStackTrace();
                return null;
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

        return null;
    }
}
