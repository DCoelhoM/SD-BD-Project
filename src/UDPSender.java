import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class UDPSender{
    private static int serverPort;

    UDPSender(int serverPort){
        UDPSender.serverPort = serverPort;
    }

    public void udpMessager() {

        new Thread() { // SEND UDP MESSAGES
            public void run() {
                try {
                    while (true) {
                        System.out.println("entrei na thread");
                        String msg = "Number of clients in port " + "PORT AQUI is " + "numero de clientes";
                        InetAddress group = InetAddress.getByName("224.1.2.3");
                        MulticastSocket s = new MulticastSocket(UDPSender.serverPort);
                        System.out.println("o port é " + serverPort);
                        DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, 5555);
                        s.send(hi);
                        currentThread().sleep(5000);
                    }

                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
}
