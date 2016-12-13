package iBei.src.iBei.Auxiliar;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Class to help pass data between RMIs
 */
public class DataTransfer implements Serializable {
    private List<Auction> auctions;
    private List<User> users;
    private Map<String,String> online_users; //{Username, TCP_Host:Port}
    private List<Map.Entry<String,String>> notifications; //{Username, Message}
    private int last_auc_id;

    public DataTransfer(List<Auction> auctions, List<User> users, Map<String,String> online_users, List<Map.Entry<String,String>> notifications, int last_auc_id){
        super();
        this.auctions = auctions;
        this.users = users;
        this.online_users = online_users;
        this.notifications = notifications;
        this.last_auc_id = last_auc_id;
    }

    public List<Auction> getAuctions() {
        return auctions;
    }

    public List<User> getUsers() {
        return users;
    }

    public Map<String, String> getOnline_users() {
        return online_users;
    }

    public List<Map.Entry<String, String>> getNotifications() {
        return notifications;
    }

    public int getLast_auc_id() {
        return last_auc_id;
    }
}