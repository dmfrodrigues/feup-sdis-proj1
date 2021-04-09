package sdis.Runnables;

import sdis.Messages.DeleteMessage;
import sdis.Peer;

import java.io.IOException;

import static java.lang.Thread.sleep;

public class DeleteRunnable implements Runnable{

    private final Peer peer;
    private final String pathname;

    public DeleteRunnable(Peer peer, String pathname){
        this.peer = peer;
        this.pathname = pathname;
    }

    @Override
    public void run() {

        if(!peer.getFileTable().hasFile(pathname)){
            System.out.println("File does not exist in peer table");
            return;
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
        if(!peer.getVersion().equals("1.0")){
            // wait for DELETED messages
            try {
                sleep(1000);
            } catch (InterruptedException ignored) {}
            if(peer.getFileTable().getFileStoredByPeers(peer.getFileTable().getFileID(pathname)) == null)
                return;
            if(peer.getFileTable().getFileStoredByPeers(peer.getFileTable().getFileID(pathname)).size() > 0){
                peer.getFileTable().addPendingDelete(pathname);
            }
            else{
                peer.getFileTable().removePendingDelete(pathname);
            }

        }
    }
}
