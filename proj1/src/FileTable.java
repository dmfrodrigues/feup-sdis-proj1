import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FileTable {

    private static Map<String, Pair<String, Integer>> table = new HashMap<>();
    private static final String table_path = "fileID.ser";

    private static Map<String, Integer> actualRepDegree = new HashMap<>();
    private static Map<String, Integer> desiredRepDegree = new HashMap<>();

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

    public void incrementActualRepDegree(String chunkID){
        int actual = actualRepDegree.getOrDefault(chunkID, 0);
        actualRepDegree.put(chunkID, actual + 1);
    }

    public void decrementActualRepDegree(String chunkID){
        int actual = actualRepDegree.getOrDefault(chunkID, 0);
        if(actual != 0)
            actualRepDegree.put(chunkID, actual - 1);
    }

    public void setDesiredRepDegree(String chunkID, int value){
        desiredRepDegree.put(chunkID, value);
    }

    public int getActualRepDegree(String fileID){
        return actualRepDegree.getOrDefault(fileID, 0);
    }

    public int getDesiredRepDegree(String fileID){
        return desiredRepDegree.getOrDefault(fileID, 0);
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
    public boolean hasFile(String filename){
        return table.containsKey(filename);
    }

    public Set<String> getFilenames(){
        return table.keySet();
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
