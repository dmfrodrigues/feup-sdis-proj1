package sdis.Runnables;

import sdis.Exceptions.BackupProtocolException;
import sdis.Messages.PutchunkMessage;
import sdis.Peer;
import sdis.Storage.FileChunkIterator;

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

        for(int i = 0; i < n; ++i){
            byte[] chunk = fileChunkIterator.next();
            PutchunkMessage message = new PutchunkMessage(peer.getId(), fileChunkIterator.getFileId(), i, replicationDegree, chunk, peer.getDataBroadcastAddress());
            BackupChunkCallable backupChunkCallable = new BackupChunkCallable(peer, message, replicationDegree);
            try {
                backupChunkCallable.call();
            } catch (BackupProtocolException e) {
                System.err.println("Failed to backup chunk " + message.getChunkID() + "; aborting");
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }
}
