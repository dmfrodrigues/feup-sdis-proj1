import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChunkedFile {
    private final File file;
    private final int chunkSize;
    private List<byte[]> chunks;
    private String fileId;

    /**
     * @brief Construct ChunkedFile.
     *
     * @param file      File to parse
     */
    public ChunkedFile(File file) throws IOException {
        this(file, 64000);
    }
    /**
     * @brief Construct ChunkedFile.
     *
     * @param file      File to parse
     * @param chunkSize Chunk size, in bytes; defaults to 64kB = 64000B
     */
    public ChunkedFile(File file, int chunkSize) throws IOException {
        this.file = file;
        this.chunkSize = chunkSize;
        chunks = new ArrayList<>();

        fileId = createFileId();
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

    private String createFileId() throws IOException {
        // Create digester
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert digest != null;

        // Digest metadata
        String metadata = file.getPath();
        byte[] metadata_bytes = metadata.getBytes();
        digest.update(metadata_bytes, 0, metadata_bytes.length);

        // Digest file contents
        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        byte[] buffer = new byte[8192];
        int count;
        while ((count = inputStream.read(buffer)) > 0) {
            digest.update(buffer, 0, count);
        }
        inputStream.close();

        byte[] hash = digest.digest();
        return Utils.bytesToHexString(hash);
    }

    public String getFileId(){
        return fileId;
    }
}
