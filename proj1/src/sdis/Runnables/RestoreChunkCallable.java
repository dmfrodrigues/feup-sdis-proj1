package sdis.Runnables;

import sdis.Messages.GetchunkMessage;
import sdis.Messages.GetchunkTCPMessage;
import sdis.Peer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RestoreChunkCallable extends ProtocolCallable<byte[]> {
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

    public RestoreChunkCallable(Peer peer, GetchunkMessage message) {
        this.peer = peer;
        this.message = message;
    }

    public byte[] call() {
        byte[] chunk = null;

        for(int attempt = 0; attempt < ATTEMPTS && chunk == null; ++attempt) {

            // Restore enhancement
            if(peer.requireVersion("1.4")){
                try {
                    ServerSocket serverSocket = new ServerSocket(0);
                    serverSocket.setSoTimeout((int) SOCKET_TIMEOUT_MILLIS);

                    System.out.println("listening on address: " + InetAddress. getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort() );
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
                    System.out.println("Failed to establish a connection: " + e.toString());
                }
            }

            // Make request
            Future<byte[]> f;
            try {
                f = peer.getDataRecoverySocketHandler().request(message);
            } catch (IOException e) {
                System.err.println("Failed to make request, trying again");
                e.printStackTrace();
                continue;
            }
            System.out.println("Asked for chunk " + message.getChunkID());

            // Wait for request to be satisfied
            try {
                chunk = f.get(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                System.err.println("Future execution interrupted, trying again");
                e.printStackTrace();
            } catch (ExecutionException e) {
                System.err.println("Future execution caused an exception, trying again");
                e.printStackTrace();
            } catch (TimeoutException e) {
                f.cancel(true);
                System.err.println("Timed out waiting for CHUNK, trying again");
                e.printStackTrace();
            }


        }

        return chunk;
    }
}
