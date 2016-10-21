import java.io.Serializable;

public class User implements Serializable{
    private String state; // "active", "banned"
    private String username;
    private String password;

    public User(String username, String password){
        this.state = "active";
        this.username = username;
        this.password = password;
    }

    public void ban(){
        this.state = "banned";
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public String getState() {
        return state;
    }

    @Override
    public String toString() {
        return "User: {" +
                "name='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
