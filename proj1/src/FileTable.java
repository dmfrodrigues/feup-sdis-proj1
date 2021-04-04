import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FileTable implements Serializable {

    private static Map<String, Pair<String, Integer>> table = new HashMap<>();
    private static String table_path;

    public static Map<String, Integer> actualRepDegree = new ConcurrentHashMap<>();
    public static Map<String, Integer> chunkDesiredRepDegree = new ConcurrentHashMap<>();
    public static Map<String, Integer> fileDesiredRepDegree = new ConcurrentHashMap<>();

    public FileTable(String path) {
        table_path = path + "/fileID.ser";
    }

    /**
     * @brief Inserts an entry in the file ID table and saves it in the local table file.
     *
     * @param filename Filename
     * @param fileID File ID
     */
    public void insert(String filename, String fileID, Integer numberChunks) {
        table.put(filename, new Pair<>(fileID, numberChunks));
        save();
    }

    public void incrementActualRepDegree(String chunkID){
        int actual = actualRepDegree.getOrDefault(chunkID, 0);
        actualRepDegree.put(chunkID, actual + 1);
        save();
    }

    public void decrementActualRepDegree(String chunkID){
        int actual = actualRepDegree.getOrDefault(chunkID, 0);
        if(actual != 0)
            actualRepDegree.put(chunkID, actual - 1);
        save();
    }

    public void setFileDesiredRepDegree(String fileID, int value){
        fileDesiredRepDegree.put(fileID, value);
        save();
    }

    public void setChunkDesiredRepDegree(String chunkID, int value){
        chunkDesiredRepDegree.put(chunkID, value);
        save();
    }

    public int getActualRepDegree(String fileID){
        return actualRepDegree.getOrDefault(fileID, 0);
    }
    public int getChunkDesiredRepDegree(String fileID){
        return chunkDesiredRepDegree.getOrDefault(fileID, 0);
    }
    public int getFileDesiredRepDegree(String fileID){
        return fileDesiredRepDegree.getOrDefault(fileID, 0);
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

    public void save(){
        try {
            FileOutputStream o =
                    new FileOutputStream(table_path);
            ObjectOutputStream os = new ObjectOutputStream(o);
            os.writeObject(table);
            os.writeObject(actualRepDegree);
            os.writeObject(chunkDesiredRepDegree);
            os.writeObject(fileDesiredRepDegree);
            os.close();
            o.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @brief Loads file ID table.
     */
    public void load() {
        try {
            FileInputStream i = new FileInputStream(table_path);
            ObjectInputStream is = new ObjectInputStream(i);
            table = (Map<String, Pair<String, Integer>>) is.readObject();
            actualRepDegree= (Map<String, Integer>) is.readObject();
            chunkDesiredRepDegree= (Map<String, Integer>) is.readObject();
            fileDesiredRepDegree= (Map<String, Integer>) is.readObject();
            i.close();
            is.close();
        } catch (IOException | ClassNotFoundException ignored) {}
    }
}
