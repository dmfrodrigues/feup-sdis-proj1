package sdis.Runnables;

import sdis.Messages.PutchunkMessage;
import sdis.Peer;
import sdis.Storage.FileChunkIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BackupFileCallable extends BaseProtocolCallable {
    private final Peer peer;
    private final FileChunkIterator fileChunkIterator;
    private final int replicationDegree;

    public BackupFileCallable(Peer peer, FileChunkIterator fileChunkIterator, int replicationDegree){
        this.peer = peer;
        this.fileChunkIterator = fileChunkIterator;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public Void call() {
        int n = fileChunkIterator.length();

        peer.getFileTable().setFileDesiredRepDegree(fileChunkIterator.getFileId(), replicationDegree);

        List<Future<Void>> futureList = new ArrayList<>();

        for(int i = 0; i < n; ++i){
            byte[] chunk = fileChunkIterator.next();
            PutchunkMessage message = new PutchunkMessage(peer.getId(), fileChunkIterator.getFileId(), i, replicationDegree, chunk, peer.getDataBroadcastAddress());
            BackupChunkCallable backupChunkCallable = new BackupChunkCallable(peer, message, replicationDegree);
            futureList.add(peer.getExecutor().submit(backupChunkCallable));
        }
        for(int i = 0; i < n; ++i) {
            Future<Void> f = futureList.get(i);
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                System.err.println("Aborting backup of file " + fileChunkIterator.getFileId());
                return null;
            }
        }

        System.out.println(fileChunkIterator.getFileId() + "\t| Done backing-up file");

        return null;
    }
}
