import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Manage peer storage.
 *
 * There should be at most one instance of this class.
 */
public class PeerStorage {
    private static final int max_size = 64000000;
    private final String path;

    public PeerStorage(String path){
        this.path = path;
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

    /**
     * @brief Get occupied space in bytes.
     *
     * @return int representing the number of bytes stored.
     **/
    private int getActualSize(){
        File storage= new File(path);
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
        if(getActualSize() + chunk.length > max_size) return false;

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

}