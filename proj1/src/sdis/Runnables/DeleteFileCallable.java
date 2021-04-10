package sdis.Runnables;

import sdis.Messages.DeleteMessage;
import sdis.Peer;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DeleteFileCallable extends BaseProtocolCallable {

    private final Peer peer;
    private final String pathname;

    public DeleteFileCallable(Peer peer, String pathname){
        this.peer = peer;
        this.pathname = pathname;
    }

    @Override
    public Void call() {

        if(!peer.getFileTable().hasFile(pathname)){
            System.out.println("File " + pathname + " does not exist in peer table");
            return null;
        }

        DeleteMessage message = new DeleteMessage(peer.getId(),
                peer.getFileTable().getFileID(pathname), peer.getControlAddress());

        peer.getStorageManager().deleteFile(peer.getFileTable().getFileID(pathname));

        try {
            peer.send(message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Delete Enhancement
        if(peer.requireVersion("1.1")){
            // wait for DELETED messages, returns number of deleted messages
            Future<Integer> f = peer.getControlSocketHandler().checkDeleted(message, 1000);

            if(peer.getFileTable().getFileStoredByPeers(peer.getFileTable().getFileID(pathname)) == null)
                return null;

            int notDeleted = 0;
            try {
                notDeleted = f.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            if(notDeleted > 0){
                for(Integer i: peer.getFileTable().getFileStoredByPeers(peer.getFileTable().getFileID(pathname))){
                    peer.getFileTable().addPeerPendingDelete(i, pathname);
                }

            }
            else{
                peer.getFileTable().removePathFromPeersPendingDelete(pathname);
            }

        }

        return null;
    }
}
