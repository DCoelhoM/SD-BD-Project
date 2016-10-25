import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class Admin {

    RMIServer RMI = null;
    PrintWriter out;
    BufferedReader in = null;
    private String username;
    private int id;
    static Socket socket;


    int port;
    public Admin(int port)  throws java.rmi.RemoteException{
        super();
        this.port = port;
    }

    void rmiConnection(){
        try {
            this.RMI = (RMIServer) LocateRegistry.getRegistry(7000).lookup("iBei");
        } catch (RemoteException | NotBoundException e1) {
            rmiConnection();
        }
    }

    public static void main(String args[]){
        int serverPort = 6000;
        PrintWriter outToServer;
        BufferedReader inFromServer = null;

        try{
            if(args.length==3){
                serverPort = Integer.parseInt(args[0]);
                socket = new Socket(args[1], Integer.parseInt(args[2]));
            } else {
                socket = new Socket("localhost", 6000);//TODO Antes da defesa mudar para 12345!!!!!!!!!!!!!!!!!<<<<<<<<<<<<----------------------------------------------------------
            }


            Admin admin = new Admin(serverPort);
            admin.rmiConnection();


            // create a thread for reading from the keyboard and writing to the server
            new Thread() {
                public void run() {
                    Scanner keyboardScanner = new Scanner(System.in);
                    while(!socket.isClosed()) {
                        String readKeyboard = keyboardScanner.nextLine();
                        admin.parseAdminInput(readKeyboard);
                    }
                }
            }.start();

        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void parseAdminInput(String data){
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
        this.chosenType(parsedInput);
    }

    private void chosenType(LinkedHashMap<String, String> parsedInput) {
        String type = parsedInput.get("type");

        switch(type){
            case "ban_user":
                this.ban_user(parsedInput);
                break;
            case "cancel_auction":
                this.cancel_auction(parsedInput);
                break;
            case "test":
                test();
                break;
            default:
                break;
        }
    }

    private void ban_user(LinkedHashMap<String, String> parsedInput){
        username = parsedInput.get("username");

        try {
            if(this.RMI.ban_user(username)){
                System.out.println("type : ban_user , status : ok");
            } else {
                System.out.println("type : ban_user , status : false");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void cancel_auction(LinkedHashMap<String, String> parsedInput){
        id = Integer.parseInt(parsedInput.get("id"));

        try {
            if(this.RMI.cancel_auction(id)){
                System.out.println("apaguei bem");
            } else {
                System.out.println("nao apaguei");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //  Este teste deverá, no mínimo, um leilão, licitar nesse leilão, consultar os detalhes desse leilão e verificar se o resultado está correcto

    private static void test(){

        PrintWriter outToServer = null;
        BufferedReader inFromServer = null;

        // create streams for writing to and reading from the socket
        try {
            inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToServer = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // TEST REGISTER
        String REGISTER = "type : register , username : test_register, password : test_register";
        outToServer.println(REGISTER);

        String SUCCESSFUL_REGISTER = "type : register , ok : true";
        try {
            if(inFromServer.readLine().contentEquals(SUCCESSFUL_REGISTER)){
                System.out.println("Register Working Fine!");
            } else {
                System.out.println("Wrong Answer From register");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TEST LOGIN
        String LOGIN = "type : login , username : test_register , password : test_register";
        outToServer.println(LOGIN);

        String SUCCESSFUL_LOGIN = "type : login , ok : true";
        try {
            if(inFromServer.readLine().contentEquals(SUCCESSFUL_LOGIN)){
                System.out.println("Login Working Fine");
            } else {
                System.out.println("Wrong Answer From login");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TEST AUCTION CREATION
        String CREATE_AUCTION = "type : create_auction , code : 9780451524934, title : 1984 , description : big brother is watching you , deadline : 2017-10-24 15-16 , amount : 10";
        outToServer.println(CREATE_AUCTION);

        String SUCCESSFUL_CREATE_AUCTION = "type : create_auction , ok : true";
        try {
            if(inFromServer.readLine().contentEquals(SUCCESSFUL_CREATE_AUCTION)){
                System.out.println("Auction Creation Working Fine!");
            } else {
                System.out.println("Wrong Answer From auction_creation");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String DETAIL_AUCTION = "type : detail_auction , id : 1";
        outToServer.println(DETAIL_AUCTION);

        String DETAIL_AUCTION_EXPECTED_ANSWER = "code: 9780451524934, title: 1984, description: big brother is watching you, deadline: Tue Oct 24 15:16:00 WEST 2017, messages_count: 0, bids_count: 0";
        try {
            if(inFromServer.readLine().contentEquals(DETAIL_AUCTION_EXPECTED_ANSWER)){
                System.out.println("Detail Auction Working Fine!");
            } else {
                System.out.println("Wrong Answer From detail_auction");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

