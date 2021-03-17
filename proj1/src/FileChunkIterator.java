import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

public class FileChunkIterator implements Iterator<byte[]> {
    private static final int MAX_LENGTH = 1000000;

    private final File file;
    private final int chunkSize;
    private final String fileId;
    byte[] buffer;
    FileInputStream fileStream;

    /**
     * @brief Construct ChunkedFile.
     *
     * @param file      File to parse
     */
    public FileChunkIterator(Peer peer, File file) throws IOException {
        this(peer, file, 64000);
    }
    /**
     * @brief Construct ChunkedFile.
     *
     * @param file      File to parse
     * @param chunkSize Chunk size, in bytes; defaults to 64kB = 64000B
     */
    public FileChunkIterator(Peer peer, File file, int chunkSize) throws IOException {
        this.file = file;
        this.chunkSize = chunkSize;

        if(length() > MAX_LENGTH) throw new FileTooLargeException(file);

        fileId = createFileId();
        buffer = new byte[this.chunkSize];
        fileStream = new FileInputStream(this.file);

        peer.getFileTable().insert(file.getName(), fileId, length());
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

    /**
     * Length of chunked file, in chunks.
     *
     * If the file size is an exact multiple of the chunk size, an extra empty chunk is considered at the end.
     *
     * @return  Length of chunked file, in chunks
     */
    public int length(){
        long l = file.length();
        long ret = l/chunkSize + 1;
        return (int) ret;
    }

    long nextIndex = 0;

    @Override
    public boolean hasNext() {
        return nextIndex < length();
    }

    @Override
    public byte[] next() {
        int size = 0;
        try {
            size = fileStream.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ++nextIndex;
        return Arrays.copyOf(buffer, size);
    }

    @Override
    public void forEachRemaining(Consumer<? super byte[]> action) {
        while(hasNext()){
            action.accept(next());
        }
    }
}
