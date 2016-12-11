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

import static java.awt.SystemColor.text;


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
                System.out.println("user logou e adicionei aos online users");
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
        System.out.println("user logou e removi dos online users");
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

    // type: create_auction, code: 123152152, title: 1928, description: benfica campeao, deadline: 2017-01-01 14-51, amount: 200
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


    private boolean insertIntoBidNotification(String username, int bid_id){
        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        PreparedStatement ps = null;

        try {
            ps = db_connection.prepareStatement("INSERT INTO bid_notification (bid_id, username) VALUES (?, ?)");
            ps.setInt(1, bid_id);
            ps.setString(2, username);
            ps.executeUpdate();
            db_connection.commit();
            connectionPoolManager.returnConnectionToPool(db_connection);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            connectionPoolManager.returnConnectionToPool(db_connection);
            return false;
        }
    }




    private boolean insertIntoMessageNotification(String username, int message_id){
        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        System.out.println("entrei np insertInto!!!!!!!!----------");
        PreparedStatement ps;

        try {
            ps = db_connection.prepareStatement("INSERT INTO message_notification (message_id, username) VALUES (?, ?)");
            ps.setInt(1, message_id);
            System.out.println(" o message_id é : " + message_id );
            ps.setString(2, username);
            System.out.println(" o username é : " + username);

            ps.executeUpdate();
            db_connection.commit();
            System.out.println("dei commit!");
            connectionPoolManager.returnConnectionToPool(db_connection);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                db_connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            connectionPoolManager.returnConnectionToPool(db_connection);
            return false;
        }
    }

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


    /**
     * Method to bid on a specific auction
     */
    @Override

    public boolean bid(int auction_id, String bidder, double amount) throws RemoteException {
        PreparedStatement ps = null;
        PreparedStatement queryToInsertBid = null;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        ResultSet last_bid;
        ResultSet get_owner;
        String auction_owner;
        String userWhoIsWinning;
        double last_bid_amount;
        double initial_amount;
        int last_bid_id;
        String host_port;
        int bid_id_after_insert;

        try {
            ps = db_connection.prepareStatement("SELECT a.username owner, b.amount amount, b.username username, b.id bid_id FROM bid b, auction a WHERE b.bid_date = (SELECT MAX(bid_date) FROM bid WHERE auction_id = ?) AND a.id = b.auction_id AND a.state = 'active'");
            ps.setInt(1, auction_id);

            last_bid = ps.executeQuery();
            System.out.println("recebi a response");
            if(last_bid.next()){ // acontece se alguém já lá tiver uma bid
                auction_owner = last_bid.getString("owner");
                userWhoIsWinning = last_bid.getString("username");
                System.out.println("O user que está a ganhar e vai ser notificado é + " + userWhoIsWinning);
                last_bid_amount = last_bid.getDouble("amount");
                last_bid_id = last_bid.getInt("bid_id");

                if(amount < last_bid_amount){ // bid válida
                    queryToInsertBid = db_connection.prepareStatement("INSERT INTO bid (auction_id, username, bid_date, amount) values (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                    queryToInsertBid.setInt(1, auction_id);
                    queryToInsertBid.setString(2, bidder);
                    java.sql.Timestamp date = new java.sql.Timestamp(new java.util.Date().getTime());
                    queryToInsertBid.setTimestamp(3, date);
                    queryToInsertBid.setDouble(4, amount);

                    queryToInsertBid.executeUpdate();

                    ResultSet keys = queryToInsertBid.getGeneratedKeys();
                    keys.next();
                    bid_id_after_insert = keys.getInt(1);

                    db_connection.commit();

                    host_port = checkIfUserOnline(userWhoIsWinning);
                    if(!host_port.isEmpty()){
                        System.out.println("user que estava a ganhar está on");
                        System.out.println(userWhoIsWinning);
                        TCPServer tcp = getTCPbyHostPort(host_port);
                        if(tcp != null){
                            System.out.println("antes de note");
                            String note = "type: notification_bid, id: " + auction_id + " user: " + bidder + " amount: " + amount;
                            tcp.sendNotification(userWhoIsWinning, note);
                        }
                    } else {
                        insertIntoBidNotification(userWhoIsWinning, bid_id_after_insert);
                    }

                    host_port = checkIfUserOnline(auction_owner);
                    if(!host_port.isEmpty()){
                        TCPServer tcp = getTCPbyHostPort(host_port);
                        if(tcp != null){
                            String note = "type: notification_bid, id: " + auction_id + " user: " + bidder + " amount: " + amount;
                            tcp.sendNotification(auction_owner, note);
                            connectionPoolManager.returnConnectionToPool(db_connection);
                            return true;
                        }
                    } else {
                        insertIntoBidNotification(auction_owner, bid_id_after_insert);
                        connectionPoolManager.returnConnectionToPool(db_connection);
                        return true;
                    }
                }
            } else {
                ps = db_connection.prepareStatement("SELECT username owner, amount initial_amount FROM auction WHERE id = ?");
                ps.setInt(1, auction_id);
                System.out.println(auction_id);

                get_owner = ps.executeQuery();
                get_owner.next();
                auction_owner = get_owner.getString("owner");
                initial_amount = get_owner.getDouble("initial_amount");

                if(amount < initial_amount){
                    queryToInsertBid = db_connection.prepareStatement("INSERT INTO bid (auction_id, username, bid_date, amount) values (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                    queryToInsertBid.setInt(1, auction_id);
                    queryToInsertBid.setString(2, bidder);
                    java.sql.Timestamp date = new java.sql.Timestamp(new java.util.Date().getTime());
                    queryToInsertBid.setTimestamp(3, date);
                    queryToInsertBid.setDouble(4, amount);

                    queryToInsertBid.executeUpdate();

                    ResultSet keys = queryToInsertBid.getGeneratedKeys();
                    keys.next();
                    bid_id_after_insert = keys.getInt(1);

                    db_connection.commit();

                    host_port = checkIfUserOnline(auction_owner);
                    if(!host_port.isEmpty()){
                        TCPServer tcp = getTCPbyHostPort(host_port);
                        if(tcp != null){
                            String note = "type: notification_bid, id: " + auction_id + " user: " + bidder + " amount: " + amount;
                            tcp.sendNotification(auction_owner, note);
                            connectionPoolManager.returnConnectionToPool(db_connection);
                            return true;
                        }
                    } else {
                        insertIntoBidNotification(auction_owner, bid_id_after_insert);
                        connectionPoolManager.returnConnectionToPool(db_connection);
                        return true;
                    }

                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
        return false;
    }

    public boolean message(int auction_id, String messager, String text) throws RemoteException {
        System.out.println("ENTREI NO MESSAGE");
        PreparedStatement ps;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        int response;
        ResultSet responseUsers;
        String userToNotify = "";
        String host_port = "";
        int id_message;

        try {
            ps = db_connection.prepareStatement("INSERT INTO message (auction_id, username, message_date, text) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);

            ps.setInt(1, auction_id);
            ps.setString(2, messager);
            java.sql.Timestamp date = new java.sql.Timestamp(new java.util.Date().getTime());
            ps.setTimestamp(3, date);
            ps.setString(4, text);
            System.out.println("ANTES DO EXECUTE UPDATE");
            System.out.println(auction_id);
            System.out.println(messager);
            System.out.println(date);
            System.out.println(text);
            response = ps.executeUpdate();
            System.out.println("depois do execute hu3");
            db_connection.commit();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            id_message = keys.getInt(1);
            System.out.println(id_message);

            System.out.println("DEPOIS");


            System.out.println("A RESPOSTA FOI ===================="+ response);
            if (response == 1){
                ps = db_connection.prepareStatement("SELECT DISTINCT username userToNotify FROM message WHERE username <> ? AND auction_id = ?");
                ps.setString(1, messager);
                ps.setInt(2, auction_id);
                responseUsers = ps.executeQuery();
                while(responseUsers.next()){
                    userToNotify = responseUsers.getString("userToNotify");
                    host_port = checkIfUserOnline(userToNotify);
                    if(!host_port.isEmpty()){
                        System.out.println("user que estava a ganhar está on");
                        TCPServer tcp = getTCPbyHostPort(host_port);
                        if(tcp != null){
                            System.out.println("antes de note");
                            String note = "type: notification_message, id: " + auction_id + ", user: " + userToNotify + ", text: " + text;
                            tcp.sendNotification(userToNotify, note);
                        }
                    } else {
                        System.out.println("o mano esta off");
                        insertIntoMessageNotification(userToNotify, id_message);
                    }
                }
                connectionPoolManager.returnConnectionToPool(db_connection);
                return true;
            }

        } catch (SQLException e) {
            try {
                connectionPoolManager.returnConnectionToPool(db_connection);
                db_connection.rollback();
                return false;
            } catch (SQLException e1) {
                e1.printStackTrace();
            }

            e.printStackTrace();
        }
        try {
            db_connection.rollback();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
        return false;
    }

    public void checkIfThereAreMessagesForUser(String username){
        System.out.println("vou checkar se há mensagens offline para " + username);

        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        PreparedStatement ps = null;
        String note;
        int auction_id;
        int message_notification_id;
        String text;
        String messager;
        String host_port;
        ResultSet response;

        try {
            ps = db_connection.prepareStatement("SELECT m.auction_id auction_id, m.text text, m.username messager, mn.id message_notification_id FROM message_notification mn, message m WHERE mn.message_id = m.id AND mn.username = ?");
            ps.setString(1, username);

            response = ps.executeQuery();
            System.out.println("recebi a response");

            while(response.next()){
                System.out.println("entrei no next, logo há mensagens");
                auction_id = response.getInt("auction_id");
                text = response.getString("text");
                messager = response.getString("messager");
                message_notification_id = response.getInt("message_notification_id");
                note = "type: notification_message, id: " + auction_id + " user: " + messager + " text: " + text;
                host_port = checkIfUserOnline(username);
                if(!host_port.isEmpty()){
                    System.out.println("tenho host port");
                    TCPServer tcp = getTCPbyHostPort(host_port);
                    try {
                        System.out.println("tcp.sendnotification");
                        tcp.sendNotification(username, note);
                        removeMessagesNotifications(message_notification_id);
                    } catch (RemoteException e) {
                        System.out.println("Error sending notification to user after he logged in");
                        e.printStackTrace();
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Error querying for note");
            e.printStackTrace();
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
    }

    public void checkIfThereAreNotificationsForUser(String username){
        System.out.println("vou checkar se há notificações offline para " + username);

        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        PreparedStatement ps = null;
        String note;
        int auction_id;
        int bid_notification_id;
        double amount;
        String bidder;
        String host_port;
        ResultSet response;


        try {
            ps = db_connection.prepareStatement("SELECT b.auction_id auction_id, b.amount amount, b.username bidder, bn.id bid_notification_id FROM bid_notification bn, bid b WHERE bn.bid_id = b.id AND bn.username = ?");
            ps.setString(1, username);

            response = ps.executeQuery();
            System.out.println("recebi a response");

            while(response.next()){
                System.out.println("entrei no next, logo há notificações");
                auction_id = response.getInt("auction_id");
                amount = response.getDouble("amount");
                bidder = response.getString("bidder");
                bid_notification_id = response.getInt("bid_notification_id");
                note = "type: notification_bid, id: " + auction_id + " user: " + bidder + " amount: " + amount;
                host_port = checkIfUserOnline(username);
                if(!host_port.isEmpty()){
                    System.out.println("tenho host port");
                    TCPServer tcp = getTCPbyHostPort(host_port);
                    try {
                        System.out.println("tcp.sendnotification");
                        tcp.sendNotification(username, note);
                        removeBidsNotifications(bid_notification_id);
                    } catch (RemoteException e) {
                        System.out.println("Error sending notification to user after he logged in");
                        e.printStackTrace();
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Error querying for note");
            e.printStackTrace();
        }

        connectionPoolManager.returnConnectionToPool(db_connection);
    }


    private void removeMessagesNotifications(int message_notification_id){
        PreparedStatement ps = null;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            ps = db_connection.prepareStatement("DELETE FROM message_notification WHERE id = ?");
            ps.setInt(1, message_notification_id);
            ps.executeUpdate();
            db_connection.commit();

        } catch (SQLException e) {
            System.out.println("Error deleting message notification");
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
    }

    private void removeBidsNotifications(int bid_notification_id){
        PreparedStatement ps = null;
        Connection db_connection = connectionPoolManager.getConnectionFromPool();

        try {
            ps = db_connection.prepareStatement("DELETE FROM bid_notification WHERE id = ?");
            ps.setInt(1, bid_notification_id);
            ps.executeUpdate();
            db_connection.commit();

        } catch (SQLException e) {
            System.out.println("Error deleting bid notification");
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
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

        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        ResultSet response;
        PreparedStatement ps = null;
        int auction_id;
        String notification = "";
        String userToNotify = "";
        String host_port;
        int message_id;

        try {
            ps = db_connection.prepareStatement("SELECT b.username username, a.id auction_id FROM bid b, auction a WHERE (b.bid_date, b.auction_id) IN (SELECT MAX(bid_date), auction_id FROM bid GROUP BY auction_id) AND a.id = b.auction_id AND a.state = 'active' AND a.deadline < sysdate();");
            response = ps.executeQuery();

            ps = db_connection.prepareStatement("UPDATE auction SET state = 'ended' WHERE state = 'active' AND deadline < sysdate()");
            ps.executeUpdate();

            db_connection.commit();

            System.out.println("alterei estado de algumas???");

            while(response.next()){
                auction_id = response.getInt("auction_id");
                userToNotify = response.getString("username");
                notification = "type: notification_auction_won, text: You have won the auction with the following id: " + auction_id;
                host_port = checkIfUserOnline(userToNotify);
                if(!host_port.isEmpty()){
                    System.out.println("user que ganhou está on");
                    TCPServer tcp = getTCPbyHostPort(host_port);
                    if(tcp != null){
                        System.out.println("antes de note");
                        try {
                            tcp.sendNotification(userToNotify, notification);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("o mano esta off");
                    ps = db_connection.prepareStatement("INSERT INTO message (auction_id, username, message_date, text) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                    ps.setInt(1, auction_id);
                    ps.setString(2, userToNotify);
                    java.sql.Timestamp date = new java.sql.Timestamp(new java.util.Date().getTime());
                    ps.setTimestamp(3, date);
                    ps.setString(4, notification);

                    ps.executeUpdate();

                    ResultSet keys = ps.getGeneratedKeys();
                    keys.next();
                    message_id = keys.getInt(1);

                    db_connection.commit();
                    insertIntoMessageNotification(userToNotify, message_id);
                }
            }
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

    /**
     * Method to cancel an auction (Admin permission)
     */
    @Override
    public boolean cancel_auction(int id) throws RemoteException {

        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        PreparedStatement ps;
        int succeeded;

        try {
            ps = db_connection.prepareStatement("UPDATE auction SET state = 'canceled' WHERE state = 'active' and id = ?");
            ps.setInt(1, id);
            succeeded = ps.executeUpdate();
            db_connection.commit();

            if(succeeded == 1){
                db_connection.commit();
                connectionPoolManager.returnConnectionToPool(db_connection);
                return true;
            } else{
                db_connection.rollback();
                connectionPoolManager.returnConnectionToPool(db_connection);
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                db_connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            connectionPoolManager.returnConnectionToPool(db_connection);
            return false;
        }
    }

    /**
     * Method to ban a user (Admin permission)
     */
    @Override
    public boolean ban_user(String username) throws RemoteException {
        System.out.println("entrei no ban_user");

        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        PreparedStatement ps;
        int response;
        ResultSet auctions_to_remove;
        ResultSet user_oldest_bids;
        ResultSet max_date;
        int auction_id;
        double amount;
        Timestamp date_bid;
        Timestamp date_bid_max;
        ResultSet getResponse;
        Timestamp date_to_change;
        String owner = "";

        try {
            ps = db_connection.prepareStatement("UPDATE user SET state = 'banned' WHERE state = 'active' and username = ?");
            ps.setString(1, username);
            response = ps.executeUpdate();

            if(response == 1){
                db_connection.commit();
                System.out.println("response foi 1, bani o user " + username);

                // 1º CANCELAR AUCTIONS DE ONDE O USER A SER BANIDO É OWNER
                ps = db_connection.prepareStatement("SELECT id auction_id FROM auction WHERE username = ?");
                ps.setString(1, username);
                auctions_to_remove = ps.executeQuery();

                while(auctions_to_remove.next()){
                    System.out.println("vou cancelar auction do " + username);
                    cancel_auction(auctions_to_remove.getInt("auction_id"));
                }

                // REMOVER BIDS

                // encontrar a primeira dele
                // TODO: ESTE SELECT TÁ FODIDO
                ps = db_connection.prepareStatement("SELECT b.auction_id auction_id, b.amount amount, b.bid_date bid_date FROM bid b WHERE (b.bid_date, b.auction_id) IN (SELECT MIN(bid_date), b.auction_id FROM bid WHERE username = ? GROUP BY auction_id) AND username = ?");
                ps.setString(1, username);
                ps.setString(2, username);
                user_oldest_bids = ps.executeQuery();


                // por a ultima igual à primeira dele e remover todas entre a ultima e a primeira dele
                while(user_oldest_bids.next()){
                    System.out.println("dentro do while");
                    amount = user_oldest_bids.getDouble("amount");
                    System.out.println(amount);
                    auction_id = user_oldest_bids.getInt("auction_id");
                    System.out.println(auction_id);
                    date_bid = user_oldest_bids.getTimestamp("bid_date");
                    System.out.println(date_bid);
                    ps = db_connection.prepareStatement("SELECT bid_date bid_date_max, a.username owner FROM bid b, auction a WHERE b.bid_date = (SELECT MAX(bid_date) FROM bid WHERE auction_id = ? AND username <> ? GROUP BY auction_id) AND b.auction_id = ? and a.id = b.auction_id");
                    ps.setInt(1, auction_id);
                    ps.setString(2, username);
                    ps.setInt(3, auction_id);
                    System.out.println("vou executar a query");
                    max_date = ps.executeQuery();
                    if(max_date.next()){ // SE UILIZADOR A SER BANIDO NÃO FOI O UNICO A FAZER BID
                        date_bid_max = max_date.getTimestamp("bid_date_max");
                        owner = max_date.getString("owner");
                        System.out.println("depois do select");

                        ps = db_connection.prepareStatement("SELECT MAX(bid_date) date_to_change_amount FROM bid WHERE auction_id = ? AND username <> ? GROUP BY auction_id");
                        ps.setInt(1, auction_id);
                        ps.setString(2, username);
                        getResponse = ps.executeQuery();
                        getResponse.next();
                        date_to_change = getResponse.getTimestamp("date_to_change_amount");

                        ps = db_connection.prepareStatement("UPDATE bid SET amount = ? WHERE bid_date = ? AND auction_id = ?");
                        ps.setDouble(1, amount);
                        ps.setTimestamp(2, date_to_change);
                        ps.setInt(3, auction_id);
                        ps.executeUpdate();
                        db_connection.commit();
                        System.out.println("dei update no amount da pessoa que tinha tinha o máximo");

                        ps = db_connection.prepareStatement("DELETE FROM bid where (bid_date > ? and bid_date < ? and auction_id = ?) OR (username = ? AND auction_id = ?)");
                        System.out.println(date_bid.toString());
                        System.out.println(date_bid_max.toString());
                        ps.setString(1, date_bid.toString());
                        ps.setString(2, date_bid_max.toString());
                        ps.setInt(3, auction_id);
                        ps.setString(4, username);
                        ps.setInt(5, auction_id);
                        ps.executeUpdate();
                        db_connection.commit();
                        System.out.println("VOU ENVIAR MENSAGEM PARA O NURAL!!!!!!!!!!");


                        message(auction_id, owner, "Sorry for the inconvenience, but something changed in this auction.");
                        System.out.println("apaguei as bids do mano a ser banido aka " + username);

                    } else {
                        ps = db_connection.prepareStatement("DELETE FROM bid where username = ? AND auction_id = ?");
                        ps.setString(1, username);
                        ps.setInt(2, auction_id);
                        ps.executeUpdate();
                        db_connection.commit();

                        System.out.println("VOU ENVIAR MENSAGEM PARA O NURAL!!!!!!!!!!");
                        message(auction_id, owner, "Sorry for the inconvenience, but something changed in this auction.");
                        System.out.println("ele foi o unico a fazer bid, logo dou delete das cenas dele");
                    }
                }

                connectionPoolManager.returnConnectionToPool(db_connection);
                return true;
            }
        } catch (SQLException e) {
            try {
                db_connection.rollback();
                connectionPoolManager.returnConnectionToPool(db_connection);
                return false;
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Method that counts users auctions and returns it ordered
     */
    @Override
    public Map mostAuctionsUsers() throws RemoteException {
        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        PreparedStatement ps;
        ResultSet topAuctions;
        String owner;
        int numberAuctions;
        HashMap<String, Integer> usersAuctions = new HashMap<>();
        try {
            ps = db_connection.prepareStatement("SELECT COUNT(*) number_auctions, username owner from auction GROUP by username  ORDER BY number_auctions DESC LIMIT 10");
            topAuctions = ps.executeQuery();
            while(topAuctions.next()){
                owner = topAuctions.getString("owner");
                numberAuctions = topAuctions.getInt("number_auctions");
                usersAuctions.put(owner, numberAuctions);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);
        return usersAuctions;
    }

    /**
     * Method that counts users won auctions and returns it ordered
     */
    @Override
    public Map userWithMostAuctionsWon() throws RemoteException {

        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        PreparedStatement ps;
        ResultSet auctionWithMostWins;
        String username;
        HashMap<String, Integer> usersAuctions = new HashMap<>();

        try {
            ps = db_connection.prepareStatement("SELECT  a.username username from (SELECT b.username username from bid b, auction a  where (b.bid_date, b.auction_id) in (SELECT MAX(bid_date), auction_id from bid GROUP BY auction_id) AND a.id = b.auction_id AND a.state = 'ended')a ORDER BY a.username LIMIT 10;");
            auctionWithMostWins = ps.executeQuery();
            while (auctionWithMostWins.next()){
                username = auctionWithMostWins.getString("username");
                if (usersAuctions.containsKey(username)) {
                    usersAuctions.put(username, usersAuctions.get(username) + 1);
                } else {
                    usersAuctions.put(username, 1);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return usersAuctions;
    }

    /**
     * Method to list all the auctions created in the last 10 days
     */
    @Override
    public ArrayList<Auction> auctionsInTheLast10Days(){
        ArrayList<Auction> listAuctionsInTheLast10Days = new ArrayList<>();
        Connection db_connection = connectionPoolManager.getConnectionFromPool();
        PreparedStatement ps;
        ResultSet lastBids;
        String owner;
        int numberAuctions;
        String code;
        String title;
        String description;
        Timestamp deadline;
        double amount;
        Date date;

        try {
            ps = db_connection.prepareStatement("SELECT username owner, title title, description description, deadline deadline, amount amount, code code from auction WHERE DATEDIFF(sysdate(), created_date) < 10");
            lastBids = ps.executeQuery();
            while(lastBids.next()){
                owner = lastBids.getString("owner");
                title = lastBids.getString("title");
                description = lastBids.getString("description");
                code = lastBids.getString("code");
                deadline = lastBids.getTimestamp("deadline");
                date = new Date(deadline.getTime());
                amount = lastBids.getDouble("amount");
                listAuctionsInTheLast10Days.add(new Auction(owner, code, title, description, date, amount));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        connectionPoolManager.returnConnectionToPool(db_connection);

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
    String password = "coimbra";

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
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
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