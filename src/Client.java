import java.io.*;
import java.net.*;
import java.util.*;

public class Client {
    public static void main(String[] args) {
        Socket s = null;
        try {
            // 1o passo
            s = new Socket("localhost", 6000);

            System.out.println("SOCKET=" + s);
            // 2o passo
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            String texto = "";
            InputStreamReader input = new InputStreamReader(System.in);
            BufferedReader reader = new BufferedReader(input);
            System.out.println("Introduza texto:");

            // 3o passo
            while (true) {
                // READ STRING FROM KEYBOARD
                try {
                    texto = reader.readLine();
                } catch (Exception e) {
                }

                // WRITE INTO THE SOCKET
                out.writeUTF(texto);

                // READ FROM SOCKET
                String data = in.readUTF();

                // DISPLAY WHAT WAS READ
                System.out.println("Received: " + data);
            }

        } catch (UnknownHostException e) {
            System.out.println("Sock:" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        } finally {
            if (s != null)
                try {
                    s.close();
                } catch (IOException e) {
                    System.out.println("close:" + e.getMessage());
                }
        }
    }

}
