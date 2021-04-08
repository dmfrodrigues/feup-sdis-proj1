package sdis.Runnables;

import sdis.Messages.DeleteMessage;
import sdis.Peer;

import java.io.IOException;

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
    }
}
