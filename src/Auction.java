import java.io.IOException;
import java.util.*;

public class Auction {
    private int state; // 0-> Canceled; 1 -> Active; 2 -> Ended
    private int id;
    //private String uniqueID;
    private String owner; //Owner email
    private int code; //Product EAN/ISBN code
    private String name;
    private String description;
    private Date deadline;
    private int amount;
    private Map<String,Integer> bids;
    private Map<String,String> messages;

    public Auction(String mail, int code, String name, String description, Date deadline, int amount) {
        this.state = 1;
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
        this.name = name;
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

    public void bid(String name, int value){
        for (int i=0;i<5;i++){
            bids.put("BIDDD"+String.valueOf(i),100*(i+1));
        }
        int first_bid = (Integer) bids.values().toArray()[0];
        int last_bid = (Integer) bids.values().toArray()[bids.values().size()-1];
        System.out.println("First bid: "+first_bid);
        System.out.println("Last bid: "+last_bid);
        System.out.println(bids);

    }

    public int getID(){
        return id;
    }

    public String getOwner(){
        return owner;
    }

    public int getCode(){
        return code;
    }

    @Override
    public String toString() {
        return "Auction{" +
                "state=" + state +
                ", id='" + id + '\'' +
                ", owner='" + owner + '\'' +
                ", code=" + code +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", deadline=" + deadline +
                ", amount=" + amount +
                ", bids=" + bids +
                ", messages=" + messages +
                '}';
    }

    public static void main(String args[]){
        Auction teste = new Auction("DINIS", 123456,"LALALA","LEILAO TESTE",new Date(),10);
        teste.bid("lalala",5);
        Auction teste1 = new Auction("DINIS", 123456,"LALALA","LEILAO TESTE2",new Date(),10);
        teste.bid("lala2",10);
        System.out.println(teste);
        System.out.println(teste1);
    }
}

