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
        Socket socket;
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

            // create streams for writing to and reading from the socket
            inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToServer = new PrintWriter(socket.getOutputStream(), true);

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


            // the main thread loops reading from the server and writing to System.out
            String messageFromServer;
            while((messageFromServer = inFromServer.readLine()) != null)
                System.out.println(messageFromServer);

        }  catch (IOException e) {
            if(inFromServer == null)
                System.out.println("\nUsage: java TCPClient <host> <port>\n");
            System.out.println(e.getMessage());
        } finally {
            try { inFromServer.close(); } catch (Exception e) {}
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
        System.out.println(parsedInput);

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
                out.println("type : ban_user , status : ok");
            } else {
                out.println("type : ban_user , status : false");
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

    private static void test(){
        System.out.println("ola");
    }
}


