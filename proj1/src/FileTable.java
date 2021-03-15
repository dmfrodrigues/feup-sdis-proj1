import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileTable {

    private final Map<String, Pair<String, Integer>> table = new HashMap<>();
    private static final String table_path = "fileID.properties";

    /**
     * @brief Inserts an entry in the file ID table and saves it in the local table file.
     *
     * @param filename Filename
     * @param fileID File ID
     */
    public void insert(String filename, String fileID, Integer numberChunks) {
        table.put(filename, new Pair<>(fileID, numberChunks));
        Properties properties = new Properties();
        for (Map.Entry<String,Pair<String,Integer>> entry : table.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }
        try {
            properties.store(new FileOutputStream(table_path), null);
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
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(table_path));
        } catch (IOException ignored) {}
        for (String key : properties.stringPropertyNames()) {
            table.put(key, (Pair<String, Integer>) properties.get(key));
        }
    }
}
