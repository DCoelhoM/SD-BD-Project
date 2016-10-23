import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class UDPSender{
    private static int serverPort;
    private TCPServerImpl tcp;

    UDPSender(int serverPort, TCPServerImpl tcp){
        UDPSender.serverPort = serverPort;
        this.tcp = tcp;
    }

    public void udpMessager() {

        new Thread() { // SEND UDP MESSAGES
            public void run() {
                    while (true) {
                        int counter = 0;
                        try {
                            counter = tcp.RMI.checkNumberUsers(UDPSender.serverPort);
                            String msg = "Number of clients in port " + UDPSender.serverPort + "is: " + counter;
                            InetAddress group = InetAddress.getByName("224.1.2.3");
                            MulticastSocket s = new MulticastSocket(UDPSender.serverPort);
                            System.out.println("o port é " + UDPSender.serverPort);
                            DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, 5555);
                            s.send(hi);
                            sleep(5000);
                        } catch (RemoteException e) {
                            try {
                                System.out.println("Connection with problems...");
                                Thread.sleep(5000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                            tcp.rmiConnection();
                            udpMessager();
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        }.start();
    }
}
