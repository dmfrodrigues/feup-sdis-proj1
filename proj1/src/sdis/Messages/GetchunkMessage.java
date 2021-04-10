package sdis.Messages;

import sdis.Peer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class GetchunkMessage extends MessageWithChunkNo {
    /**
     * How much a peer receiving this message should wait (and sense MDR) before answering
     */
    private static final int RESPONSE_TIMEOUT_MILLIS = 400;

    /**
     * Address used in version 1.3, IP:PORT
     */
    private String address = null;

    public GetchunkMessage(int senderId, String fileId, int chunkNo, InetSocketAddress inetSocketAddress){
        super("1.0", "GETCHUNK", senderId, fileId, chunkNo, inetSocketAddress);
    }

    public GetchunkMessage(int senderId, String fileId, int chunkNo, String address, InetSocketAddress inetSocketAddress){
        super("1.4", "GETCHUNK", senderId, fileId, chunkNo, inetSocketAddress);
        this.address = address;
    }

    public byte[] getBytes(){
        if(!getVersion().equals("1.4")){
            byte[] header = super.getBytes();
            byte[] term = ("\r\n\r\n").getBytes();
            byte[] ret = new byte[header.length + term.length];
            System.arraycopy(header       , 0, ret, 0, header.length);
            System.arraycopy(term         , 0, ret, header.length, term.length);
            return ret;
        }
        else{
            byte[] header = super.getBytes();
            byte[] term = ("\r\n"+ address +"\r\n").getBytes();
            byte[] ret = new byte[header.length + term.length];
            System.arraycopy(header       , 0, ret, 0, header.length);
            System.arraycopy(term         , 0, ret, header.length, term.length);
            return ret;
        }
    }

    public String getHostname(){
        return address.split(":")[0];
    }

    private int getPort(){
        return Integer.parseInt(address.split(":")[1]);
    }

    @Override
    public void process(Peer peer) {


        System.out.println("Peer " + getSenderId() + " requested chunk " + getChunkID());
        if(!peer.getStorageManager().hasChunk(getChunkID()) || !peer.requireVersion(getVersion())) return;

        byte[] chunk;

        // Restore enhancement
        if(getVersion().equals("1.4")){
            try {
                System.out.println("Trying to connect to : "+getHostname()+":"+getPort());
                Socket socket = new Socket(getHostname(), getPort());
                // send chunk
                OutputStream output = socket.getOutputStream();
                chunk = peer.getStorageManager().getChunk(getChunkID());
                output.write(chunk);
                System.out.println("Sent Chunk");
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if(peer.getDataRecoverySocketHandler().sense(this, 400)) return;

        try {
            chunk = peer.getStorageManager().getChunk(getChunkID());
        } catch (IOException e) {
            System.err.println("Failed to ask if this peer has chunk with ID " + getChunkID());
            e.printStackTrace();
            return;
        }
        ChunkMessage message = new ChunkMessage(peer.getId(), getFileId(), getChunkNo(), chunk, peer.getDataRecoveryAddress());
        try {
            peer.send(message);
        } catch (IOException e) {
            System.err.println("Failed to answer GetchunkMessage with a ChunkMessage");
            e.printStackTrace();
        }
        System.out.println("Sent chunk " + message.getChunkID());
    }
}
