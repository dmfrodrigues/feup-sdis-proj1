package sdis;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface PeerInterface extends Remote {
    void backup(String pathname, int replicationDegree) throws IOException;
    void restore(String pathname) throws IOException;
    void delete(String pathname) throws RemoteException;
    void reclaim(int space_kb) throws RemoteException;
    String state() throws RemoteException;
}
