package iBei.src.iBei.RMIServer;
import iBei.src.iBei.TCPServer.TCPServer;
import iBei.src.iBei.Auxiliar.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public interface RMIServer extends java.rmi.Remote {

    //TCP
    void addTCPServer(TCPServer tcp, String host_port) throws java.rmi.RemoteException;
    int checkNumberUsers(String host_port) throws java.rmi.RemoteException;

    //USER
    boolean register(String username, String password) throws java.rmi.RemoteException;
    boolean login(String username, String password, String tcp_host_port) throws java.rmi.RemoteException;
    boolean logout(String username) throws java.rmi.RemoteException;
    boolean userAlreadyLogged(String username) throws java.rmi.RemoteException;

    //AUCTIONS
    public void checkIfThereAreMessagesForUser(String username) throws  java.rmi.RemoteException;
    boolean create_auction(String owner, String code, String title, String description, Date deadline, double amount) throws java.rmi.RemoteException;
    ArrayList<Auction> search_auction(String code) throws java.rmi.RemoteException;
    Auction detail_auction(int id) throws java.rmi.RemoteException;
    ArrayList<Auction> my_auctions(String username) throws java.rmi.RemoteException;
    boolean bid(int id, String username, double amount) throws java.rmi.RemoteException;
    boolean edit_auction(String username, int id, HashMap<String,String> data) throws java.rmi.RemoteException;
    boolean message(int auction_id, String name, String msg) throws java.rmi.RemoteException;
    ArrayList<String> online_users() throws java.rmi.RemoteException;

    //void sendNotification(String type) throws java.rmi.RemoteException;

    void checkIfThereAreNotificationsForUser(String username) throws java.rmi.RemoteException;

        //ADMIN
    boolean cancel_auction(int id) throws java.rmi.RemoteException;
    boolean ban_user(String username) throws java.rmi.RemoteException;
    Map mostAuctionsUsers() throws java.rmi.RemoteException;
    Map userWithMostAuctionsWon() throws java.rmi.RemoteException;
    ArrayList<Auction> auctionsInTheLast10Days() throws java.rmi.RemoteException;

    //Test RMI
    DataTransfer ping() throws  java.rmi.RemoteException;

}
