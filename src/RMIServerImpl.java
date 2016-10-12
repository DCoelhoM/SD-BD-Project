import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Date;

public class RMIServerImpl extends java.rmi.server.UnicastRemoteObject  implements RMIServer{
    private ArrayList<Auction> auctions;
    private ArrayList<User> users;

    public RMIServerImpl() throws java.rmi.RemoteException{
        super();
        auctions = new ArrayList<>();
        users = new ArrayList<>();

    }

    @Override
    public boolean register(String name, String password) throws RemoteException {
        return false;
    }

    @Override
    public boolean login(String name, String password) throws RemoteException {
        return false;
    }

    @Override
    public boolean create_auction(int code, String title, String description, Date deadline, int amount) throws RemoteException {
        return false;
    }

    @Override
    public ArrayList<Auction> search_auction() throws RemoteException {
        return null;
    }

    @Override
    public Auction detail_auction() throws RemoteException {
        return null;
    }

    @Override
    public ArrayList<Auction> my_auctions() throws RemoteException {
        return null;
    }

    @Override
    public boolean bid() throws RemoteException {
        return false;
    }

    @Override
    public boolean edit_auction() throws RemoteException {
        return false;
    }

    @Override
    public boolean message() throws RemoteException {
        return false;
    }

    @Override
    public ArrayList<String> online_users() throws RemoteException {
        return null;
    }

    @Override
    public boolean cancel_auction(int code) throws RemoteException {
        return false;
    }

    @Override
    public boolean ban_user(String name) throws RemoteException {
        return false;
    }

    @Override
    public String stats() throws RemoteException {
        return null;
    }

    public static void main(String args[]){
        try {
            RMIServerImpl rmiServer = new RMIServerImpl();
            Registry r = LocateRegistry.createRegistry(7000);
            r.rebind("sd", rmiServer);
            System.out.println("RMI Server ready.");
        } catch (RemoteException re) {
            System.out.println("Exception in RMIServerImpl.main: " + re);
        }
    }
}
