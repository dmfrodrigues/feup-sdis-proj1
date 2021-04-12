package sdis.Callables;

import sdis.Exceptions.RestoreProtocolException;
import sdis.Messages.GetchunkMessage;
import sdis.Messages.GetchunkTCPMessage;
import sdis.Peer;
import sdis.Utils.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class RestoreChunkSupplier extends ProtocolSupplier<Pair<Integer,byte[]>> {
    /**
     * Timeout of waiting for a CHUNK response to a GETCHUNK message, in milliseconds.
     */
    private final static long TIMEOUT_MILLIS = 1000;
    /**
     * Timeout of waiting for a response in socket.
     */
    private final static long SOCKET_TIMEOUT_MILLIS = 500;
    /**
     * Number of attempts before giving up to receive CHUNK.
     */
    private final static int ATTEMPTS = 5;

    private final Peer peer;
    private final GetchunkMessage message;

    public RestoreChunkSupplier(Peer peer, GetchunkMessage message) {
        this.peer = peer;
        this.message = message;
    }

    public Pair<Integer,byte[]> get() {
        byte[] chunk = null;

        for(int attempt = 0; attempt < ATTEMPTS && chunk == null; ++attempt) {

            // Restore enhancement
            if(peer.requireVersion("1.4")){
                try {
                    ServerSocket serverSocket = new ServerSocket(0);
                    serverSocket.setSoTimeout((int) SOCKET_TIMEOUT_MILLIS);

                    System.out.println(message.getChunkID() + "\t| Listening on address: " + InetAddress. getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort() );
                    peer.send(
                        new GetchunkTCPMessage(
                            peer.getId(),
                            message.getFileId(),
                            message.getChunkNo(),
                            InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort(),
                            peer.getControlAddress()
                        )
                    );
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout((int) SOCKET_TIMEOUT_MILLIS);

                    //Reads chunk
                    InputStream input = socket.getInputStream();

                    chunk = input.readAllBytes();

                    socket.close();
                    serverSocket.close();
                    if (chunk != null) break;
                } catch (IOException e) {
                    System.out.println(message.getChunkID() + "\t| Failed to establish a connection: " + e);
                }
            }

            // Make request
            Future<byte[]> f;
            try {
                f = peer.getDataRecoverySocketHandler().request(message, TIMEOUT_MILLIS);
            } catch (IOException e) {
                System.err.println(message.getChunkID() + "\t| Failed to make request, trying again");
                e.printStackTrace();
                continue;
            }
            System.out.println(message.getChunkID() + "\t| Asked for chunk");

            // Wait for request to be satisfied
            try {
                chunk = f.get();
            } catch (InterruptedException e) {
                System.err.println(message.getChunkID() + "\t| Future execution interrupted, trying again");
                e.printStackTrace();
            } catch (ExecutionException e) {
                System.err.println(message.getChunkID() + "\t| Future execution caused an exception, trying again");
                e.printStackTrace();
            }

            if(chunk == null){
                System.err.println(message.getChunkID() + "\t| Timed out waiting for CHUNK, trying again");
            }
        }

        if(chunk == null) throw new CompletionException(new RestoreProtocolException("Could not restore chunk " + message.getChunkID()));

        return new Pair<>(message.getChunkNo(), chunk);
    }
}
