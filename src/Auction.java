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
    private Map<String,Integer> bids;
    private Map<String,String> messages;

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
        this.bids = Collections.synchronizedMap(new LinkedHashMap<String, Integer>());
        this.messages = Collections.synchronizedMap(new LinkedHashMap<String, String>());

        try {
            previous_id.openWriteOW(filename);
            previous_id.writeLine(String.valueOf(this.id));
            previous_id.closeWrite();
        } catch (java.io.IOException e) {
            System.out.println("Problem with save id file.");
        }
    }

    public boolean addBid(String username, int value){
        int last_bid = (Integer) bids.values().toArray()[bids.values().size()-1];
        if (value<last_bid){
            bids.put(username,value);
            return true;
        }
        return false;
    }

    public void addMsg(String username, String msg){
        messages.put(username,msg);
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

    @Override
    public String toString() {
        int msg_count = messages.size();
        int bids_count = bids.size();

        String aux_details = "title: " + title + ", description: " + description + ", deadline: " + deadline.toString() + ", messages_count: " + String.valueOf(msg_count);

        int i=0;
        for (Map.Entry<String,String> m : messages.entrySet()){
            String user = ", messages_" + String.valueOf(i) + "_user: " + m.getKey();
            String msg = ", messages_" + String.valueOf(i) + "_text: " + m.getValue();
            aux_details += user + msg;
            i++;
        }

        aux_details+= ", bids_count: " + String.valueOf(bids_count);

        int j=0;
        for (Map.Entry<String,Integer> b : bids.entrySet()){
            String user = ", messages_" + String.valueOf(j) + "_user: " + b.getKey();
            String amount = ", messages_" + String.valueOf(j) + "_amount: " + String.valueOf(b.getValue());
            aux_details += user + amount;
            j++;
        }

        return aux_details;
    }

    public static void main(String args[]){
        Auction teste = new Auction("DINIS", 123456,"LALALA","LEILAO TESTE",new Date(),10);

        Auction teste1 = new Auction("DINIS", 123456,"LALALA","LEILAO TESTE2",new Date(),10);

        System.out.println(teste);
        System.out.println(teste1);
    }
}

