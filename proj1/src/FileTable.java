import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class FileTable {

    private static Map<String, Pair<String, Integer>> table = new HashMap<>();
    private static final String table_path = "fileID.ser";

    /**
     * @brief Inserts an entry in the file ID table and saves it in the local table file.
     *
     * @param filename Filename
     * @param fileID File ID
     */
    public void insert(String filename, String fileID, Integer numberChunks) {
        table.put(filename, new Pair<>(fileID, numberChunks));
        try {
            FileOutputStream o =
                    new FileOutputStream(table_path);
            ObjectOutputStream os = new ObjectOutputStream(o);
            os.writeObject(table);
            os.close();
            o.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @brief Get the file ID of a given filename.
     *
     * @param filename Name of file
     * @return String with file iD
     */
    public String  getFileID      (String filename) {
        return table.get(filename).first;
    }
    public Integer getNumberChunks(String filename) {
        return table.get(filename).second;
    }

    /**
     * @brief Loads file ID table.
     */
    public void load() {
        try {
            FileInputStream i = new FileInputStream(table_path);
            ObjectInputStream is = new ObjectInputStream(i);
            table = (HashMap) is.readObject();
            i.close();
            is.close();
        } catch (IOException | ClassNotFoundException ignored) {}
    }
}
