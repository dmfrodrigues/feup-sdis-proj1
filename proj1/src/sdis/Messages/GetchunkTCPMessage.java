package sdis.Messages;

import sdis.Peer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * <Version> GETCHUNKTCP  <senderId> <fileId> <chunkNo> <address> <CRLF><CRLF>
 *  address -> IP:PORT
 */

public class GetchunkTCPMessage extends MessageWithChunkNo {

    /**
     * Address used in version 1.4, IP:PORT
     */
    private String address;

    public GetchunkTCPMessage(int senderId, String fileId, int chunkNo, String address, InetSocketAddress inetSocketAddress){
        super("1.4", "GETCHUNKTCP", senderId, fileId, chunkNo, inetSocketAddress);
        this.address = address;
    }

    public byte[] getBytes(){
        byte[] header = super.getBytes();
        byte[] term = (" " + address + "\r\n\r\n").getBytes();
        byte[] ret = new byte[header.length + term.length];
        System.arraycopy(header       , 0, ret, 0, header.length);
        System.arraycopy(term         , 0, ret, header.length, term.length);
        return ret;
    }

    public String getHostname(){
        return address.split(":")[0];
    }

    private int getPort(){
        return Integer.parseInt(address.split(":")[1]);
    }

    @Override
    public void process(Peer peer) {

        System.out.println(getChunkID() + "\t| Peer " + getSenderId() + " requested chunk");

        if(!peer.getStorageManager().hasChunk(getChunkID())) return;

        // Restore enhancement
        try {
            System.out.println(getChunkID() + "\t| Trying to connect to : "+getHostname()+":"+getPort());
            Socket socket = new Socket(getHostname(), getPort());
            // send chunk
            OutputStream output = socket.getOutputStream();
            byte[] chunk = peer.getStorageManager().getChunk(getChunkID());
            output.write(chunk);
            System.out.println(getChunkID() + "\t| Sent Chunk using TCP");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
