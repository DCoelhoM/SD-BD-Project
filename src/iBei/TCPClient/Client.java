package iBei.TCPClient;
import java.io.*;
import java.net.*;
import java.util.*;

class TCPClient {
    public static void main(String[] args) {
        Socket socket;
        PrintWriter outToServer;
        BufferedReader inFromServer = null;

        try {
            // connect to the specified address:port (default is localhost:12345)
            if(args.length == 2)
                socket = new Socket(args[0], Integer.parseInt(args[1]));
            else
                socket = new Socket("localhost", 6000);//TODO Antes da defesa mudar para 12345!!!!!!!!!!!!!!!!!<<<<<<<<<<<<----------------------------------------------------------

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
            if(inFromServer == null)
                System.out.println("\nUsage: java TCPClient <host> <port>\n");
            System.out.println(e.getMessage());
        } finally {
            try { inFromServer.close(); } catch (Exception e) {}
        }
    }
}