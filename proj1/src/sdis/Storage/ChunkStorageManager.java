package sdis.Storage;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Manage peer chunk storage.
 *
 * There should be at most one instance of this class.
 */
public class ChunkStorageManager {
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
        for(File chunk: this.getChunks()){
            if(chunk.getName().equals(chunkID))
                return true;
        }
        return false;
    }

    public byte[] getChunk(String chunkID) throws IOException {
        for(File chunk: this.getChunks()){
            if(chunk.getName().equals(chunkID))
                return Files.readAllBytes(chunk.toPath());
        }
        return null;
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
    public boolean saveChunk(String id, byte[] chunk){
        if(getMemoryUsed() + chunk.length > max_size) return false;

        try {
            OutputStream os = new FileOutputStream(path + "/" + id);
            os.write(chunk);
            os.close();
        }
        catch (Exception e) {
            System.out.println("Exception: " + e);
        }

        return true;
    }

    public void deleteFile(String fileID){
        File storage = new File(path);
        File[] chunks = Objects.requireNonNull(storage.listFiles((dir, name) -> name.startsWith(fileID)));

        for (File chunk : chunks) {
            chunk.delete();
        }
    }

}