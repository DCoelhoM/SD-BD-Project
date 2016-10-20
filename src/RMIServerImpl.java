import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    public ArrayList<Auction> my_auctions(String username) throws RemoteException {
        ArrayList<Auction> user_aucs = new ArrayList<>();
        for (Auction a : auctions){
            if (a.getOwner().equals(username) || a.checkUserBidActivity(username) || a.checkUserMessageActivity(username)){
                user_aucs.add(a);
            }
        }
        return user_aucs;
    }

    @Override
    public boolean bid(int id, String username, int amount) throws RemoteException {
        for (Auction a:auctions){
            if (a.getID()==id){
                return a.addBid(username,amount);
            }
        }
        return false;
    }

    @Override
    public boolean edit_auction(String username, int id, HashMap<String,String> data) throws RemoteException {
        for (Auction a:auctions){
            if (a.getID()==id){
                if (!a.checkBids()){
                    if (data.containsKey("code")){
                        a.setCode(Long.parseLong(data.get("code")));
                    }
                    if (data.containsKey("title")){
                        a.setTitle(data.get("title"));
                    }
                    if (data.containsKey("description")){
                        a.setDescription(data.get("description"));
                    }
                    if (data.containsKey("deadline")){
                        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
                        try {
                            a.setDeadline(df.parse(data.get("deadline")));
                        } catch (ParseException e) {
                            System.out.println("Problems with parsing deadline.");
                        }
                    }
                    if (data.containsKey("amount")){
                       a.setAmount(Integer.parseInt(data.get("amount")));
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean message(int auction_id, String username, String msg) throws RemoteException {
        for (Auction a:auctions){
            if (a.getID()==auction_id){
                a.addMsg(username,msg);
                return true;
            }
        }
        return false;
    }

    @Override
    public ArrayList<String> online_users() throws RemoteException {
        return null;
    }

    @Override
    public boolean cancel_auction(int id) throws RemoteException {
        for (Auction a:auctions){
            if (a.getID()==id){
                a.cancelAuction();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean ban_user(String username) throws RemoteException {
        for (User u:users){
            if (u.getUsername().equals(username)){
                for (Auction a:auctions){
                    //Cancelar auctions do utilizador
                    if (a.getOwner().equals(username)){
                        a.cancelAuction();
                    } else if (a.checkUserBidActivity(username)){ //confirmar se fez licitações nos leilões
                        a.removeUserBids(username);
                        a.addMsg("NOTIFICATION","SORRY FOR THE PROBLEM");
                    }
                }
            }
        }
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
