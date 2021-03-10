import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkedFile {
    private final File file;
    private final int chunkSize;
    private List<byte[]> chunks;

    /**
     * @brief Construct ChunkedFile.
     *
     * @param file      File to parse
     */
    public ChunkedFile(File file) {
        this(file, 64000);
    }
    /**
     * @brief Construct ChunkedFile.
     *
     * @param file      File to parse
     * @param chunkSize Chunk size, in bytes; defaults to 64kB = 64000B
     */
    public ChunkedFile(File file, int chunkSize) {
        this.file = file;
        this.chunkSize = chunkSize;
        chunks = new ArrayList<>();
    }

    public void readChunks() throws IOException {
        final FileInputStream fileStream = new FileInputStream(file);

        int size;
        byte[] buffer = new byte[chunkSize];
        do {
            size = fileStream.read(buffer);
            chunks.add(Arrays.copyOf(buffer, size));
        } while(size == chunkSize);
    }

    public void writeChunks() throws IOException {
        final FileOutputStream fileStream = new FileOutputStream(file);

        for(final byte[] chunk: chunks){
            fileStream.write(chunk);
        }
    }

    public int getNumberChunks(){
        return chunks.size();
    }

    public byte[] getChunk(int n){
        return chunks.get(n);
    }

    public void setChunk(int n, byte[] chunk){
        chunks.set(n, chunk);
    }

    public void pushChunk(byte[] chunk){
        chunks.add(chunk);
    }
}
