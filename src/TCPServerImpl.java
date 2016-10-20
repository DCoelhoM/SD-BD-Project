import java.net.*;
import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

public class TCPServerImpl extends java.rmi.server.UnicastRemoteObject implements TCPServer{
    static RMIServer RMI = null;
    public TCPServerImpl()  throws java.rmi.RemoteException{
        super();
    }

    public static void main(String args[]){
        try {
            rmiConnection();
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
                    new Connection(clientSocket, number);
                }
            } catch(IOException e) {
                System.out.println("Listen:" + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Exception in main: " + e);
        }
    }
    static void rmiConnection(){
        try {
            TCPServerImpl.RMI = (RMIServer) LocateRegistry.getRegistry(7000).lookup("iBei");
        } catch (RemoteException | NotBoundException e1) {
            rmiConnection();
        }
    }
}

class Connection extends Thread {
    PrintWriter out;
    BufferedReader in = null;
    private Socket clientSocket;
    private int thread_number;
    private String username;

    public Connection (Socket newClientSocket, int number) {
        this.thread_number = number;
        try{
            this.clientSocket = newClientSocket;
            this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);

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
                detail_auction(parsedInput);
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
    //TODO: CHECK IF USER IS ALREADY LOGGED SO NO ONE CAN LOG WITHOUT HIM LOGGING FIRST
    private void login(LinkedHashMap<String, String> parsedInput) {
        String username, password;
        username = parsedInput.get("username");
        password = parsedInput.get("password");
        try {
            if (TCPServerImpl.RMI.login(username, password)) {
                out.println("type : login , ok : true");
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
            TCPServerImpl.rmiConnection();
            login(parsedInput);
        }
    }

    // type : register , username : pierre , password : omidyar
    private void register(LinkedHashMap<String, String> parsedInput){
        String username, password;
        username = parsedInput.get("username");
        password = parsedInput.get("password");

        try {
            if(TCPServerImpl.RMI.register(username, password)){
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
            TCPServerImpl.rmiConnection();
            register(parsedInput);
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
            if(TCPServerImpl.RMI.create_auction(this.username, code, title, description, deadline, amount)){
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
            TCPServerImpl.rmiConnection();
            create_auction(parsedInput);
        }
    }

    // type : search_auction , code : 9780451524935
    private void search_auction(LinkedHashMap<String, String> parsedInput){
        long code;
        code = Long.parseLong(parsedInput.get("code"));

        try {
            ArrayList<Auction> a_list = TCPServerImpl.RMI.search_auction(code);
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
            TCPServerImpl.rmiConnection();
            search_auction(parsedInput);
        }
    }

    // type : detail_auction , id : 101
    /*
    type : detail_auction , title : 1984, description : big brother i swa tching you , de a dli ne : 2017!01!01 00 :01 , messages_coun t :2, messages_0_user : pierre , messages_0_text : qual a editora?, messages_1_user : pierre , messages_1_text : entretanto vi que era a antigona , bids_count : 0
     */
    // TODO: mudar o toString disto.
    private void detail_auction(LinkedHashMap<String, String> parsedInput){
        int id = Integer.parseInt(parsedInput.get("id"));

        Auction auction;
        try {
            auction = TCPServerImpl.RMI.detail_auction(id);
            out.println(auction);
        } catch (RemoteException e) {
            try {
                System.out.println("Connection with problems...");
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            TCPServerImpl.rmiConnection();
            detail_auction(parsedInput);
        }
    }

}

