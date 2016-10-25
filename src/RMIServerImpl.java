import javax.xml.bind.SchemaOutputResolver;
import java.io.IOException;
import java.lang.reflect.Array;
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
    private List<Map.Entry<String,String>> online_users; //{Username, TCP_Host:Port}
    private List<Map.Entry<String,TCPServer>> connected_TCPs; //{TCP_Host:Port, TCPServer} TODO Se forem maquinas diferentes tem que receber o host tambem
    private List<Map.Entry<String,String>> notifications; //{Username, Message}


    public RMIServerImpl() throws java.rmi.RemoteException{
        super();
        auctions = Collections.synchronizedList(new ArrayList<>());
        users = Collections.synchronizedList(new ArrayList<>());
        online_users = Collections.synchronizedList(new ArrayList<>());
        connected_TCPs = Collections.synchronizedList(new ArrayList<>());
        notifications = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public void addTCPServer(TCPServer tcp, String host_port) throws RemoteException{
        connected_TCPs.add(new AbstractMap.SimpleEntry<>(host_port, tcp));
    }

    @Override
    public int checkNumberUsers(String host_port) throws RemoteException {
        int counter = 0;
        for(Map.Entry<String,String> u : online_users){
            if(u.getValue().equals(host_port)){
                counter ++;
            }
        }
        return counter;
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
            saveUsers();
            return true;
        } else {
            return false;
        }
    }

    private boolean checkCredentials(String username, String password){
        for (User u : users){
            if (u.getUsername().equals(username) && u.getState().equals("active")){
                return u.getPassword().equals(password);
            }
        }
        return false; //username not found
    }

    private void addOnlineUser(String username, String tcp_host_port) throws RemoteException {
        online_users.add(new AbstractMap.SimpleEntry<>(username, tcp_host_port));
        this.saveOnlineUsers();
    }


    public boolean userAlreadyLogged(String username){
        for (Map.Entry<String, String> u : online_users) {
            if (u.getKey().equals(username)) {
                return true;
            }
        }
        return false;
    }

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

    @Override
    public boolean logout(String username) throws RemoteException {
        return removeOnlineUser(username);
    }


    private boolean removeOnlineUser(String username) throws RemoteException {
        for (Map.Entry<String, String> u : online_users) {
            if (u.getKey().equals(username)) {
                online_users.remove(u);
                this.saveOnlineUsers();
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean create_auction(String owner, String code, String title, String description, Date deadline, double amount) throws RemoteException {
        //TODO DEADLINE > ACTUAL DATE
        //TODO EMPTY ARGS
        Auction new_auc = new Auction(owner, code, title, description, deadline, amount);
        auctions.add(new_auc);
        saveAuctions();
        return true;
    }

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
                    DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
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

    //TODO
    private String checkIfUserOnline(String username){
        for (Map.Entry<String,String> u:online_users){
            if(u.getKey().equals(username)){
                return u.getValue(); //RETORNA A PORTA DO TCP SERVER QUE ESTÁ CONECTADO
            }
        }
        return "";
    }
    private TCPServer getTCPbyHostPort(String tcp_host_port){
        for (Map.Entry<String,TCPServer> tcp:connected_TCPs){
            if(tcp.getKey().equals(tcp_host_port)){
                return tcp.getValue();
            }
        }
        return null;
    }
    @Override
    public void sendNotification(){
        System.out.println(notifications);
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
                            System.out.println("TCP PARTIDO");
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

    @Override
    public ArrayList<String> online_users() throws RemoteException {

        ArrayList<String> users_online = new ArrayList<>();
        for (Map.Entry<String,String> online_user:online_users){
            users_online.add(online_user.getKey());
        }
        return users_online;
    }

    public void end_auctions(){
        boolean flag = false;
        for (Auction a:auctions){
            if (a.getDeadline().before(new Date())){
                a.endAuction();
                flag = true;
            }
        }
        if (flag){
            saveAuctions();
        }
    }

    @Override
    public boolean cancel_auction(int id) throws RemoteException {
        System.out.println("na cancel_auction");
        for (Auction a:auctions){
            System.out.println("auction tem id" + a.getID());
            if (a.getID()==id){
                a.cancelAuction();
                saveAuctions();
                System.out.println("cancelei a auction");
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean ban_user(String username) throws RemoteException {
        for (User u:users){
            if (u.getUsername().equals(username)){
                u.ban();
                for (Auction a:auctions){
                    //Cancelar auctions do utilizador
                    if (a.getOwner().equals(username)){
                        a.cancelAuction();
                    } else if (a.checkUserBidActivity(username)){ //confirmar se fez licitações nos leilões
                        a.removeUserBids(username);
                        a.addMsg("NOTIFICATION","SORRY FOR THE PROBLEM");
                    }
                }
                return true;
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

    public void loadOnlineUsers(){
        ObjectFile file = new ObjectFile();
        try {
            file.openRead("online_users");

            try {
                this.online_users = (List<Map.Entry<String,String>>) file.readObject();
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
    static void testRMI(RMIServer RMI, int port){
        try {
            String answer = RMI.ping();
            try {
                System.out.println(answer);
                Thread.sleep(10000);
                testRMI(RMI, port);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } catch (RemoteException e) {
            rmiStart(port);
        }

    }

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
