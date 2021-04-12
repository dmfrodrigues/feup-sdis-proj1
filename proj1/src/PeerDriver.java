import sdis.Peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.rmi.AlreadyBoundException;

public class PeerDriver {
    public static void main(String[] args) throws IOException, AlreadyBoundException {
        if(args.length != 9){
            System.out.println("ERROR: not enough arguments");
            System.out.print(getUsage());
            return;
        }

        String serviceAccessPoint = args[2];

        Peer peer = new Peer(
                args[0],
                Integer.parseInt(args[1]),
                new InetSocketAddress(InetAddress.getByName(args[3]), Integer.parseInt(args[4])),
                new InetSocketAddress(InetAddress.getByName(args[5]), Integer.parseInt(args[6])),
                new InetSocketAddress(InetAddress.getByName(args[7]), Integer.parseInt(args[8]))
        );

        peer.bindAsRemoteObject(serviceAccessPoint);
    }

    private static String getUsage(){
        return
            "Usage:\n"+
            "    java PeerDriver VERSION PEER_ID SERVICE_ACCESS_POINT MC MC_PORT MDB MDB_PORT MDR MDR_PORT\n"+
            "    VERSION                 Protocol version, in format <n>.<m>; usually 1.0\n"+
            "    PEER_ID                 Peer ID\n"+
            "    SERVICE_ACCESS_POINT    Service RMI access point\n"+
            "    MC                      Multicast control channel address\n"+
            "    MC_PORT                 Multicast control channel port\n"+
            "    MDB                     Multicast data broadcast channel address\n"+
            "    MDB_PORT                Multicast data broadcast channel port\n"+
            "    MDR                     Multicast data recovery channel address\n"+
            "    MDR_PORT                Multicast data recovery channel port\n"
        ;
    }
}
