package sdis.Callables;

import sdis.Messages.RemovedMessage;
import sdis.Peer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ReclaimRunnable extends ProtocolRunnable {

    private final Peer peer;
    private final int space_kb;

    public ReclaimRunnable(Peer peer, int space_kb) {
        this.peer = peer;
        this.space_kb = space_kb;
    }

    @Override
    public void run() {
        if(peer.getStorageManager().getMemoryUsed() > space_kb *1000){
            List<File> chunks = peer.getStorageManager().getChunks();
            for (File file : chunks) {
                RemovedMessage message = new RemovedMessage(peer.getId(),
                        file.getName().split("-", 2)[0],
                        Integer.parseInt(file.getName().split("-", 2)[1]), peer.getControlAddress());
                file.delete();
                peer.getFileTable().decrementActualRepDegree(file.getName());
                try {
                    peer.send(message);
                    System.out.println(message.getChunkID() + "\t| Sent REMOVED");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (peer.getStorageManager().getMemoryUsed() <= space_kb * 1000)
                    break;
            }
        }
        peer.getStorageManager().setMaxSize(space_kb *1000);
    }
}
