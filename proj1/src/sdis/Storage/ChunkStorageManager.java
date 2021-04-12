package sdis.Storage;

import sdis.Peer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Manage peer chunk storage.
 *
 * There should be at most one instance of this class.
 */
public class ChunkStorageManager {
    private static final int BUFFER_SIZE = 80000;

    private int max_size;
    private final String path;

    public ChunkStorageManager(String path, int max_size){
        this.path = path;
        this.max_size = max_size;
        createStorage();
    }

    /**
     * @brief Creates a storage directory.
     **/
    private void createStorage(){
        File file = new File(path);

        if (file.mkdirs()) {
            System.out.println("Storage created");
        } else {
            System.out.println("Failed to create storage");
        }
    }

    public List<File> getChunks(){
        File storage = new File(path);
        return Arrays.asList(Objects.requireNonNull(storage.listFiles()));
    }

    public boolean hasChunk(String chunkID){
        File file = new File(path + "/" + chunkID);
        return file.canRead();
    }

    public CompletableFuture<byte[]> getChunk(String chunkID) throws IOException {
        AsynchronousFileChannel is = AsynchronousFileChannel.open(Path.of(path + "/" + chunkID), StandardOpenOption.READ);
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        Future<Integer> f = is.read(buffer, 0);
        return CompletableFuture.supplyAsync(() -> {
            try {
                int size = f.get();
                is.close();
                byte[] bufferArray = new byte[size];
                buffer.flip();
                buffer.get(bufferArray, 0, size);
                return bufferArray;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Peer.getExecutor());
    }

    public int getCapacity() {
        return max_size;
    }

    public String getPath() {
        return path;
    }

    public void setMaxSize(int max_size){
        this.max_size = max_size;
    }

    /**
     * @brief Get occupied space in bytes.
     *
     * @return int representing the number of bytes stored.
     **/
    public int getMemoryUsed(){
        File storage = new File(path);
        int size = 0;
        for (File file : Objects.requireNonNull(storage.listFiles()))
                size += file.length();
        return size;
    }

    /**
     * @brief Saves a chunk in the backup directory.
     *
     * @param id Chunk identifier.
     * @param chunk Byte array of the chunk to be stored.
     * @return true if successful, false otherwise.
     **/
    public boolean saveChunk(String id, byte[] chunk) throws IOException {
        if(getMemoryUsed() + chunk.length > max_size) return false;
        ByteBuffer buffer = ByteBuffer.wrap(chunk);
        AsynchronousFileChannel os = AsynchronousFileChannel.open(Path.of(path + "/" + id), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            os.write(buffer, 0).get();
        } catch (InterruptedException | ExecutionException e) {
            return false;
        }
        os.close();
        return true;
    }

    public void deleteChunk(String chunkID){
        File chunk = new File(path + "/" + chunkID);
        chunk.delete();
    }

    public void deleteFile(String fileID){
        File storage = new File(path);
        File[] chunks = Objects.requireNonNull(storage.listFiles((dir, name) -> name.startsWith(fileID)));

        for (File chunk : chunks) {
            chunk.delete();
        }
    }

}