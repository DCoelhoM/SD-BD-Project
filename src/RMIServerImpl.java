import java.rmi.NotBoundException;
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
            System.out.println(u);
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
            return true;
        } else {
            return false;
        }
    }

    private boolean checkCredentials(String username, String password){
        for (User u : users){
            if (u.getUsername().equals(username)){
                //Wrong pw
                return u.getPassword().equals(password);
            }
        }
        return false; //username not found
    }

    @Override
    public boolean login(String username, String password) throws RemoteException {
        return checkCredentials(username, password);
    }

    @Override
    public boolean create_auction(String owner, long code, String title, String description, Date deadline, int amount) throws RemoteException {
        //TODO DEADLINE > ACTUAL DATE
        //TODO EMPTY ARGS
        Auction new_auc = new Auction(owner, code, title, description, deadline, amount);
        auctions.add(new_auc);
        return true;
    }

    @Override
    public ArrayList<Auction> search_auction(long code) throws RemoteException {
        ArrayList<Auction> auctions_found = new ArrayList<>();
        for (Auction a:auctions){
            if (a.getCode()==code){
                System.out.println(a);
                auctions_found.add(a);
            }
        }
        System.out.println(auctions_found);
        return auctions_found;

        /*
        String auctions_found = "";
        int count=0;
        for (Auction a:auctions){
            if (a.getCode()==code){
                String id_aux = ", items_" + String.valueOf(count) + "_id: " + String.valueOf(a.getID());
                String code_aux = ", items_" + String.valueOf(count) + "_code: " + String.valueOf(a.getCode());
                String title_aux = ", items_" + String.valueOf(count) + "_title: " + String.valueOf(a.getTitle());
                auctions_found += id_aux + code_aux + title_aux;
                count++;
            }
        }
        return "type: search_auction , items_count: "+String.valueOf(count)+auctions_found;
        */
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
    public boolean cancel_auction(long code) throws RemoteException {
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

    @Override
    public String ping() throws RemoteException {
        return "Pong";
    }
    static void rmiStart(){
        Registry r;
        RMIServerImpl rmiServer;
        try {
            rmiServer = new RMIServerImpl();
            r = LocateRegistry.createRegistry(7000);
            r.rebind("iBei", rmiServer);
            System.out.println("RMI Server ready.");
        } catch (RemoteException e) {
            System.out.println("Cant create registry!");
        }

    }
    static void testRMI(RMIServer RMI){
        try {
            String answer = RMI.ping();
            try {
                System.out.println(answer);
                Thread.sleep(10000);
                testRMI(RMI);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (RemoteException e) {
            rmiStart();
        }

    }

    public static void main(String args[]){
        try {
            RMIServer RMI = (RMIServer) LocateRegistry.getRegistry(7000).lookup("iBei");
            testRMI(RMI);
        } catch (RemoteException | NotBoundException e) {
            rmiStart();
        }


    }
}
