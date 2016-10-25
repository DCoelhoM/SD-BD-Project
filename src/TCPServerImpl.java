import java.net.*;
import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TCPServerImpl extends java.rmi.server.UnicastRemoteObject implements TCPServer{
    RMIServer RMI = null;
    Notification notes = null; //array list com os users online para mandar notificaçao
    String host;
    int port;
    String host_port;
    String primary_rmi_host, backup_rmi_host;
    int p_rmi_port, b_rmi_port;
    Map<String,Integer> TCPs_load;


    public TCPServerImpl(String host, int port, String p_host, String b_host, int p_port, int b_port)  throws java.rmi.RemoteException{
        super();
        this.host = host;
        this.port = port;
        this.host_port = this.host+":"+String.valueOf(this.port);
        this.primary_rmi_host = p_host;
        this.p_rmi_port = p_port;
        this.backup_rmi_host = b_host;
        this.b_rmi_port = b_port;
        this.TCPs_load = Collections.synchronizedMap(new HashMap<String, Integer>());
    }

    @Override
    public void sendNotification(String username, String msg) throws RemoteException {
        notes.sendNotification(username,msg);
    }

    public void sendTCPloadNotification(){
        String msg = "type: notification_load , server_list : " + TCPs_load.size();
        int i=0;
        for (Map.Entry<String,Integer> load:TCPs_load.entrySet()){
            String host_port[] = load.getKey().split(":");
            msg+= ", server_" + i + "_hostname: " + host_port[0] + ", server_" + i + "_port: " + host_port[1] + ", server_" + i + "_load: " + load.getValue();
            i++;
        }
        notes.sendNotificationToAll(msg);
    }

    void rmiConnection(String p_host, String b_host, int p_port, int b_port){ //tenta o primario, se não funcionar, vai tentar o backup
        try {
            this.RMI = (RMIServer) LocateRegistry.getRegistry(p_host, p_port).lookup("iBei");
            this.RMI.addTCPServer((TCPServer)this,this.host_port);
        } catch (RemoteException | NotBoundException e1) {
            rmiConnection(b_host,p_host,b_port,p_port);
        }
    }

    public static void main(String args[]){
        if(args.length==6) {
            String tcp_host, primary_rmi_host, backup_rmi_host;
            int tcp_port, p_rmi_port, b_rmi_port;
            tcp_host = args[0];
            tcp_port = Integer.parseInt(args[1]);
            primary_rmi_host = args[2];
            p_rmi_port = Integer.parseInt(args[3]);
            backup_rmi_host = args[4];
            b_rmi_port = Integer.parseInt(args[5]);


            System.setProperty("java.net.preferIPv4Stack", "true");

            UDPSender udp;
            try {
                TCPServerImpl tcp = new TCPServerImpl(tcp_host, tcp_port, primary_rmi_host, backup_rmi_host, p_rmi_port, b_rmi_port);
                tcp.rmiConnection(primary_rmi_host,backup_rmi_host,p_rmi_port,b_rmi_port);
                int number = 0;
                try {
                    System.out.println("Listening on port " + tcp_port);
                    ServerSocket listenSocket = new ServerSocket(tcp_port);
                    System.out.println("LISTEN SOCKET=" + listenSocket);
                    tcp.notes = new Notification();
                    udp = new UDPSender(tcp);
                    udp.udpMessager();

                    // MULTICAST - RECEBER MENSAGENS UDP
                    new Thread() {
                        public void run() {
                            MulticastSocket socket = null;
                            try {
                                socket = new MulticastSocket(5555); // 5555 é a PORTA
                                socket.joinGroup(InetAddress.getByName("224.1.2.3")); // 224.1.2.3 É O ENDEREÇO DO GRUPO
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            byte[] buf = new byte[1000];
                            DatagramPacket message = new DatagramPacket(buf, buf.length);
                            try {
                                while (true) {
                                    socket.receive(message);
                                    String parsedMessage = new String(message.getData(), 0, message.getLength());
                                    String load_info[] = parsedMessage.split("->");
                                    tcp.TCPs_load.put(load_info[0].trim(), Integer.parseInt(load_info[1].trim()));
                                    System.out.println(parsedMessage);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    //Notificação de load de 60 em 60 segs
                    new Thread() {
                        public void run() {
                            while(true) {
                                tcp.sendTCPloadNotification();
                                try {
                                    Thread.sleep(60000);
                                } catch (InterruptedException e) {
                                    System.out.println("Error in sleep of notification thread!");
                                }
                            }
                        }
                    }.start();


                    while (true) {
                        Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                        System.out.println("CLIENT_SOCKET (created at accept())=" + clientSocket);
                        number++;
                        new Connection(clientSocket, number, tcp);
                    }
                } catch (IOException e) {
                    System.out.println("Listen:" + e.getMessage());
                }
            } catch (Exception e) {
                System.out.println("Exception in main: " + e);
            }
        } else {
            System.out.println("Usage: tcp_host tcp_port primary_rmi_host primary_rmi_ port backup_rmi_host backup_rmi_port");
        }
    }
}

class Connection extends Thread {
    PrintWriter out;
    BufferedReader in = null;
    private Socket clientSocket;
    private int thread_number;
    private String username = null;
    private TCPServerImpl tcp;

    public Connection (Socket newClientSocket, int number, TCPServerImpl tcp) {
        this.thread_number = number;
        try{
            this.clientSocket = newClientSocket;
            this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
            this.tcp = tcp;
            this.start();
        }catch(IOException e){System.out.println("Connection:" + e.getMessage());}
    }
    //=============================
    public void run() {
        String resposta;
        try {
            while (true) {
                //an echo server
                String data = in.readLine();
                System.out.println("T[" + thread_number + "] Recebeu: " + data);
                parseUserInput(data);
            }
        } catch (EOFException e) {
            System.out.println("EOF:" + e);

        } catch (IOException e) {
            System.out.println("IO:" + e);
        } catch (NullPointerException e) {
            System.out.println("piças");
            System.out.println(this.username);
            logout();
        }
    }

    private void parseUserInput(String data){

        String[] aux;
        LinkedHashMap<String, String> parsedInput = new LinkedHashMap<String, String>();

        aux = data.split(",");

        // TODO: ter a certeza que sub string 1 e 2 vem separadas por espaço : espaço, senão parte. verificar no script de python
        for (String field : aux) {
            String[] split = field.split(":");
            String firstSubString = split[0].trim();
            String secondSubString = split[1].trim();
            parsedInput.put(firstSubString, secondSubString);
        }
        System.out.println(parsedInput);

        chosenType(parsedInput);
    }

    private void chosenType(LinkedHashMap<String, String> parsedInput){
        String type = parsedInput.get("type");

        /* TODO: type: logout
           TODO: testar em 2 maquinas
           TODO: usar packages
           TODO: funcionalidades primeiro, tratamento de erros depois
         */
        switch(type){
            case "login":
                login(parsedInput);
                break;
            case "logout":
                logout();
                break;
            case "register":
                register(parsedInput);
                break;
            case "create_auction":
                create_auction(parsedInput);
                break;
            case "search_auction":
                search_auction(parsedInput);
                break;
            case "detail_auction":
                detail_auction(parsedInput);
                break;
            case "my_auctions":
                my_auctions();
                break;
            case "bid":
                bid(parsedInput);
                break;
            case "edit_auction":
                edit_auction(parsedInput);
                break;
            case "message":
                message(parsedInput);
                break;
            case "online_users":
                online_users();
                break;
            default:
                break;
        }
    }

    // type : login , username : pierre , password : omidyar
    private void login(LinkedHashMap<String, String> parsedInput) {
        String username, password;
        username = parsedInput.get("username");
        password = parsedInput.get("password");
        System.out.println(tcp.port);
        try {
            if (tcp.RMI.login(username, password,tcp.host_port)) {
                System.out.println(tcp.port);
                out.println("type : login , ok : true");
                tcp.notes.addConnectedUser(username,out);
                tcp.RMI.sendNotification();
                this.username = username;
            } else {
                out.println("type : login , ok : false");
            }
        } catch (IOException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            login(parsedInput);
        }
    }

    private void logout(){
        try {
            if(tcp.RMI.logout(this.username)) {
                tcp.notes.removeUser(username);
                out.println("type : logout, ok : true");
            } else {
                out.println("type : logout, ok : false");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // type : register , username : pierre , password : omidyar
    private void register(LinkedHashMap<String, String> parsedInput){
        String username, password;
        username = parsedInput.get("username");
        password = parsedInput.get("password");

        try {
            if(tcp.RMI.register(username, password)){
                out.println("type : register , ok : true");
            } else {
                out.println("type : register , ok : false");
            }
        } catch (IOException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            register(parsedInput);
        }
    }

    // type : create_auction , code : 9780451524934, title : 1984 , description : big brother is watching you , deadline : 2017-10-24 15-16 , amount : 10
    //String owner, int code, String title, String description, Date deadline, int amount
    private void create_auction(LinkedHashMap<String, String> parsedInput){
        String code;
        double amount;
        String title, description, date;
        Date deadline = null;

        SimpleDateFormat formattedDate = new SimpleDateFormat ("yyyy-MM-dd HH-mm");

        code = parsedInput.get("code");
        amount = Double.parseDouble(parsedInput.get("amount"));
        title = parsedInput.get("title");
        description = parsedInput.get("description");
        date = parsedInput.get("deadline");

        try {
            deadline = formattedDate.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            if(tcp.RMI.create_auction(this.username, code, title, description, deadline, amount)){
                out.println("type : create_auction , ok : true");
            } else {
                out.println("type : create_auction , ok : false");
            }
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            create_auction(parsedInput);
        }
    }

    // type : search_auction , code : 9780451524935
    private void search_auction(LinkedHashMap<String, String> parsedInput){
        String code;
        code = parsedInput.get("code");

        try {
            ArrayList<Auction> a_list = tcp.RMI.search_auction(code);
            String auctions_found = "";
            int count=0;
            for (Auction a:a_list){
                String id_aux = ", items_" + String.valueOf(count) + "_id: " + String.valueOf(a.getID());
                String code_aux = ", items_" + String.valueOf(count) + "_code: " + String.valueOf(a.getCode());
                String title_aux = ", items_" + String.valueOf(count) + "_title: " + String.valueOf(a.getTitle());
                auctions_found += id_aux + code_aux + title_aux;
                count++;
            }
            out.println("type: search_auction , items_count: "+String.valueOf(count)+auctions_found);
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            search_auction(parsedInput);
        }
    }

    // type : detail_auction , id : 101

    private void detail_auction(LinkedHashMap<String, String> parsedInput){
        int id = Integer.parseInt(parsedInput.get("id"));

        Auction auction;
        try {
            auction = tcp.RMI.detail_auction(id);
            out.println(auction);
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            detail_auction(parsedInput);
        }
    }

    // type : my_auctions
    private void my_auctions(){
        ArrayList<Auction> user_auctions;

        try {
            user_auctions = tcp.RMI.my_auctions(this.username);
            String auctions_found = "";
            int count=0;
            for (Auction a:user_auctions){
                String id_aux = ", items_" + String.valueOf(count) + "_id: " + String.valueOf(a.getID());
                String code_aux = ", items_" + String.valueOf(count) + "_code: " + String.valueOf(a.getCode());
                String title_aux = ", items_" + String.valueOf(count) + "_title: " + String.valueOf(a.getTitle());
                auctions_found += id_aux + code_aux + title_aux;
                count++;
            }
            out.println("type: my_auctions , items_count: "+String.valueOf(count)+auctions_found);
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            my_auctions();
        }
    }

    // type : bid , id : 101, amount : 9
    // TODO : NÃO DEIXAR BIDS ABAIXO DE 0
    private void bid(LinkedHashMap<String, String> parsedInput){
        int id = Integer.parseInt(parsedInput.get("id"));
        double amount = Double.parseDouble(parsedInput.get("amount"));

        try {
            if(tcp.RMI.bid(id,this.username, amount)){
                out.println("type : bid , ok : true");
                tcp.RMI.sendNotification();
            } else {
                out.println("type : bid , ok : false");
            }
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            bid(parsedInput);
        }
    }

    // type : edit_auction , id : 101 , amount : 25
    private void edit_auction(LinkedHashMap<String, String> parsedInput){
        int id = Integer.parseInt(parsedInput.get("id"));

        try {
            if(tcp.RMI.edit_auction(this.username, id, parsedInput)){
                out.println("type : edit_auction , ok : true");
            } else {
                out.println("type : edit_auction , ok : false");
            }
        }catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            edit_auction(parsedInput);
        }
    }

    //public boolean message(int auction_id, String username, String msg) throws RemoteException {
    private void message(LinkedHashMap<String, String> parsedInput){

        String message = parsedInput.get("text");
        int auction_id = Integer.parseInt(parsedInput.get("id"));

        try {
            if(tcp.RMI.message(auction_id, this.username, message)){
                out.println("type : message , ok : true");
                tcp.RMI.sendNotification();
            } else {
                out.println("type : message , ok : false");
            }
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            message(parsedInput);
        }
    }

    private void online_users(){

        ArrayList<String> users_online;

        try {
            users_online = tcp.RMI.online_users();
            int users_count = users_online.size();
            int i = 0;

            String response = "type : online_users , users_count : " + users_count;

            for(String user : users_online){
                response += " , users_" + i + "_username : " + user;
                i++;
            }
            out.println(response);
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection(tcp.primary_rmi_host, tcp.backup_rmi_host, tcp.p_rmi_port, tcp.b_rmi_port);
            online_users();
        }

    }
}

class Notification{
    private Map<String,PrintWriter> connected_users; //{Username, Out}

    public Notification() {
        this.connected_users = Collections.synchronizedMap(new LinkedHashMap<String, PrintWriter>());
    }

    public void sendNotification(String username, String msg) {
        connected_users.get(username).println(msg);
    }

    public void sendNotificationToAll(String msg){
        for (Map.Entry<String,PrintWriter> u: connected_users.entrySet()){
            u.getValue().println(msg);
        }
    }

    public void addConnectedUser(String username, PrintWriter out){
        connected_users.put(username,out);
    }

    public void removeUser(String username){
        connected_users.remove(username);
    }
}
