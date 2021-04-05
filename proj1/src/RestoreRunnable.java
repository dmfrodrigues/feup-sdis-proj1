import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RestoreRunnable implements Runnable {
    private final Peer peer;
    private final String filename;

    private final FileChunkOutput fileChunkOutput;

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
            System.out.println("Trying to get chunk " + i);
            GetchunkMessage message = new GetchunkMessage(peer.getVersion(), peer.getId(), fileId, i, peer.getControlAddress());
            try {
                Future<byte[]> f = peer.getDataRecoverySocketHandler().request(message);
                System.out.println("    Asked for chunk " + i);
                byte[] chunk = f.get();
                System.out.println("    Got chunk " + i);
                fileChunkOutput.set(i, chunk);
            } catch (IOException | ExecutionException | InterruptedException ignored) {}

        }

    }
}
