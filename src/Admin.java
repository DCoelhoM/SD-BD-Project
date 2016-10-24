import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class Admin {

    RMIServer RMI = null;

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

            // create streams for writing to and reading from the socket
            inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            outToServer = new PrintWriter(socket.getOutputStream(), true);

            // create a thread for reading from the keyboard and writing to the server
            new Thread() {
                public void run() {
                    Scanner keyboardScanner = new Scanner(System.in);
                    while(!socket.isClosed()) {
                        String readKeyboard = keyboardScanner.nextLine();
                        outToServer.println(readKeyboard);
                    }
                }
            }.start();

            // the main thread loops reading from the server and writing to System.out
            String messageFromServer;
            while((messageFromServer = inFromServer.readLine()) != null)
                System.out.println(messageFromServer);

        } catch (IOException e) {
            e.printStackTrace();
        }


        // RMI CONNECTION
        try {
            Admin admin = new Admin(serverPort);
            admin.rmiConnection();
            int number=0;
            try {
                System.out.println("Listening on port " + serverPort);
                ServerSocket listenSocket = new ServerSocket(serverPort);
                System.out.println("LISTEN SOCKET="+listenSocket);

                while(true) {
                    Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                    System.out.println("CLIENT_SOCKET (created at accept())="+clientSocket);
                    number++;
                    new AdminConnection(clientSocket, number, admin);
                }
            } catch(IOException e) {
                System.out.println("Listen:" + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Exception in main: " + e);
        }
    }
}




// RMI CONNECTION
class AdminConnection extends Thread {
    PrintWriter out;
    BufferedReader in = null;
    private Socket clientSocket;
    private int thread_number;
    private String username;
    private int id;
    private Admin admin;

    public AdminConnection(Socket newClientSocket, int number, Admin admin) {
        this.thread_number = number;
        try {
            this.clientSocket = newClientSocket;
            this.in = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.out = new PrintWriter(this.clientSocket.getOutputStream(), true);
            this.admin = admin;
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }

    //=============================
    public void run() {
        try {
            while (true) {
                //an echo server
                String data = in.readLine();
                System.out.println("T[" + thread_number + "] Recebeu: " + data);
                parseAdminInput(data);
            }
        } catch (EOFException e) {
            System.out.println("EOF:" + e);

        } catch (IOException e) {
            System.out.println("IO:" + e);
        }
    }


    // type : test
    // type : ban_user , username : dinis
    // type : cancel_auction , id : 2
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

        chosenType(parsedInput);
    }

    private void chosenType(LinkedHashMap<String, String> parsedInput) {
        String type = parsedInput.get("type");

        switch(type){
            case "ban_user":
                ban_user(parsedInput);
                break;
            case "cancel_auction":
                cancel_auction(parsedInput);
                break;
            default:
                break;
        }
    }

    private void ban_user(LinkedHashMap<String, String> parsedInput){
        username = parsedInput.get("username");

        try {
            if(admin.RMI.ban_user(username)){
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
            if(admin.RMI.cancel_auction(id)){
                out.println("type : cancel_auction , status : ok");
            } else {
                out.println("type : cancel_auction , status : false");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }



}
