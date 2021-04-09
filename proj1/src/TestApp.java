import sdis.PeerInterface;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    private static String peerAccessPoint;
    private static String operation;
    private static String filepath;
    private static int max_size;
    private static int replicationDegree;

    public static void main(String[] args) {
        if(!parseArgs(args)){
            System.out.print(getUsage());
            return;
        }

        try{
            Registry registry = LocateRegistry.getRegistry();
            PeerInterface stub = (PeerInterface) registry.lookup(peerAccessPoint);
            switch (operation){
                case "BACKUP":
                    stub.backup(filepath, replicationDegree);
                    break;
                case "RESTORE":
                    stub.restore(filepath);
                    break;
                case "DELETE":
                    stub.delete(filepath);
                    break;
                case "RECLAIM":
                    stub.reclaim(max_size);
                    break;
                case "STATE":
                    String response = stub.state();
                    System.out.println(response);
                    break;
                default:
                    break;
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static boolean parseArgs(String[] args) {
        if(args.length < 2 || args.length > 4) return false;

        peerAccessPoint = args[0];
        operation = args[1];

        switch (operation){
            case "BACKUP":
                filepath = args[2];
                replicationDegree = Integer.parseInt(args[3]);
                break;
            case "RESTORE":
            case "DELETE":
                filepath = args[2];
                break;
            case "RECLAIM":
                max_size = Integer.parseInt(args[2]);
                break;
            case "STATE":
                break;
            default:
                return false;
        }

        return true;
    }

    private static String getUsage(){
        return
                "Usage:\n"+
                        "    java TestApp PEER_ACCESS_POINT OPERATION OPERAND_1 OPERAND_2\n"+
                        "    PEER_ACCESS_POINT       Name of the remote object providing the service\n"+
                        "    OPERATION               The operation to be executed: BACKUP, RESTORE, DELETE, RECLAIM or STATE\n"+
                        "    OPERAND_1               The path name of the file to BACKUP/RESTORE/DELETE, or, in the case of RECLAIM\n"+
                        "                            the maximum amount of disk space (in KByte) that the service can use.\n"+
                        "    OPERAND_2               Replication degree of BACKUP\n"
                ;
    }
}
