package iBei.TCPServer;

public interface TCPServer extends java.rmi.Remote {
    void sendNotification(String username, String msg) throws java.rmi.RemoteException;
}
