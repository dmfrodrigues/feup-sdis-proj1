import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Objects;

public class PeerStorage {
    private static final int max_size = 64000000;
    private static final String path = "../backup/storage";

    public PeerStorage(){
        createStorage();
    }

    private void createStorage(){
        File file = new File(path);

        if (file.mkdirs()) {
            System.out.println("Storage created");
        } else {
            System.out.println("Failed to create storage");
        }
    }

    private int getActualSize(){
        File storage= new File(path);
        int size = 0;
        for (File file : Objects.requireNonNull(storage.listFiles()))
                size += file.length();
        return size;
    }

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