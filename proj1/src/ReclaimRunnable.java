import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ReclaimRunnable implements Runnable {

    private Peer peer;
    private int space_kbytes;

    public ReclaimRunnable(Peer peer, int space_kbytes) {
        this.peer = peer;
        this.space_kbytes = space_kbytes;
    }

    @Override
    public void run() {
        if(peer.getStorageManager().getMemoryUsed() > space_kbytes*1000){
            List<File> chunks = peer.getStorageManager().getChunks();
            Iterator<File> itr = chunks.iterator();
            while(itr.hasNext()){
                File file = itr.next();

                RemovedMessage message = new RemovedMessage(peer.getVersion(), peer.getId(),
                        file.getName().split("-", 2)[0],
                        Integer.parseInt(file.getName().split("-", 2)[1]), peer.getControlAddress());
                file.delete();
                peer.getFileTable().decrementActualRepDegree(file.getName());
                try {
                    peer.send(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(peer.getStorageManager().getMemoryUsed() <= space_kbytes*1000)
                    break;
            }
        }
        peer.getStorageManager().setMaxSize(space_kbytes*1000);
    }
}
