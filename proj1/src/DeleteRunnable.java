import java.io.IOException;

public class DeleteRunnable implements Runnable{

    private Peer peer;
    private String pathname;

    public DeleteRunnable(Peer peer, String pathname){
        this.peer = peer;
        this.pathname = pathname;
    }

    @Override
    public void run() {
        DeleteMessage message = new DeleteMessage(peer.getVersion(), peer.getId(), "1", peer.getDataBroadcastAddress());
        try {
            peer.send(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
