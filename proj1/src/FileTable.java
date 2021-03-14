import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class FileTable {

    private final Map<String, String> table = new HashMap<String, String>();
    private static final String table_path = "fileID.properties";

    /**
     * @brief Inserts an entry in the file ID table and saves it in the local table file.
     *
     * @param filename Filename
     * @param fileID File ID
     */
    public void insert(String filename, String fileID) {
        table.put(filename, fileID);
        Properties properties = new Properties();
        for (Map.Entry<String,String> entry : table.entrySet()) {
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
    public String getFileID(String filename) {
        return table.get(filename);
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
            table.put(key, properties.get(key).toString());
        }
    }
}
