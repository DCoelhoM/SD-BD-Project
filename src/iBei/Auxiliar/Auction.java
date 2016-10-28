package iBei.Auxiliar;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Auction implements Serializable {
    private String state; // "active", "canceled", "ended"
    private int id;
    //private String uniqueID;
    private String owner; //Owner username
    private String code; //Product EAN/ISBN code
    private String title; //title
    private String description;
    private Date deadline;
    private double amount;
    private Date creation_date;
    private List<Map.Entry<String,Double>> bids;
    private List<Map.Entry<String,String>> messages;
    private List<String> previous_auction_data; // Arraylist that saves the edited auctions for each auction

    public Auction(String mail, String code, String title, String description, Date deadline, double amount) {
        this.state = "active";
        //this.uniqueID = UUID.randomUUID().toString();
        //ID
        String filename = "id.txt";
        TextFile previous_id = new TextFile();
        int prev_id=0;
        try {
            previous_id.openRead(filename);
            prev_id=Integer.valueOf(previous_id.readLine());
            previous_id.closeRead();
        } catch (IOException e) {
            try {
                //file doesnt exist -> create new file
                previous_id.openWriteOW(filename);
                previous_id.writeLine("0");
                previous_id.closeWrite();
            } catch (IOException e1) {
                System.out.println("Problem with read/create id file.");
            }
        }
        this.id = prev_id+1;
        this.owner = mail;
        this.code = code;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.amount = amount;
        this.creation_date = new Date();
        this.bids = Collections.synchronizedList(new ArrayList<Map.Entry<String, Double>>());
        this.messages = Collections.synchronizedList(new ArrayList<Map.Entry<String, String>>());
        this.previous_auction_data = Collections.synchronizedList(new ArrayList<String>());


        try {
            previous_id.openWriteOW(filename);
            previous_id.writeLine(String.valueOf(this.id));
            previous_id.closeWrite();
        } catch (java.io.IOException e) {
            System.out.println("Problem with save id file.");
        }
    }

    public boolean checkUserBidActivity(String username){
        for (Map.Entry<String,Double> b : bids){
            if (b.getKey().equals(username)){
                return true;
            }
        }
        return false;
    }

    public boolean checkUserMessageActivity(String username){
        for (Map.Entry<String,String> m : messages){
            if (m.getKey().equals(username)){
                return true;
            }
        }
        return false;
    }

    public String getUsernameLastBid(){
        if (this.getNumberBids() > 0){
            return bids.get(bids.size() - 1).getKey();
        }
        return "";
    }

    public int getNumberBids(){
        return bids.size();
    }

    public boolean addBid(String username, double value){
        //if (!username.equals(this.owner)) {
            if (value>0 && value<amount) {
                if (bids.size() > 0) {
                    double last_bid = bids.get(bids.size() - 1).getValue();
                    if (value < last_bid) {
                        bids.add(new AbstractMap.SimpleEntry<>(username, value));
                        return true;
                    }
                } else {
                    bids.add(new AbstractMap.SimpleEntry<>(username, value));
                    return true;
                }
            }
        //}
        return false;
    }

    public void removeUserBids(String username){
        int n_bids = bids.size();
        if(n_bids>0) {
            double amount = 0;
            int index_first_occur = 0;
            for (int i = 0; i < n_bids; i++) {
                if (bids.get(i).getKey().equals(username)) {
                    amount = bids.get(i).getValue();
                    index_first_occur = i;
                    break;
                }
            }

            boolean last_valid_user_to_bid=true;
            for (int i = n_bids - 1; i >= index_first_occur; i--) {
                if(!bids.get(i).getKey().equals(username) && last_valid_user_to_bid){
                    bids.get(i).setValue(amount);
                    last_valid_user_to_bid=false;
                }else{
                    bids.remove(i);
                }
            }
        }
    }

    public void addMsg(String username, String msg){
        messages.add(new AbstractMap.SimpleEntry<>(username, msg));
    }

    public void cancelAuction(){
        this.state = "canceled";
    }

    public void endAuction(){
        this.state = "ended";
    }

    public ArrayList<String> getParticipants(){
        ArrayList<String> users = new ArrayList<>();
        users.add(this.owner);
        for (Map.Entry<String,String> m:messages){
            if (!users.contains(m.getKey())){
                users.add(m.getKey());
            }
        }
        for (Map.Entry<String,Double> b:bids){
            if (!users.contains(b.getKey())){
                users.add(b.getKey());
            }
        }
        return users;
    }

    public int getID(){
        return id;
    }

    public String getOwner(){
        return owner;
    }

    public String getCode(){
        return code;
    }

    public String getTitle() {
        return title;
    }

    public Date getDeadline() {
        return deadline;
    }

    public String getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public Date getCreationDate() { return creation_date; }

    public void setCreationDate(Date creation_date) { this.creation_date = creation_date; }

    public void setCode(String code) {
        this.code = code;
    }

    public void setPrevious_auction_data(String previous_auction_data) { this.previous_auction_data.add(previous_auction_data); }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        int msg_count = messages.size();
        int bids_count = bids.size();

        String aux_details = "code: "+ code + ", title: " + title + ", description: " + description + ", deadline: " + deadline.toString() + ", messages_count: " + String.valueOf(msg_count);

        int i=0;
        for (Map.Entry<String,String> m : messages){
            String user = ", messages_" + String.valueOf(i) + "_user: " + m.getKey();
            String msg = ", messages_" + String.valueOf(i) + "_text: " + m.getValue();
            aux_details += user + msg;
            i++;
        }

        aux_details+= ", bids_count: " + String.valueOf(bids_count);

        int j=0;
        for (Map.Entry<String,Double> b : bids){
            String user = ", bids_" + String.valueOf(j) + "_user: " + b.getKey();
            String amount = ", bids_" + String.valueOf(j) + "_amount: " + String.valueOf(b.getValue());
            aux_details += user + amount;
            j++;
        }

        aux_details += ", state: " + state + ", creator: "+ owner + ", created_at: "+creation_date;

        return aux_details;
    }
}

