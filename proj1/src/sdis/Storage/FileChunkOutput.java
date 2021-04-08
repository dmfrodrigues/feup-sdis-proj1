package sdis.Storage;

import sdis.Utils.FixedSizeBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @brief File chunk output.
 *
 * Is used to reconstruct a file.
 * Is in sync with a local filesystem file.
 * Buffers chunks saved using sdis.Storage.FileChunkOutput#set(int, byte[]), and writes them to the file whenever possible.
 */
public class FileChunkOutput {
    private final static int BUFFER_SIZE = 10;

    private final FileOutputStream fileOutputStream;
    private final FixedSizeBuffer<byte[]> buffer;

    /**
     * Create sdis.Storage.FileChunkOutput.
     *
     * @param file  File to sync with/write to
     * @throws FileNotFoundException If file is not found (never thrown, as file needs not exist)
     */
    public FileChunkOutput(File file) throws FileNotFoundException {
        fileOutputStream = new FileOutputStream(file);
        buffer = new FixedSizeBuffer<>(BUFFER_SIZE);
    }

    /**
     * @brief Add a chunk.
     *
     * Fails if the chunk index is too far ahead of the first missing chunk.
     *
     * @param i     Index of the chunk in the file
     * @param e     Chunk
     * @throws IOException                      If write to file fails
     * @throws ArrayIndexOutOfBoundsException   If chunk index was not accepted
     */
    public void set(int i, byte[] e) throws IOException, ArrayIndexOutOfBoundsException {
        buffer.set(i, e);
        if(buffer.hasNext()){
            fileOutputStream.write(buffer.next());
        }
    }

    /**
     * Close file.
     *
     * @throws IOException  If fails to close
     */
    public void close() throws IOException {
        fileOutputStream.close();
    }
}
