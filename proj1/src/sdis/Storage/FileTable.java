package sdis.Storage;

import sdis.Utils.Pair;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileTable implements Serializable {

    private static Map<String, Pair<String, Integer>> table = new HashMap<>();
    private static String table_path;

    public static Map<String, Integer> actualRepDegree = new ConcurrentHashMap<>();
    public static Map<String, Integer> chunkDesiredRepDegree = new ConcurrentHashMap<>();
    public static Map<String, Integer> fileDesiredRepDegree = new ConcurrentHashMap<>();

    /**
     * Map of files and the peers that have stored it.
     */
    public static Map<String, HashSet<Integer>> fileStoredByPeers = new ConcurrentHashMap<>();

    /**
     * Pending files to be deleted. Files which not all peers have successfully deleted.
     */
    public static Set<String> pendingDelete = new HashSet<>();

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
    public List<String> getFileIDs(){
        List<String> fileIDs = new ArrayList<>();
        for(Pair<String, Integer> pair: table.values()){
            fileIDs.add(pair.first);
        }
        return fileIDs;
    }

    public void addPeerToFileStored(String fileID, int peerID){
        HashSet<Integer> set = fileStoredByPeers.getOrDefault(fileID, new HashSet<>());
        set.add(peerID);
        fileStoredByPeers.put(fileID, set);
    }

    public void removePeerFromFileStored(String fileID, int peerID){
        fileStoredByPeers.get(fileID).remove(peerID);
    }

    public Set<Integer> getFileStoredByPeers(String fileID){
        return fileStoredByPeers.get(fileID);
    }

    public void addPendingDelete(String path){
        pendingDelete.add(path);
    }

    public void removePendingDelete(String path){
        pendingDelete.remove(path);
    }

    public Set<String> getPendingDelete(){
        return pendingDelete;
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
            os.writeObject(fileStoredByPeers);
            os.writeObject(pendingDelete);
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
            actualRepDegree = (Map<String, Integer>) is.readObject();
            chunkDesiredRepDegree = (Map<String, Integer>) is.readObject();
            fileDesiredRepDegree = (Map<String, Integer>) is.readObject();
            fileStoredByPeers = (Map<String, HashSet<Integer>>) is.readObject();
            pendingDelete = (Set<String>) is.readObject();
            i.close();
            is.close();
        } catch (IOException | ClassNotFoundException ignored) {}
    }
}
