import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RMIServerImpl extends java.rmi.server.UnicastRemoteObject  implements RMIServer{
    private List<Auction> auctions;
    private List<User> users;

    public RMIServerImpl() throws java.rmi.RemoteException{
        super();
        auctions = Collections.synchronizedList(new ArrayList<>());
        users = Collections.synchronizedList(new ArrayList<>());
    }
    private boolean checkUsernameAvailability(String username){
        for (User u:users){
            if (u.getUsername().equals(username)){
                return false;
            }
        }
        return true;
    }
    @Override
    public boolean register(String username, String password) throws RemoteException {
        if (checkUsernameAvailability(username)){
            User new_user = new User(username, password);
            users.add(new_user);
            System.out.println(new_user);
            return true;
        } else {
            return false;
        }
    }

    private boolean checkCredentials(String username, String password){
        for (User u : users){
            if (u.getUsername().equals(username)){
                System.out.println(password);
                //Wrong pw
                return u.getPassword().equals(password);
            }
        }
        return false; //username not found
    }

    @Override
    public boolean login(String username, String password) throws RemoteException {
        System.out.println(password);
        return checkCredentials(username, password);
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
    public Auction detail_auction(int id) throws RemoteException {
        for (Auction a:auctions){
            if (a.getID()==id){
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
    public boolean bid(int id, int amount) throws RemoteException {
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
            r.rebind("iBei", rmiServer);
            System.out.println("RMI Server ready.");
        } catch (RemoteException re) {
            System.out.println("Exception in RMIServerImpl.main: " + re);
        }
    }
}
