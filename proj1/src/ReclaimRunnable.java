public class ReclaimRunnable implements Runnable {

    private Peer peer;
    private int space_kbytes;

    public ReclaimRunnable(Peer peer, int space_kbytes) {
        this.peer = peer;
        this.space_kbytes = space_kbytes;
    }

    @Override
    public void run() {

    }
}
