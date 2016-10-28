package iBei.Admin;
import iBei.RMIServer.RMIServer;
import iBei.Auxiliar.Auction;
import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.text.SimpleDateFormat;
import java.util.*;

public class Admin {
    int rmi_port;
    int tcp_port;
    RMIServer RMI = null;
    PrintWriter out = null;
    BufferedReader in = null;

    public Admin(int rmi_port, int tcp_port, Socket socket){
        super();
        this.rmi_port = rmi_port;
        this.tcp_port = tcp_port;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException e) {
            System.out.println("Problem creating IO from TCP.");
        }
    }

    public void rmiConnection(){
        try {
            this.RMI = (RMIServer) LocateRegistry.getRegistry(this.rmi_port).lookup("iBei");
        } catch (RemoteException | NotBoundException e1) {
            rmiConnection();
        }
    }

    public static void main(String args[]){
        int tcpserver_port = 6000;
        int rmiserver_port = 7000;
        try{
            if(args.length==2){
                tcpserver_port = Integer.parseInt(args[0]);
                rmiserver_port = Integer.parseInt(args[1]);
            }


            Socket socket = new Socket("localhost",tcpserver_port);
            Admin admin = new Admin(rmiserver_port,tcpserver_port, socket);

            admin.rmiConnection();

            // create a thread for reading from the keyboard and writing to the server
            new Thread() {
                public void run() {
                    Scanner keyboardScanner = new Scanner(System.in);
                    while(!socket.isClosed()) {
                        String readKeyboard = keyboardScanner.nextLine();
                        HashMap<String, String> data = admin.parseData(readKeyboard);
                        admin.chosenType(data);
                    }
                }
            }.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public HashMap<String, String> parseData(String line) {
        HashMap<String, String> g = new HashMap<>();
        Arrays.stream(line.split(",")).map(s -> s.split(":")).forEach( i -> g.put(i[0].trim(), i[1].trim()) );
        return g;
    }

    private void chosenType(HashMap<String, String> parsedInput) {
        String type = parsedInput.get("type");

        switch(type){
            case "ban_user":
                this.ban_user(parsedInput);
                break;
            case "cancel_auction":
                this.cancel_auction(parsedInput);
                break;
            case "test":
                test();
                break;
            case "statistics":
                statistics();
                break;
            default:
                break;
        }
    }

    private void ban_user(HashMap<String, String> parsedInput){
        String username = parsedInput.get("username");

        try {
            if(this.RMI.ban_user(username)){
                System.out.println("type : ban_user , ok : true");
            } else {
                System.out.println("type : ban_user , ok : false");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void cancel_auction(HashMap<String, String> parsedInput){
        int id = Integer.parseInt(parsedInput.get("id"));

        try {
            if(this.RMI.cancel_auction(id)){
                System.out.println("apaguei bem");
            } else {
                System.out.println("nao apaguei");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void statistics() {
        System.out.println("STATISTICS:");
        ArrayList<Auction> listAuctionsInLast10Days;
        Map<Integer, String> map;

        try {
            map = this.RMI.mostAuctionsUsers();
            System.out.println("Users with most auctions:");
            Set set2 = map.entrySet();
            Iterator iterator2 = set2.iterator();
            int i = 0;
            while (iterator2.hasNext() && i < 10) {
                Map.Entry me2 = (Map.Entry) iterator2.next();
                System.out.print(me2.getKey() + ": ");
                System.out.println(me2.getValue());
                i++;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        try {
            map = this.RMI.userWithMostAuctionsWon();
            System.out.println("Users with most won auctions:");
            Set set2 = map.entrySet();
            Iterator iterator2 = set2.iterator();
            int i = 0;
            while(iterator2.hasNext() && i < 10) {
                Map.Entry me2 = (Map.Entry)iterator2.next();
                System.out.print(me2.getKey() + ": ");
                System.out.println(me2.getValue());
                i++;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //SimpleDateFormat myFormat = new SimpleDateFormat("dd MM yyyy");
        try {
            listAuctionsInLast10Days = this.RMI.auctionsInTheLast10Days();
            System.out.println("Auctions in the last 10 days : ");
            for (Auction auction : listAuctionsInLast10Days){
                System.out.println(auction);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }

    public String randomString(int min_leng, int max_leng){
        Random r_length = new Random();
        int length = r_length.nextInt(max_leng - min_leng + 1) + min_leng;

        char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        StringBuilder sb = new StringBuilder();

        Random random = new Random();

        for (int i = 0; i < length; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }

        return sb.toString();
    }

    private void test(){
        System.out.println("Creating data for tests!");
        String random_name = randomString(6,10);
        String random_password = randomString(6,10);
        String random_code = randomString(8,13);
        String random_title = randomString(4,8);
        String random_desc = randomString(15,25);
        double random_amount =  5 + (100 - 5) * new Random().nextDouble();
        Date dt = new Date();
        long MINUTE = 60*1000;
        Date newdt = new Date(dt.getTime() + 2 * MINUTE);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH-mm");
        String deadline = dateFormat.format(newdt);

        String REGISTER = "type: register, username: " + random_name + ", password: " + random_password;
        String LOGIN = "type: login, username: " + random_name + ", password: " + random_password;
        String CREATE_AUCTION = "type: create_auction, code: " + random_code + ", title:" + random_title + ", description:" + random_desc + ", deadline: " + deadline + ", amount: " + random_amount;
        String MY_AUCTIONS = "type: my_auctions";
        String DETAIL_AUCTION = "type: detail_auction, id: ";
        String LOGOUT = "type: logout";

        HashMap<String, String> data = new HashMap<>();
        String response;

        // TEST REGISTER
        this.out.println(REGISTER);
        try {
            response = this.in.readLine();
            data = parseData(response);
            if(data.get("ok").equals("true")){
                System.out.println("Register Working Fine!");
            } else {
                System.out.println("Wrong Answer From register");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TEST LOGIN
        this.out.println(LOGIN);
        try {
            response = this.in.readLine();
            data = parseData(response);
            if(data.get("ok").equals("true")){
                System.out.println("Login Working Fine");
            } else {
                System.out.println("Wrong Answer From login");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TEST AUCTION CREATION
        this.out.println(CREATE_AUCTION);
        try {
            response = this.in.readLine();
            data = parseData(response);
            if(data.get("ok").equals("true")){
                System.out.println("Auction Creation Working Fine!");
            } else {
                System.out.println("Wrong Answer From auction_creation");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TEST MY_AUCTIONS
        int auc_id=0;
        this.out.println(MY_AUCTIONS);
        try {
            response = this.in.readLine();
            data = parseData(response);
            if (data.get("items_count").equals("1") && data.get("items_0_code").equals(random_code)){
                auc_id = Integer.parseInt(data.get("items_0_id"));
                System.out.println("My Auctions Working Fine!");
            } else {
                System.out.println("Wrong Answer From my_auctions");
            }
        } catch (IOException e){
            e.printStackTrace();
        }

        //TEST DETAIL_AUCTION
        this.out.println(DETAIL_AUCTION+String.valueOf(auc_id));
        try {
            response = this.in.readLine();
            data = parseData(response);
            if (data.get("code").equals(random_code) && data.get("title").equals(random_title)){
                System.out.println("Detail Auction Working Fine!");
            } else {
                System.out.println("Wrong Answer From detail_auction");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //TEST LOGOUT
        this.out.println(LOGOUT);
        try {
            response = this.in.readLine();
            data = parseData(response);
            if(data.get("ok").equals("true")){
                System.out.println("Logout Working Fine");
            } else {
                System.out.println("Wrong Answer From logout");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


