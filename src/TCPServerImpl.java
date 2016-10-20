import java.net.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

public class TCPServerImpl extends java.rmi.server.UnicastRemoteObject implements TCPServer{

    public TCPServerImpl()  throws java.rmi.RemoteException{
        super();
    }

    public static void main(String args[]){
        try {
            RMIServer RMI = (RMIServer) LocateRegistry.getRegistry(7000).lookup("iBei");
            int number=0;
            try {
                int serverPort = 6000;
                System.out.println("Listening on port 6000!");
                ServerSocket listenSocket = new ServerSocket(serverPort);
                System.out.println("LISTEN SOCKET="+listenSocket);
                while(true) {
                    Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                    System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                    number++;
                    new Connection(RMI, clientSocket, number);
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
    private DataInputStream in;
    private DataOutputStream out;
    private Socket clientSocket;
    private int thread_number;
    private RMIServer RMI;
    private String username;

    public Connection (RMIServer RMI, Socket newClientSocket, int number) {
        this.RMI = RMI;
        this.thread_number = number;
        try{
            this.clientSocket = newClientSocket;
            this.in = new DataInputStream(this.clientSocket.getInputStream());
            this.out = new DataOutputStream(this.clientSocket.getOutputStream());
            this.start();
        }catch(IOException e){System.out.println("Connection:" + e.getMessage());}
    }
    //=============================
    public void run(){
        String resposta;
        try{
            while(true){
                //an echo server
                String data = in.readUTF();
                System.out.println("T["+thread_number + "] Recebeu: "+data);
                parseUserInput(data);
                //resposta=String.valueOf(RMI.register("Dinis","dinis","dinis"));
                //out.writeUTF(resposta);
            }
        }catch(EOFException e){System.out.println("EOF:" + e);
        }catch(IOException e){System.out.println("IO:" + e);}
    }

    private void parseUserInput(String data){

        String[] aux;
        LinkedHashMap<String, String> parsedInput = new LinkedHashMap<String, String>();

        aux = data.split(",");

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

        switch(type){
            case "login":
                login(parsedInput);
                break;
            case "status":
                break;
            case "item_list":
                System.out.println("123");
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
                System.out.println("123");
                break;
            case "my_auctions":
                System.out.println("123");
                break;
            case "bid":
                System.out.println("123");
                break;
            case "edit_auction":
                System.out.println("123");
                break;
            case "message":
                System.out.println("123");
                break;
            case "online_users":
                System.out.println("123");
                break;
        }
    }

    //type : login , username : pierre , password : omidyar
    //TODO: CHECK IF USER IS ALREADY LOGGED
    private void login(LinkedHashMap<String, String> parsedInput){
        String username, password;
        username = parsedInput.get("username");
        password = parsedInput.get("password");
        try {
            if(RMI.login(username, password)){
                this.username = username;
                out.writeUTF("type : login , ok : true");
            } else {
                out.writeUTF("type : login , ok : false");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // type : register , username : pierre , password : omidyar
    private void register(LinkedHashMap<String, String> parsedInput){
        String username, password;
        username = parsedInput.get("username");
        password = parsedInput.get("password");

        try {
            if(RMI.register(username, password)){
                out.writeUTF("type : register , ok : true");
            } else {
                out.writeUTF("type : register , ok : false");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //type : create_auction , code : 9780451524935, title : 1984 , description : big brother is watching you , deadline : 2017-01-01 00:01 , amount : 10
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
            if(RMI.create_auction(this.username, code, title, description, deadline, amount)){
                out.writeUTF("type : create_auction , ok : true");
            } else {
                out.writeUTF("type : create_auction , ok : false");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // type : search_auction , code:9780451524935
    private void search_auction(LinkedHashMap<String, String> parsedInput){
        long code;
        code = Long.parseLong(parsedInput.get("code"));

        try {
            System.out.println(RMI.search_auction(code));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}

