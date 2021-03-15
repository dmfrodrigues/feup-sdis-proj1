import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileChunkOutput {
    private final static int BUFFER_SIZE = 10;

    private final File file;
    private final int chunkSize;

    private final FileOutputStream fileOutputStream;
    private final FixedSizeBuffer<byte[]> buffer;

    public FileChunkOutput(File file) throws FileNotFoundException {
        this(file, 64000);
    }
    public FileChunkOutput(File file, int chunkSize) throws FileNotFoundException {
        this.file = file;
        this.chunkSize = chunkSize;

        fileOutputStream = new FileOutputStream(file);
        buffer = new FixedSizeBuffer<>(BUFFER_SIZE);
    }

    public void set(int i, byte[] e) throws IOException {
        buffer.set(i, e);
        if(buffer.hasNext()){
            fileOutputStream.write(buffer.next());
        }
    }

    public int getBegin(){
        return buffer.getBegin();
    }
}
