package iBei.RMIServer;
import iBei.TCPServer.TCPServer;
import iBei.Auxiliar.*;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RMIServerImpl extends java.rmi.server.UnicastRemoteObject  implements RMIServer{
    private List<Auction> auctions;
    private List<User> users;
    private Map<String,String> online_users; //{Username, TCP_Host:Port}
    private Map<String,TCPServer> connected_TCPs; //{TCP_Host:Port, TCPServer}
    private List<Map.Entry<String,String>> notifications; //{Username, Message}


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
        for (User u:users){
            if (u.getUsername().equals(username)){
                return false;
            }
        }
        return true;
    }

    /**
     * Method to register new user
     */
    @Override
    public boolean register(String username, String password) throws RemoteException {
        if (checkUsernameAvailability(username)){
            User new_user = new User(username, password);
            users.add(new_user);
            saveUsers();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method to check if username and password are valid
     */
    private boolean checkCredentials(String username, String password){
        for (User u : users){
            if (u.getUsername().equals(username) && u.getState().equals("active")){
                return u.getPassword().equals(password);
            }
        }
        return false; //username not found
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
     * Method to check if user isn't banned
     */
    public boolean checkIfUserNotBanned(String username){
        for(User u : users){
            if(u.getUsername().equals(username)){
                if(u.getState().equals("active")){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method to login a user
     */
    @Override
    public boolean login(String username, String password, String tcp_host_port) throws RemoteException {
        if(checkIfUserNotBanned(username)){
            if(!userAlreadyLogged(username)){
                if(checkCredentials(username, password)){
                    addOnlineUser(username,tcp_host_port);
                    return true;
                }
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
    @Override
    synchronized public boolean create_auction(String owner, String code, String title, String description, Date deadline, double amount) throws RemoteException {
        Auction new_auc = new Auction(owner, code, title, description, deadline, amount);
        auctions.add(new_auc);
        saveAuctions();
        return true;
    }

    /**
     * Method to search for various auctions with a specific code
     */
    @Override
    public ArrayList<Auction> search_auction(String code) throws RemoteException {
        ArrayList<Auction> auctions_found = new ArrayList<>();
        for (Auction a:auctions){
            if (a.getCode().equals(code)){
                auctions_found.add(a);
            }
        }
        return auctions_found;
    }

    /**
     * Method to searh for an auction by id
     */
    @Override
    public Auction detail_auction(int id) throws RemoteException {
        for (Auction a:auctions){
            if (a.getID()==id){
                return a;
            }
        }
        return null;
    }

    /**
     * Method to list all auctions from a specific user
     */
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

    /**
     * Method to bid on a specific auction
     */
    @Override
    public boolean bid(int id, String username, double amount) throws RemoteException {
        for (Auction a:auctions){
            if(a.getID()==id && a.getState().equals("active")){
                String usertonotify = a.getUsernameLastBid();
                int nbids = a.getNumberBids();

                if(a.addBid(username,amount)) {
                    if (!usertonotify.equals(a.getOwner())){
                        String note = "type: notification_bid, id: " + id + ", user: " + username + ", amount: " + amount;
                        notifications.add(new AbstractMap.SimpleEntry<>(a.getOwner(), note));
                    }
                    if (nbids>0) {
                        String note = "type: notification_bid, id: " + id + ", user: " + username + ", amount: " + amount;
                        notifications.add(new AbstractMap.SimpleEntry<>(usertonotify, note));
                    }
                    saveAuctions();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method to edit various or a specific attribute of an auction
     */
    @Override
    public boolean edit_auction(String username, int id, HashMap<String,String> data) throws RemoteException {
        for (Auction a : auctions){
            if (a.getID()==id && a.getOwner().equals(username)){
                a.setPrevious_auction_data("title: " + a.getTitle() + ", code: " + a.getCode() + ", description: " + a.getDescription() + ", deadline: " + a.getDeadline() + ", amount: " + a.getAmount());
                if (data.containsKey("code")){
                    a.setCode(data.get("code"));
                }
                if (data.containsKey("title")){
                    a.setTitle(data.get("title"));
                }
                if (data.containsKey("description")){
                    a.setDescription(data.get("description"));
                }
                if (data.containsKey("deadline")){
                    DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH-mm");
                    try {
                        a.setDeadline(df.parse(data.get("deadline")));
                    } catch (ParseException e) {
                        System.out.println("Problems with parsing deadline.");
                    }
                }
                if (data.containsKey("amount")){
                   a.setAmount(Double.parseDouble(data.get("amount")));
                }
                saveAuctions();
                return true;
            }
        }
        return false;
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
     * Method to send a notification if the user is online
     */
    @Override
    public void sendNotification(){
        List<Map.Entry<String,String>> notes_to_delete = Collections.synchronizedList(new ArrayList<>());
        synchronized (notifications) {
            for (Map.Entry<String, String> n : notifications) {
                String host_port = checkIfUserOnline(n.getKey());
                if (!host_port.isEmpty()) {
                    TCPServer tcp = getTCPbyHostPort(host_port);
                    if (tcp != null) {
                        try {
                            tcp.sendNotification(n.getKey(), n.getValue());
                            notes_to_delete.add(n);
                        } catch (RemoteException e) {
                            removeTCPandUsers(host_port);
                        }
                    }
                }
            }
        }
        for (Map.Entry<String,String> n:notes_to_delete){
            notifications.remove(n);
        }
        this.saveNotifications();
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
                saveAuctions();
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
                sendNotification();
                flag = true;
            }
        }
        if (flag){
            saveAuctions();
        }
    }

    /**
     * Method to cancel an auction (Admin permission)
     */
    @Override
    public boolean cancel_auction(int id) throws RemoteException {
        for (Auction a:auctions){
            System.out.println("auction tem id" + a.getID());
            if (a.getID()==id && a.getState().equals("active")){
                a.cancelAuction();
                saveAuctions();
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
     * Method to save auctions to a file
     */
    public void saveAuctions(){
        ObjectFile file = new ObjectFile();
        try {
            file.openWrite("auctions");
            try {
                file.writeObject(this.auctions);
            } catch (IOException e) {
                System.out.println("Problem saving auctions");
            }
            try {
                file.closeWrite();
            } catch (IOException e) {
                System.out.println("Problem close auctions file(WRITE MODE).");
            }
        } catch (IOException e) {
            System.out.println("Problem opening auctions file(WRITE MODE).");
        }

    }
    /**
     * Method to save users to a file
     */
    public void saveUsers(){
        ObjectFile file = new ObjectFile();
        try {
            file.openWrite("users");
            try {
                file.writeObject(this.users);
            } catch (IOException e) {
                System.out.println("Problem saving users");
            }
            try {
                file.closeWrite();
            } catch (IOException e) {
                System.out.println("Problem closing users file(WRITE MODE).");
            }
        } catch (IOException e) {
            System.out.println("Problem opening users file(WRITE MODE)");
        }

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
     * Method to save notifications to a file
     */
    public void saveNotifications(){
        ObjectFile file = new ObjectFile();
        try {
            file.openWrite("notifications");
            try {
                file.writeObject(this.notifications);
            } catch (IOException e) {
                System.out.println("Problem saving notifications");
            }
            try {
                file.closeWrite();
            } catch (IOException e) {
                System.out.println("Problem closing notifications file(WRITE MODE).");
            }
        } catch (IOException e) {
            System.out.println("Problem opening notifications file(WRITE MODE)");
        }
    }

    /**
     * Method to load auctions from a file
     */
    public void loadAuctions(){
        ObjectFile file = new ObjectFile();
        try {
            file.openRead("auctions");
            try {
                this.auctions = (List<Auction>) file.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Problem loading auctions");
            }

            try {
                file.closeRead();
            } catch (IOException e) {
                System.out.println("Problem closing auctions file(READ MODE)");
            }
        } catch (IOException e) {
            System.out.println("Problem opening auctions file(READ MODE)(No auctions found)");
        }
    }

    /**
     * Method to load users from a file
     */
    public void loadUsers(){
        ObjectFile file = new ObjectFile();
        try {
            file.openRead("users");

            try {
                this.users = (List<User>) file.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Problem loading users");
            }

            try {
                file.closeRead();
            } catch (IOException e) {
                System.out.println("Problem closing users file(READ MODE)");
            }
        } catch (IOException e) {
            System.out.println("Problem opening users file(READ MODE)(No users found)");
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
     * Method to load notifications from a file
     */
    public void loadNotifications(){
        ObjectFile file = new ObjectFile();
        try {
            file.openRead("notifications");

            try {
                this.notifications = (List<Map.Entry<String,String>>) file.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Problem loading notifications");
            }

            try {
                file.closeRead();
            } catch (IOException e) {
                System.out.println("Problem closing notifications file(READ MODE)");
            }
        } catch (IOException e) {
            System.out.println("Problem opening notifications file(READ MODE)(No notifications found)");
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
        saveAuctions();
        saveNotifications();
        saveUsers();
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
            rmiServer.loadAuctions();
            rmiServer.loadUsers();
            rmiServer.loadOnlineUsers();
            rmiServer.loadNotifications();

            // Thread para acabar com leilões à hora certa
            new Thread() {
                public void run() {
                    while(true) {
                        System.out.println("Chamei o end auctions");
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
                Thread.sleep(10000);
                testRMI(RMI, port);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (RemoteException e) {
            rmiStart(port);
        }

    }

    /**
     * Method to init RMIServer
     */
    public static void main(String args[]){
        String host_aux_rmi;
        int port_aux_rmi, port;
        if (args.length==3){
            host_aux_rmi = args[0];
            port_aux_rmi = Integer.parseInt(args[1]);
            port = Integer.parseInt(args[2]);
            try {
                RMIServer RMI = (RMIServer) LocateRegistry.getRegistry(host_aux_rmi,port_aux_rmi).lookup("iBei");
                testRMI(RMI, port);
            } catch (RemoteException | NotBoundException e) {
                rmiStart(port);
            }
        } else {
            System.out.println("Usage: host_second_rmi port_second_rmi port");
        }


    }
}