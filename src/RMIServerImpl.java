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
    private boolean checkNameAvailability(String name){
        for (User u:users){
            if (u.getName().equals(name)){
                return true;
            }
        }
        return false;
    }
    @Override
    public boolean register(String name, String password) throws RemoteException {
        if (!checkNameAvailability(name)){
            User new_user = new User(name,password);
            users.add(new_user);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkCredentials(String name, String password){
        for (User u:users){
            if (u.getName().equals(name)){
                if (u.getPassword().equals(password)){
                    return true;
                } else {
                    return false; //WRONG PW
                }
            }
        }
        return false; //USER NOT FOUND
    }

    @Override
    public boolean login(String name, String password) throws RemoteException {
        if (checkCredentials(name,password)){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean create_auction(String owner, int code, String title, String description, Date deadline, int amount) throws RemoteException {
        //TODO DEADLINE > ACTUAL DATE
        //TODO EMPTY ARGS
        Auction new_auc = new Auction(owner, code, title, description, deadline, amount);
        auctions.add(new_auc);
        return true;
    }

    @Override
    public ArrayList<Auction> search_auction(int code) throws RemoteException {
        ArrayList<Auction> auctions_found = new ArrayList<>();
        for (Auction a:auctions){
            if (a.getCode()==code){
                auctions_found.add(a);
            }
        }
        return auctions_found;
    }

    @Override
    public Auction detail_auction(String id) throws RemoteException {
        for (Auction a:auctions){
            if (a.getUniqueID().equals(id)){
                return a;
            }
        }
        return null;
    }

    @Override
    public ArrayList<Auction> my_auctions(String name) throws RemoteException {
        ArrayList<Auction> user_aucs = new ArrayList<>();
        for (Auction a:auctions){
            if (a.getOwner().equals(name)){
                user_aucs.add(a);
            }
        }
        return user_aucs;
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
            //TODO load users and auctions from files???
            Registry r = LocateRegistry.createRegistry(7000);
            r.rebind("sd", rmiServer);
            System.out.println("RMI Server ready.");
        } catch (RemoteException re) {
            System.out.println("Exception in RMIServerImpl.main: " + re);
        }
    }
}
