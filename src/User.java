import java.io.Serializable;

public class User implements Serializable{
    private String type; // "admin", "user"
    private String state; // "active", "banned"
    private String username;
    private String password;

    public User(String username, String password){
        this.type = "user";
        this.state = "active";
        this.username = username;
        this.password = password;
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    @Override
    public String toString() {
        return "User: {" +
                "name='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
