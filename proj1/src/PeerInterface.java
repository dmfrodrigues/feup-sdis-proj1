import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote {
    void backup(String pathname, int replicationDegree) throws IOException;
    void restore(String pathname) throws RemoteException;
    void delete(String pathname) throws RemoteException;
    void reclaim(int space_kbytes) throws RemoteException;
    void state() throws RemoteException;
}
