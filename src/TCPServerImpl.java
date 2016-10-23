import sun.awt.image.ImageWatched;

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
    Notification notes = null;
    int port;
    public TCPServerImpl(int port)  throws java.rmi.RemoteException{
        super();
        this.port = port;
    }

    @Override
    public void sendNotification(String username, String msg) throws RemoteException {
        notes.sendNotification(username,msg);
    }

    void rmiConnection(){
        try {
            this.RMI = (RMIServer) LocateRegistry.getRegistry(7000).lookup("iBei");
        } catch (RemoteException | NotBoundException e1) {
            rmiConnection();
        }
    }

    public static void main(String args[]){
        System.setProperty("java.net.preferIPv4Stack", "true");
        int serverPort = 6000;

        UDPSender udp;

        if(args.length==1){
            serverPort = Integer.parseInt(args[0]);
        }
        try {
            TCPServerImpl tcp = new TCPServerImpl(serverPort);
            tcp.rmiConnection();
            tcp.RMI.addTCPServer((TCPServer)tcp,tcp.port);
            int number=0;
            try {
                System.out.println("Listening on port " + serverPort);
                ServerSocket listenSocket = new ServerSocket(serverPort);
                System.out.println("LISTEN SOCKET="+listenSocket);
                tcp.notes = new Notification();
                udp = new UDPSender(serverPort, tcp);
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
                                System.out.println("á escuta de mensagem");
                                socket.receive(message);
                                System.out.println("Mensagem recebida");
                                String parsedMessage = new String(message.getData(), 0, message.getLength());
                                System.out.println(parsedMessage);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();


                while(true) {
                    Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                    System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                    number++;
                    new Connection(clientSocket, number, tcp);
                }
            } catch(IOException e) {
                System.out.println("Listen:" + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Exception in main: " + e);
        }
    }
}

class Connection extends Thread {
    PrintWriter out;
    BufferedReader in = null;
    private Socket clientSocket;
    private int thread_number;
    private String username;
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
    public void run(){
        String resposta;
        try{
            while(true){
                //an echo server
                String data = in.readLine();
                System.out.println("T["+thread_number + "] Recebeu: "+data);
                parseUserInput(data);
                //resposta=String.valueOf(RMI.register("Dinis","dinis","dinis"));
                //out.println(resposta);
            }
        }catch(EOFException e){System.out.println("EOF:" + e);
        }catch(IOException e){System.out.println("IO:" + e);}
    }

    private void parseUserInput(String data){

        String[] aux;
        LinkedHashMap<String, String> parsedInput = new LinkedHashMap<String, String>();

        aux = data.split(",");

        // TODO: ter a certeza que sub string 1 e 2 vem separadas por espaço : espaço, senão parte. verificar no script de python
        for (String field : aux) {
            String[] split = field.split(" : ");
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
                logout(parsedInput);
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
                System.out.println("123");
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
            if (tcp.RMI.login(username, password,tcp.port)) {
                System.out.println(tcp.port);
                out.println("type : login , ok : true");
                tcp.notes.addConnectedUser(username,out);
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
            tcp.rmiConnection();
            login(parsedInput);
        }
    }

    private void logout(LinkedHashMap<String, String> parsedInput){
        try {
            if(tcp.RMI.logout(this.username)) {
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
            tcp.rmiConnection();
            register(parsedInput);
        }
    }

    // type : create_auction , code : 9780451524935, title : 1984 , description : big brother is watching you , deadline : 2017-01-01 00:01 , amount : 10
    //String owner, int code, String title, String description, Date deadline, int amount
    private void create_auction(LinkedHashMap<String, String> parsedInput){
        long code;
        int amount;
        String title, description, date;
        Date deadline = null;

        SimpleDateFormat formattedDate = new SimpleDateFormat ("yyyy-MM-dd HH:mm");

        code = Long.parseLong(parsedInput.get("code"));
        amount = Integer.parseInt(parsedInput.get("amount"));
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
            tcp.rmiConnection();
            create_auction(parsedInput);
        }
    }

    // type : search_auction , code : 9780451524935
    private void search_auction(LinkedHashMap<String, String> parsedInput){
        long code;
        code = Long.parseLong(parsedInput.get("code"));

        try {
            ArrayList<Auction> a_list = tcp.RMI.search_auction(code);
            String auctions_found = "";
            int count=0;
            for (Auction a:a_list){
                if (a.getCode()==code){
                    String id_aux = ", items_" + String.valueOf(count) + "_id: " + String.valueOf(a.getID());
                    String code_aux = ", items_" + String.valueOf(count) + "_code: " + String.valueOf(a.getCode());
                    String title_aux = ", items_" + String.valueOf(count) + "_title: " + String.valueOf(a.getTitle());
                    auctions_found += id_aux + code_aux + title_aux;
                    count++;
                }
            }
            out.println("type: search_auction , items_count: "+String.valueOf(count)+auctions_found);
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection();
            search_auction(parsedInput);
        }
    }

    // type : detail_auction , id : 101

    private void detail_auction(LinkedHashMap<String, String> parsedInput){
        int id = Integer.parseInt(parsedInput.get("id"));

        Auction auction;
        try {
            auction = tcp.RMI.detail_auction(id);
            System.out.println("123");
            out.println(auction);
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection();
            detail_auction(parsedInput);
        }
    }

    // type : my_auctions
    private void my_auctions(){
        ArrayList<Auction> user_auctions;

        try {
            user_auctions = tcp.RMI.my_auctions(this.username);
            out.println(user_auctions);
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcp.rmiConnection();
            my_auctions();
        }
    }

    // type : bid , id : 101, amount : 9
    private void bid(LinkedHashMap<String, String> parsedInput){
        int id = Integer.parseInt(parsedInput.get("id"));
        int amount = Integer.parseInt(parsedInput.get("amount"));

        try {
            if(tcp.RMI.bid(id,this.username, amount)){
                out.println("type : bid , ok : true");
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
            tcp.rmiConnection();
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
            tcp.rmiConnection();
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
            tcp.rmiConnection();
            edit_auction(parsedInput);
        }

    }
}

class Notification{
    private Map<String,PrintWriter> connected_users; //{Username, Out}

    public Notification() {
        this.connected_users = Collections.synchronizedMap(new LinkedHashMap<String, PrintWriter>());;
    }

    public void sendNotification(String username, String msg) {
        connected_users.get(username).println(msg);
    }

    public void addConnectedUser(String username, PrintWriter out){
        connected_users.put(username,out);
    }

    public void removeUser(String username){
        connected_users.remove(username);
    }
}
