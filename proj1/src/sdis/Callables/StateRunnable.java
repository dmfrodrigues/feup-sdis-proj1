package sdis.Callables;

import sdis.Peer;
import sdis.Storage.ChunkStorageManager;
import sdis.Storage.FileTable;

import java.io.File;
import java.util.Objects;

public class StateRunnable extends ProtocolRunnable {

    private final Peer peer;
    private final ChunkStorageManager storageManager;
    private String status;

    public StateRunnable(Peer peer, ChunkStorageManager storageManager){
        this.peer = peer;
        this.storageManager = storageManager;
    }

    private String getFilesInitiatedInfo(){
        FileTable fileTable = peer.getFileTable();
        String info = "\nBackup initiated files\n";
        if(fileTable.getFilenames().isEmpty())
            info += "\tNone\n";
        for(String filename:  fileTable.getFilenames()){
            info += "\n";
            info += "Filename: " + filename  + "\n";
            info += "File ID : " + fileTable.getFileID(filename) + "\n";
            info += "Desired Replication Degree: " + fileTable.getFileDesiredRepDegree(fileTable.getFileID(filename)) + "\n";
            info += "\nChunks\n";
            // file chunks
            for(int i=0; i<fileTable.getNumberChunks(filename); i++){
                info += "\tChunk ID           : " + fileTable.getFileID(filename) + "-" + i + "\n";
                info += "\tPerceived Rep. Deg.: " + fileTable.getActualRepDegree( fileTable.getFileID(filename) + "-" + i)  + "\n";
            }
            info += "\n";
        }
        info += "\n";
        return info;
    }


    private String getStoredChunksInfo(){
        FileTable fileTable = peer.getFileTable();
        String info = "Stored Chunks\n";
        final File storage = new File(storageManager.getPath());

        if(Objects.requireNonNull(storage.listFiles()).length == 0)
            info += "\tNone\n";

        for (File file : Objects.requireNonNull(storage.listFiles())) {
                info += "\tChunk ID     : " + file.getName() + "\n";
                info += "\tSize (KBytes): " + file.length()/1000 + "\n";
                info += "\tDesired Replication Degree: " + fileTable.getChunkDesiredRepDegree(file.getName()) + "\n";
                info += "\tPerceived Replication Degree: " + fileTable.getActualRepDegree(file.getName()) + "\n\n";
        }
        info += "\n";
        return info;
    }

    private String getPeerStorageInfo(){
        String info = "Storage Status\n";
        info += "\tCapacity      : " + storageManager.getCapacity()/1000 + "\n";
        info += "\tStorage in use: " + storageManager.getMemoryUsed()/1000 + "\n";
        return info;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public void run() {
        status = getFilesInitiatedInfo();
        status +="................................\n";
        status += getStoredChunksInfo();
        status +="................................\n";
        status += getPeerStorageInfo();
    }
}
