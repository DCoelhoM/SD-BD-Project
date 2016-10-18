import java.net.*;
import java.io.*;
import java.rmi.registry.LocateRegistry;

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
                resposta=String.valueOf(RMI.register("Dinis","dinis","dinis"));
                out.writeUTF(resposta);
            }
        }catch(EOFException e){System.out.println("EOF:" + e);
        }catch(IOException e){System.out.println("IO:" + e);}
    }
}

