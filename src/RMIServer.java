import java.util.ArrayList;
import java.util.Date;

public interface RMIServer extends java.rmi.Remote {

    //USER
    public boolean register(String name, String password) throws java.rmi.RemoteException;
    public boolean login(String name, String password) throws java.rmi.RemoteException;
    //AUCTIONS
    public boolean create_auction(String owner, int code, String title, String description, Date deadline, int amount) throws java.rmi.RemoteException;
    public ArrayList<Auction> search_auction(int code) throws java.rmi.RemoteException;
    public Auction detail_auction(String id) throws java.rmi.RemoteException;
    public ArrayList<Auction> my_auctions(String name) throws java.rmi.RemoteException;
    public boolean bid() throws java.rmi.RemoteException;
    public boolean edit_auction() throws java.rmi.RemoteException;
    public boolean message() throws java.rmi.RemoteException;
    public ArrayList<String> online_users() throws java.rmi.RemoteException;
    //ADMIN
    public boolean cancel_auction(int code) throws java.rmi.RemoteException;
    public boolean ban_user(String name) throws java.rmi.RemoteException;
    public String stats() throws java.rmi.RemoteException;

}
