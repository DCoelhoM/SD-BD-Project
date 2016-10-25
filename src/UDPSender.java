import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

public class UDPSender{
    private TCPServerImpl tcp;

    UDPSender(TCPServerImpl tcp){
        this.tcp = tcp;
    }

    public void udpMessager() {

        new Thread() { // SEND UDP MESSAGES
            public void run() {
                    while (true) {
                        int counter = 0;
                        try {
                            counter = tcp.RMI.checkNumberUsers(tcp.host_port);
                            String msg = "Number of clients in " + tcp.host_port + " is: " + counter;
                            InetAddress group = InetAddress.getByName("224.1.2.3");
                            MulticastSocket s = new MulticastSocket(tcp.port);
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
                            tcp.rmiConnection(tcp.primary_rmi_host,tcp.backup_rmi_host,tcp.p_rmi_port,tcp.b_rmi_port);
                            udpMessager();
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        }.start();
    }
}
