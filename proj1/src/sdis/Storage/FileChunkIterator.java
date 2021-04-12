package sdis.Storage;

import sdis.Peer;
import sdis.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @brief File chunk iterator.
 *
 * Iterator over chunks of an existing file.
 * Is used to read a file one chunk at a time.
 */
public class FileChunkIterator implements Iterator<CompletableFuture<byte[]>> {
    private static final int MAX_LENGTH = 1000000;

    private final File file;
    private final int chunkSize;
    private final String fileId;
    byte[] buffer;
    AsynchronousFileChannel fileStream;

    /**
     * @brief Construct FileChunkIterator.
     *
     * @param file      File to parse
     */
    public FileChunkIterator(File file) throws IOException {
        this(file, 64000);
    }
    /**
     * @brief Construct FileChunkIterator.
     *
     * @param file      File to parse
     * @param chunkSize Chunk size, in bytes; defaults to 64kB = 64000B
     */
    public FileChunkIterator(File file, int chunkSize) throws IOException {
        this.file = file;
        this.chunkSize = chunkSize;

        if(length() > MAX_LENGTH) throw new FileTooLargeException(file);

        buffer = new byte[this.chunkSize];
        fileStream = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ);
        fileId = createFileId();
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
        AsynchronousFileChannel inputStream = AsynchronousFileChannel.open(file.toPath(), StandardOpenOption.READ);
        ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
        int count, position = 0;
        try {
            while ((count = inputStream.read(buffer, position).get()) > 0) {
                buffer.flip();
                digest.update(buffer);
                buffer.clear();
                position += count;
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new IOException("Failed to read file contents to create file ID");
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
    public synchronized boolean hasNext() {
        return nextIndex < length();
    }

    @Override
    public synchronized CompletableFuture<byte[]> next() {
        ByteBuffer buffer = ByteBuffer.allocate(chunkSize);
        Future<Integer> f = fileStream.read(buffer, nextIndex*chunkSize);
        ++nextIndex;
        return CompletableFuture.supplyAsync(() -> {
            try {
                int size = f.get();
                byte[] bufferArray = new byte[size];
                buffer.flip();
                buffer.get(bufferArray, 0, size);
                return bufferArray;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, Peer.getExecutor());
    }

    public void close() throws IOException {
        fileStream.close();
    }
}
