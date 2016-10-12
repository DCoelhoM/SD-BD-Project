public class User {
    private String name;
    private String password;

    public User(String n, String pw){
        this.name = n;
        this.password = pw;
    }

    public String getName(){
        return name;
    }

    public String getPassword(){
        return password;
    }

}
