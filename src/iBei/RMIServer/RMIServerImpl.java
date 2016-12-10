package iBei.RMIServer;

import java.sql.*;

import iBei.TCPServer.TCPServer;
import iBei.Auxiliar.*;

import javax.xml.transform.Result;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;



public class RMIServerImpl extends java.rmi.server.UnicastRemoteObject  implements RMIServer{
    private List<Auction> auctions;
    private List<User> users;
    private Map<String,String> online_users; //{Username, TCP_Host:Port}
    private Map<String,TCPServer> connected_TCPs; //{TCP_Host:Port, TCPServer}
    private List<Map.Entry<String,String>> notifications; //{Username, Message}

    private static Statement statement = null;
    private static ResultSet resultSet = null;
    private static PreparedStatement preparedStatement = null;
    private static ConnectionPoolManager connectionPoolManager;



    public RMIServerImpl() throws java.rmi.RemoteException{
        super();
        auctions = Collections.synchronizedList(new ArrayList<>());
        users = Collections.synchronizedList(new ArrayList<>());
        online_users = Collections.synchronizedMap(new HashMap<>());
        connected_TCPs = Collections.synchronizedMap(new HashMap<>());
        notifications = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Method to save an instance of TCPServer when it connects to RMI
     */
    @Override
    public void addTCPServer(TCPServer tcp, String host_port) throws RemoteException{
        connected_TCPs.put(host_port, tcp);
    }

    /**
     * Method to count the number of users connected to a TCPServer
     */
    @Override
    public int checkNumberUsers(String host_port) throws RemoteException {
        int counter = 0;
        for(Map.Entry<String,String> u : online_users.entrySet()){
            if(u.getValue().equals(host_port)){
                counter ++;
            }
        }
        return counter;
    }

    /**
     * Method to check if a username is available
     */

    private boolean checkUsernameAvailability(String username){

        ResultSet rs;

        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            preparedStatement = db_connection.prepareStatement("select username from user u where u.username = ?");
            preparedStatement.setString(1, username);
        } catch (SQLException e) {
            System.out.println("Error preparing query");
        }
        try {
            rs = preparedStatement.executeQuery();
            rs.next();
            if(rs.getString("username").equals(username)){

                connectionPoolManager.returnConnectionToPool(db_connection);
                return false;
            }
        } catch (SQLException e) {
            System.out.println("Error executing query to check username availability from database");
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
        return true;
    }

    /**
     * Method to register new user
     */
    @Override
    public boolean register(String username, String password) throws RemoteException {

        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        if (checkUsernameAvailability(username)){
            try {
                preparedStatement = db_connection.prepareStatement("insert into user (username, password, state) values (?, ?, ?)");
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, password);
                preparedStatement.setString(3, "active");
                System.out.println("preparei a query para criar novo user");
            } catch (SQLException e) {
                System.out.println("Error preparing query");
            }
            try {
                preparedStatement.executeUpdate();
                db_connection.commit();
                System.out.println("dei commit");
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Error executing query to insert user into database");
                try {
                    db_connection.rollback();
                } catch (SQLException e1) {
                    System.out.println("Error rolling back in register");
                }
            }
            connectionPoolManager.returnConnectionToPool(db_connection);
            return true;
        } else {
            connectionPoolManager.returnConnectionToPool(db_connection);
            return false;
        }
    }

    /**
     * Method to check if username and password are valid
     */
    private boolean checkCredentials(String username, String password){

        ResultSet rs;

        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            preparedStatement = db_connection.prepareStatement("select state from user u where u.username = ? and u.password = ?");
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
        } catch (SQLException e) {
            System.out.println("Error preparing query");
        }
        try {
            rs = preparedStatement.executeQuery();
            rs.next();
            if(rs.getString("state").equals("active")){
                connectionPoolManager.returnConnectionToPool(db_connection);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error executing query to select user from database");
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
        return false; //username and/or password not found
    }

    /**
     * Method to save username and host:port to online_users
     */
    private void addOnlineUser(String username, String tcp_host_port) throws RemoteException {
        online_users.put(username, tcp_host_port);
        this.saveOnlineUsers();
    }

    /**
     * Method to check if user is already logged
     */
    public boolean userAlreadyLogged(String username){
        return online_users.containsKey(username);
    }


    /**
     * Method to login a user
     */
    @Override
    public boolean login(String username, String password, String tcp_host_port) throws RemoteException {
        if(!userAlreadyLogged(username)){
            if(checkCredentials(username, password)){
                addOnlineUser(username,tcp_host_port);
                return true;
            }
        }
        return false;
    }

    /**
     * Method to logout a user
     */
    @Override
    public boolean logout(String username) throws RemoteException {
        return removeOnlineUser(username);
    }

    /**
     * Method to remove user from online_users
     */
    private boolean removeOnlineUser(String username) throws RemoteException {
        if (online_users.containsKey(username)){
            online_users.remove(username);
            saveOnlineUsers();
            return true;
        }
        return false;
    }

    /**
     * Method to create a new auction
     */

    // type: create_auction, code: 123152152, title: 1928, description: benfica campeao, deadline: 2017-01-01 14:51, amount: 10
    @Override
    synchronized public boolean create_auction(String owner, String code, String title, String description, Date deadline, double amount) throws RemoteException {
        Auction new_auc = new Auction(owner, code, title, description, deadline, amount);
        auctions.add(new_auc);

        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            preparedStatement = db_connection.prepareStatement("insert into auction (username, state, code, title, description, created_date, deadline, amount) values (?, ?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setString(1, owner);
            preparedStatement.setString(2, "active");
            preparedStatement.setString(3, code);
            preparedStatement.setString(4, title);
            preparedStatement.setString(5, description);
            java.sql.Timestamp date = new java.sql.Timestamp(new java.util.Date().getTime());
            preparedStatement.setTimestamp(6, date);
            Timestamp timestamp = new Timestamp(deadline.getTime());
            preparedStatement.setTimestamp(7, timestamp);
            preparedStatement.setDouble(8, amount);
        } catch (SQLException e) {
            System.out.println("Error preparing query");
        }
        try {
            preparedStatement.executeUpdate();
            db_connection.commit();
        } catch (SQLException e) {
            System.out.println("Error executing query to insert auction into database");
            try {
                db_connection.rollback();
            } catch (SQLException e1) {
                System.out.println("Error rolling back in create_auction");
            }
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
        return true;
    }

    /**
     * Method to search for various auctions with a specific code
     */
    @Override
    public ArrayList<Auction> search_auction(String code) throws RemoteException {
        ArrayList<Auction> auctions_found = new ArrayList<>();
        ResultSet rs;

        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            preparedStatement = db_connection.prepareStatement("select * from auction a where a.code = ?");
            preparedStatement.setString(1, code);
        } catch (SQLException e) {
            System.out.println("Error preparing query");
        }
        try {
            rs = preparedStatement.executeQuery();
            while(rs.next()){
                auctions_found.add(new Auction(rs.getString("username"), rs.getString("code"), rs.getString("title"),
                        rs.getString("description"), new Date(rs.getTimestamp("deadline").getTime()), rs.getDouble("amount")));
            }
        } catch (SQLException e) {
            System.out.println("Error executing query to select auction by code from database");
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
        return auctions_found;
    }
    
    
    private ResultSet get_auction_messages(int id){
        
        ResultSet rs = null;

        Connection db_connection = connectionPoolManager.getConnectionFromPool();


        try {
            preparedStatement = db_connection.prepareStatement("select COUNT(*) total_messages, m.username username, m.text text from (SELECT m.username, m.text FROM message m WHERE m.auction_id = ?) aux, message m group by m.username, m.text");
            preparedStatement.setInt(1, id);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            rs = preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
        return rs;
    }

    private ResultSet get_auction_bids(int id){

        ResultSet rs = null;

        Connection db_connection = connectionPoolManager.getConnectionFromPool();


        try {
            preparedStatement = db_connection.prepareStatement("select COUNT(*) total_bids, b.username username, b.amount amount from (SELECT b.username, b.amount FROM bid b WHERE b.auction_id = ?) aux, bid b group by b.username, b.amount");
            preparedStatement.setInt(1, id);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            rs = preparedStatement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
        return rs;
    }


    /**
     * Method to searh for an auction by id
     */
    @Override
    public Auction detail_auction(int id) throws RemoteException {

        ResultSet rs = null, messages, bids;
        Auction auction = null;

        Connection db_connection = connectionPoolManager.getConnectionFromPool();


        try {
            preparedStatement = db_connection.prepareStatement("SELECT * FROM auction WHERE auction.id = ?;");
            preparedStatement.setInt(1, id);
        } catch (SQLException e) {
            System.out.println("Error preparing query");
        }
        try {
            rs = preparedStatement.executeQuery();
            rs.next();
            if(rs == null) return null;
            auction = new Auction(rs.getString("username"), rs.getString("code"), rs.getString("title"), rs.getString("description"),
                    new Date(rs.getTimestamp("deadline").getTime()), rs.getDouble("amount"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        messages = get_auction_messages(id);
        bids = get_auction_bids(id);

        try {
            while(messages.next()){
                auction.addMsg(messages.getString("username"), messages.getString("text"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            while(bids.next()){
                auction.addBid(bids.getString("username"), bids.getDouble("amount"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
        return auction;
    }

    /**
     * Method to list all auctions from a specific user
     */
    @Override
    public ArrayList<Auction> my_auctions(String username) throws RemoteException {
        ArrayList<Auction> user_aucs = new ArrayList<>();

        ResultSet rs;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            preparedStatement = db_connection.prepareStatement("select * from auction a where a.username = ?");
            preparedStatement.setString(1, username);
        } catch (SQLException e) {
            System.out.println("Error preparing query");
        }
        try {
            rs = preparedStatement.executeQuery();
            while(rs.next()){
                user_aucs.add(new Auction(rs.getString("username"), rs.getString("code"), rs.getString("title"),
                        rs.getString("description"), new Date(rs.getTimestamp("deadline").getTime()), rs.getDouble("amount")));
            }
        } catch (SQLException e) {
            System.out.println("Error executing query to select auction by code from database");
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
        return user_aucs;
    }


    private boolean insertIntoBidNotification(Connection db_connection, String username, int bid_id){

        try {
            preparedStatement = db_connection.prepareStatement("INSERT INTO bid_notification (bid_id, username) VALUES (?, ?)");
            preparedStatement.setInt(1, bid_id);
            preparedStatement.setString(2, username);
            preparedStatement.executeUpdate();
            db_connection.commit();
            connectionPoolManager.returnConnectionToPool(db_connection);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            connectionPoolManager.returnConnectionToPool(db_connection);
            return false;
        }
    }
    /**
     * Method to bid on a specific auction
     */
    @Override
    public boolean bid(int id, String username, double amount) throws RemoteException {

        PreparedStatement ps = null;
        String userToNotify = "";
        String owner = "";
        boolean bids = false;
        int bid_id = 0;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            ps = db_connection.prepareStatement("SELECT a.username owner, b.amount amount, b.username username, b.id bid_id FROM bid b, auction a WHERE b.bid_date = (SELECT MAX(bid_date) FROM bid WHERE auction_id = ?) AND a.id = b.auction_id AND a.state = 'active'");

            ps.setInt(1, id);

            resultSet = ps.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
                bids = true;
                userToNotify = resultSet.getString("username");
                owner = resultSet.getString("owner");
                bid_id = resultSet.getInt("bid_id") + 1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error preparing query");
        }
        try {
            if(!bids || resultSet.getDouble("amount") > amount){
                // TODO: passar amount como parâmetro para tirar este if
                preparedStatement = db_connection.prepareStatement("INSERT INTO bid (auction_id, username, bid_date, amount) VALUES (?, ?, ?, ?)");
                preparedStatement.setInt(1, id);
                preparedStatement.setString(2, username);
                java.sql.Timestamp date = new java.sql.Timestamp(new java.util.Date().getTime());
                preparedStatement.setTimestamp(3, date);
                preparedStatement.setDouble(4, amount);
                preparedStatement.executeUpdate();
                db_connection.commit();

                if (!userToNotify.equals(owner)){
                    if (!bids){
                        ps = db_connection.prepareStatement("SELECT username owner from auction where id = ?");
                        ps.setInt(1, id);
                        resultSet = ps.executeQuery();
                        resultSet.next();
                        owner = resultSet.getNString("owner");
                    }
                    insertIntoBidNotification(db_connection, owner, 1);
                }
                if (bids) {
                    insertIntoBidNotification(db_connection, userToNotify, bid_id);
                }
                connectionPoolManager.returnConnectionToPool(db_connection);
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Error executing query to insert bid into database");
            try {
                db_connection.rollback();
            } catch (SQLException e1) {
                System.out.println("Error rolling back in create_auction");
            }
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
        return false;
    }

    /**
     * Method to edit various or a specific attribute of an auction
     */
    @Override
    public boolean edit_auction(String username, int id, HashMap<String,String> data) throws RemoteException {

        PreparedStatement ps = null;
        String title = "";
        String description = "";
        Timestamp deadline = null;

        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            ps = db_connection.prepareStatement("SELECT * FROM auction a WHERE a.id = ? AND a.username = ?");
            ps.setInt(1, id);
            ps.setString(2, username);

            resultSet = ps.executeQuery();
            resultSet.next();
            title = resultSet.getString("title");
            description = resultSet.getString("description");
            deadline = resultSet.getTimestamp("deadline");
        } catch (SQLException e) {
            connectionPoolManager.returnConnectionToPool(db_connection);
            return false;
        }

        try {
            preparedStatement = db_connection.prepareStatement("INSERT INTO auction_history (auction_id, title, description, deadline, edited) values (?,?,?,?,?)");
            preparedStatement.setInt(1, id);
            preparedStatement.setString(2, title);
            preparedStatement.setString(3, description);
            preparedStatement.setTimestamp(4, deadline);
            java.sql.Timestamp edited = new java.sql.Timestamp(new java.util.Date().getTime());
            preparedStatement.setTimestamp(5, edited);

        } catch (SQLException e) {
            System.out.println("Error preparing query");
        }
        try {
            preparedStatement.executeUpdate();
            db_connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error executing query to insert auction into database");
            try {
                db_connection.rollback();
            } catch (SQLException e1) {
                System.out.println("Error rolling back in create_auction");
            }
        }

        try {
            preparedStatement = db_connection.prepareStatement("UPDATE auction SET title = ?, description = ?, deadline = ? WHERE id = ?");
            if (data.containsKey("title")){
                title = data.get("title");
            }
            if (data.containsKey("description")){
                description = data.get("description");
            }
            if (data.containsKey("deadline")){
                DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH-mm");
                try {
                    deadline = new java.sql.Timestamp(df.parse(data.get("deadline")).getTime());
                } catch (ParseException e) {
                    System.out.println("Problems with parsing deadline.");
                }
            }
            preparedStatement.setString(1, title);
            preparedStatement.setString(2, description);
            preparedStatement.setTimestamp(3, deadline);
            preparedStatement.setInt(4, id);

            preparedStatement.executeUpdate();
            db_connection.commit();
        } catch (SQLException e) {
            try {
                db_connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
        return true;
    }

    /**
     * Method to check if user is online
     */
    private String checkIfUserOnline(String username){
        if(online_users.containsKey(username)){
            return online_users.get(username);
        }
        return "";
    }

    /**
     * Method to get the TCP instance by host:port
     */
    private TCPServer getTCPbyHostPort(String tcp_host_port){
        if (connected_TCPs.containsKey(tcp_host_port)){
            return connected_TCPs.get(tcp_host_port);
        }
        return null;
    }

    /**
     * Method to remove disconnected TCP and their users
     */
    private void removeTCPandUsers(String host_port){
        System.out.println(online_users);
        connected_TCPs.remove(host_port);
        ArrayList<String> users_offline = new ArrayList<>();
        for (Map.Entry<String, String> u: online_users.entrySet()){
            if (u.getValue().equals(host_port)){
                users_offline.add(u.getKey());
            }
        }
        for (String u: users_offline) {
            online_users.remove(u);
        }
        saveOnlineUsers();
        System.out.println(online_users);
    }


    private ResultSet getBids(){
        PreparedStatement ps = null;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            ps = db_connection.prepareStatement("SELECT bn.bid_id bid_id, bn.username username, b.amount amount, b.username bidder, b.auction_id FROM bid_notification bn, bid b where bn.bid_id = b.id");
            resultSet = ps.executeQuery();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
        return resultSet;
    }

    private void removeBidsNotifications(int bid_id){
        PreparedStatement ps = null;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            System.out.println("apagar das bids notifications");
            ps = db_connection.prepareStatement("DELETE FROM bid_notification WHERE bid_id = ?");
            ps.setInt(1, bid_id);
            ps.executeUpdate();
            db_connection.commit();


        } catch (SQLException e) {
            try {
                db_connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
    }

    /*
    private ResultSet getMessages(){
        PreparedStatement ps = null;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            ps = db_connection.prepareStatement("SELECT bn.bid_id bid_id, bn.username username, b.amount amount, b.username bidder FROM bid_notification bn, bid b where bn.bid_id = b.id");
            resultSet = ps.executeQuery();
            resultSet.next();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return resultSet;
    }*/

    /**
     * Method to send a notification if the user is online
     */
    @Override
    public void sendNotification(String type){
        System.out.println("entrei no send notification");

        ResultSet bids = null;
        ResultSet messages;

        int bid_id;
        String username;
        Double amount;
        String bidder;
        String host_port = null;
        int auction_id;

        if(type.equals("both")){
            System.out.println("both");
            bids = getBids();
            try {
                while(bids.next()){
                    System.out.println("LUL 1");
                    bid_id = bids.getInt("bid_id");
                    username = bids.getString("username");
                    amount = bids.getDouble("amount");
                    bidder = bids.getString("bidder");
                    auction_id = bids.getInt("auction_id");
                    System.out.println("quem vamos notificar" + username);
                    System.out.println(online_users);
                    host_port = checkIfUserOnline(username);
                    if (!host_port.isEmpty()) {
                        System.out.println("LUL 2");
                        TCPServer tcp = getTCPbyHostPort(host_port);
                        if (tcp != null) {
                            System.out.println("LUL 3");
                            String note = "type: notification_bid, id: " + auction_id + ", user: " + bidder + ", amount: " + amount;
                            System.out.println("vou mandar");
                            tcp.sendNotification(username, note);
                            System.out.println("mandei");
                            removeBidsNotifications(bid_id);
                        }
                    }
                }
            } catch (SQLException | RemoteException e) {
                e.printStackTrace();
            }

            //messages = getMessages();
        } else if(type.equals("bid")){
            bids = getBids();
        } else if(type.equals("message")){
            //messages = getMessages();
        }


    }

    /**
     * Method to add a message to a specific auction
     */
    @Override
    public boolean message(int auction_id, String username, String msg) throws RemoteException {
        for (Auction a:auctions){
            if (a.getID()==auction_id){
                a.addMsg(username,msg);
                String note =  "type: notification_message, id: "+ auction_id +", user: " + username + ", text:" + msg;
                ArrayList<String> users_to_notify = a.getParticipants();
                for(String u:users_to_notify){
                    synchronized (notifications){notifications.add(new AbstractMap.SimpleEntry<>(u, note));}
                }
                //saveAuctions();
                return true;
            }
        }
        return false;
    }

    /**
     * Method to list all online users
     */
    @Override
    public ArrayList<String> online_users() throws RemoteException {

        ArrayList<String> users_online = new ArrayList<>();
        for (Map.Entry<String,String> online_user:online_users.entrySet()){
            users_online.add(online_user.getKey());
        }
        return users_online;
    }

    /**
     * Method to end auctions when the deadline ends
     */
    public void end_auctions(){
        boolean flag = false;
        for (Auction a:auctions){
            if (a.getDeadline().before(new Date()) && a.getState().equals("active")){
                a.endAuction();
                synchronized (notifications){
                    notifications.add(new AbstractMap.SimpleEntry<>(a.getUsernameLastBid(), "type: notification_auction_won, text: You have won the auction with the following id: "+a.getID()));
                }
                sendNotification("both");
                flag = true;
            }
        }
        if (flag){
            //saveAuctions();
        }
    }

    /**
     * Method to cancel an auction (Admin permission)
     */
    @Override
    public boolean cancel_auction(int id) throws RemoteException {
        for (Auction a:auctions){
            if (a.getID()==id && a.getState().equals("active")){
                a.cancelAuction();
                //saveAuctions();
                return true;
            }
        }
        return false;
    }

    /**
     * Method to ban a user (Admin permission)
     */
    @Override
    public boolean ban_user(String username) throws RemoteException {
        for (User u:users){
            if (u.getUsername().equals(username) && u.getState().equals("active")){
                u.ban();
                for (Auction a:auctions){
                    //Cancelar auctions do utilizador
                    if (a.getOwner().equals(username)){
                        a.cancelAuction();
                    } else if (a.checkUserBidActivity(username)){ //confirmar se fez licitações nos leilões
                        a.removeUserBids(username);
                        message(a.getID(),"Admin","User "+ username +" was banned, sorry for the inconvenience!");
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Method that counts users auctions and returns it ordered
     */
    @Override
    public Map mostAuctionsUsers() throws RemoteException {

        HashMap<String, Integer> usersAuctions = new HashMap<>();

        for(Auction a:auctions){
            // if user is already in the hashmap, +=1 the number of auctions created
            if(usersAuctions.containsKey(a.getOwner())){
                usersAuctions.put(a.getOwner(), usersAuctions.get(a.getOwner()) + 1);
            } else {
                usersAuctions.put(a.getOwner(), 1);
            }
        }

        Map<Integer, String> map = sortByValues(usersAuctions);
        return map;
    }

    /**
     * Method that counts users won auctions and returns it ordered
     */
    @Override
    public Map userWithMostAuctionsWon() throws RemoteException {

        HashMap<String, Integer> usersAuctions = new HashMap<>();

        for(Auction a:auctions){
            // if user is already in the hashmap, +=1 the number of auctions created
            if(a.getState().equals("ended")){
                if (a.getNumberBids()>0) {
                    if (usersAuctions.containsKey(a.getUsernameLastBid())) {
                        usersAuctions.put(a.getUsernameLastBid(), usersAuctions.get(a.getUsernameLastBid()) + 1);
                    } else {
                        usersAuctions.put(a.getUsernameLastBid(), 1);
                    }
                }
            }
        }

        Map<Integer, String> map = sortByValues(usersAuctions);
        return map;
    }

    /**
     * Method to sort a HashMap by value
     */
    private static HashMap sortByValues(HashMap map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue())
                        .compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        // Here I am copying the sorted list in HashMap
        // using LinkedHashMap to preserve the insertion order
        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }

    /**
     * Method to list all the auctions created in the last 10 days
     */
    @Override
    public ArrayList<Auction> auctionsInTheLast10Days(){
        ArrayList<Auction> listAuctionsInTheLast10Days = new ArrayList<>();
        Date now = new Date();
        for(Auction a : auctions){
            long diff = now.getTime() - a.getCreationDate().getTime();
            if(TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) <= 10){
                listAuctionsInTheLast10Days.add(a);
            }
        }
        return listAuctionsInTheLast10Days;
    }


    /**
     * Method to check RMI connection and at the same time transfer data between RMI's
     */
    @Override
    public DataTransfer ping() throws RemoteException {
        String filename = "id.txt";
        TextFile previous_id = new TextFile();
        int auc_prev_id=0;
        try {
            previous_id.openRead(filename);
            auc_prev_id=Integer.valueOf(previous_id.readLine());
            previous_id.closeRead();
        } catch (IOException e) {
            try {
                //file doesnt exist -> create new file
                previous_id.openWriteOW(filename);
                previous_id.writeLine("0");
                previous_id.closeWrite();
            } catch (IOException e1) {
                System.out.println("Problem with read/create id file.");
            }
        }
        return new DataTransfer(auctions,users,online_users,notifications,auc_prev_id);
    }


    /**
     * Method to save online_users to a file
     */
    public void saveOnlineUsers(){
        ObjectFile file = new ObjectFile();
        try {
            file.openWrite("online_users");
            try {
                file.writeObject(this.online_users);
            } catch (IOException e) {
                System.out.println("Problem saving online_users");
            }
            try {
                file.closeWrite();
            } catch (IOException e) {
                System.out.println("Problem closing online_users file(WRITE MODE).");
            }
        } catch (IOException e) {
            System.out.println("Problem opening online_users file(WRITE MODE)");
        }

    }


    /**
     * Method to load online_users from a file
     */
    public void loadOnlineUsers(){
        ObjectFile file = new ObjectFile();
        try {
            file.openRead("online_users");

            try {
                this.online_users = (Map<String,String>) file.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Problem loading online_users");
            }

            try {
                file.closeRead();
            } catch (IOException e) {
                System.out.println("Problem closing users online_file(READ MODE)");
            }
        } catch (IOException e) {
            System.out.println("Problem opening online_users file(READ MODE)(No online_users found)");
        }
    }

    /**
     * Method to save all data received from primary RMI via ping() method
     */
    public void saveDataFromPrimaryRMI(DataTransfer data){
        this.auctions = data.getAuctions();
        this.users = data.getUsers();
        this.online_users = data.getOnline_users();
        this.notifications = data.getNotifications();
        saveOnlineUsers();
    }

    /**
     * Method that initializes RMI and starts a Thread that calls end_auctions() with a minute interval
     */
    static void rmiStart(int port){
        Registry r;
        RMIServerImpl rmiServer;
        try {
            rmiServer = new RMIServerImpl();
            r = LocateRegistry.createRegistry(port);
            r.rebind("iBei", rmiServer);

            // Thread para acabar com leilões à hora certa
            new Thread() {
                public void run() {
                    while(true) {
                        rmiServer.end_auctions();
                        try {
                            Thread.sleep(40000);
                        } catch (InterruptedException e) {
                            System.out.println("Problem with end auctions thread!");
                        }
                    }
                }
            }.start();
            System.out.println("RMI Server ready.");
        } catch (RemoteException e) {
            System.out.println("Cant create registry!");
        }

    }

    /**
     * Method that checks if primary RMI is up, and if not calls rmiStart()
     */
    static void testRMI(RMIServer RMI, int port){
        try {
            DataTransfer dataFromOtherRMI = RMI.ping();
            new RMIServerImpl().saveDataFromPrimaryRMI(dataFromOtherRMI);
            String filename = "id.txt";
            TextFile previous_id = new TextFile();
            try {
                previous_id.openWriteOW(filename);
                previous_id.writeLine(String.valueOf(dataFromOtherRMI.getLast_auc_id()));
                previous_id.closeWrite();
            } catch (java.io.IOException e) {
                System.out.println("Problem with save id file.");
            }
            System.out.println("Data saved with success.");
            try {
                Thread.sleep(1000);
                testRMI(RMI, port);
            } catch (InterruptedException e) {
                System.out.println("Problem with testRMI sleep");
            }

        } catch (RemoteException e) {
            rmiStart(port);
        }

    }

    /**
     * Method to init RMIServer
     */
    public static void main(String args[]){

        connectionPoolManager = new ConnectionPoolManager();

        String host_aux_rmi = "localhost";
        int port_aux_rmi=7000, port=7000;
        if (args.length==3) {
            host_aux_rmi = args[0];
            port_aux_rmi = Integer.parseInt(args[1]);
            port = Integer.parseInt(args[2]);
        }
        try {
            RMIServer RMI = (RMIServer) LocateRegistry.getRegistry(host_aux_rmi,port_aux_rmi).lookup("iBei");
            testRMI(RMI, port);
        } catch (RemoteException | NotBoundException e) {
            rmiStart(port);
        }
    }
}


class ConnectionPoolManager{

    String databaseUrl = "jdbc:mysql://localhost:3306/ibei";
    String userName = "root";
    String password = "ibei";

    Vector connectionPool = new Vector();

    public ConnectionPoolManager(){
        initialize();
    }

    public ConnectionPoolManager(String databaseUrl, String userName, String password){
        this.databaseUrl = databaseUrl;
        this.userName = userName;
        this.password = password;
        initialize();
    }

    private void initialize() {
        //Here we can initialize all the information that we need
        initializeConnectionPool();
    }

    private void initializeConnectionPool() {
        while(!checkIfConnectionPoolIsFull()) {
            System.out.println("Connection Pool is NOT full. Proceeding with adding new connections");
            //Adding new connection instance until the pool is full
            connectionPool.addElement(createNewConnectionForPool());
        }
        System.out.println("Connection Pool is full.");
    }

    private synchronized boolean checkIfConnectionPoolIsFull() {
        final int MAX_POOL_SIZE = 5;

        //Check if the pool size
        if(connectionPool.size() < MAX_POOL_SIZE) {
            return false;
        }
        return true;
    }

    //Creating a connection
    private Connection createNewConnectionForPool() {
        Connection connection = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(databaseUrl, userName, password);
            connection.setAutoCommit(false);
            System.out.println("Connection: "+connection);
        }
        catch(SQLException sqle) {
            System.err.println("SQLException: "+sqle);
            return null;
        }
        catch(ClassNotFoundException cnfe) {
            System.err.println("ClassNotFoundException: "+cnfe);
            return null;
        }

        return connection;
    }

    public synchronized Connection getConnectionFromPool() {
        Connection connection = null;

        //Check if there is a connection available. There are times when all the connections in the pool may be used up
        if(connectionPool.size() == 0){
            createNewConnectionForPool();
        }
        connection = (Connection) connectionPool.firstElement();
        connectionPool.removeElementAt(0);

        //Giving away the connection from the connection pool
        return connection;
    }

    public synchronized void returnConnectionToPool(Connection connection) {
        //Adding the connection from the client back to the connection pool
        connectionPool.addElement(connection);
    }
}