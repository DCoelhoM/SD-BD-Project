import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Auction implements Serializable {
    private String state; // "active", "canceled", "ended"
    private int id;
    //private String uniqueID;
    private String owner; //Owner email
    private long code; //Product EAN/ISBN code
    private String title; //title
    private String description;
    private Date deadline;
    private int amount;
    private List<Map.Entry<String,Integer>> bids;
    private List<Map.Entry<String,String>> messages;

    public Auction(String mail, long code, String title, String description, Date deadline, int amount) {
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
        this.bids = Collections.synchronizedList(new ArrayList<Map.Entry<String, Integer>>());
        this.messages = Collections.synchronizedList(new ArrayList<Map.Entry<String, String>>());

        try {
            previous_id.openWriteOW(filename);
            previous_id.writeLine(String.valueOf(this.id));
            previous_id.closeWrite();
        } catch (java.io.IOException e) {
            System.out.println("Problem with save id file.");
        }
    }

    public boolean checkUserBidActivity(String username){
        for (Map.Entry<String,Integer> b : bids){
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

    public boolean addBid(String username, int value){
        if (!username.equals(this.owner)) {
            if (value>0 && value<amount) {
                if (bids.size() > 0) {
                    int last_bid = bids.get(bids.size() - 1).getValue();
                    if (value < last_bid) {
                        bids.add(new AbstractMap.SimpleEntry<>(username, value));
                        return true;
                    }
                } else {
                    bids.add(new AbstractMap.SimpleEntry<>(username, value));
                    return true;
                }
            }
        }
        return false;
    }

    public void removeUserBids(String username){
        int n_bids = bids.size();
        if(n_bids>0) {
            int amount = 0;
            int index_first_occur = 0;
            for (int i = 0; i < n_bids; i++) {
                if (bids.get(i).getKey().equals(username)) {
                    amount = bids.get(i).getValue();
                    index_first_occur = i;
                    break;
                }
            }

            if (!bids.get(n_bids - 1).getKey().equals(username)) {
                bids.get(n_bids - 1).setValue(amount);
            } else {
                bids.remove(n_bids - 1);
            }

            for (int i = n_bids - 2; i >= index_first_occur; i--) {
                bids.remove(i);
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

    public int getID(){
        return id;
    }

    public String getOwner(){
        return owner;
    }

    public long getCode(){
        return code;
    }

    public String getTitle() {
        return title;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public boolean checkBids(){
        return bids.size() > 0;
    }

    @Override
    public String toString() {
        int msg_count = messages.size();
        int bids_count = bids.size();

        String aux_details = "title: " + title + ", description: " + description + ", deadline: " + deadline.toString() + ", messages_count: " + String.valueOf(msg_count);

        int i=0;
        for (Map.Entry<String,String> m : messages){
            String user = ", messages_" + String.valueOf(i) + "_user: " + m.getKey();
            String msg = ", messages_" + String.valueOf(i) + "_text: " + m.getValue();
            aux_details += user + msg;
            i++;
        }

        aux_details+= ", bids_count: " + String.valueOf(bids_count);

        int j=0;
        for (Map.Entry<String,Integer> b : bids){
            String user = ", bids_" + String.valueOf(j) + "_user: " + b.getKey();
            String amount = ", bids_" + String.valueOf(j) + "_amount: " + String.valueOf(b.getValue());
            aux_details += user + amount;
            j++;
        }

        return aux_details;
    }

    public static void main(String args[]){
        ArrayList<Auction> auctions = new ArrayList<>();
        Auction teste = new Auction("DINIS", 123456,"LALALA","LEILAO TESTE",new Date(),1000);
        teste.addBid("jorge",100);
        teste.addBid("jorge",90);
        teste.addBid("pinho",80);
        teste.addBid("jorge",70);
        teste.removeUserBids("jorge");
        Auction teste1 = new Auction("DINIS", 1234567,"LALALA1","LEILAO TESTE",new Date(),10);
        Auction teste2 = new Auction("DINIS", 12345678,"LALALA2","LEILAO TESTE",new Date(),10);
        auctions.add(teste);
        auctions.add(teste1);
        auctions.add(teste2);
        System.out.println(auctions);
        ObjectFile file = new ObjectFile();
        try {
            file.openWrite("auctions");
        } catch (IOException e) {
            System.out.println("PROBLEMS");
        }
        try {
            file.writeObject(auctions);
        } catch (IOException e) {
            System.out.println("PROBLEMS2");
        }
        try {
            file.closeWrite();
        } catch (IOException e) {
            System.out.println("PROBLEMS3");
        }

        ArrayList<Auction> new_auctions = new ArrayList<>();
        try {
            file.openRead("auctions");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            new_auctions= (ArrayList<Auction>) file.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            file.closeRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(new_auctions);

    }
}

