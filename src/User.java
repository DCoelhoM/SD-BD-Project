public class User {
    private String email;
    private String name;
    private String password;

    public User(String mail, String n, String pw){
        this.email = mail;
        this.name = n;
        this.password = pw;
    }

    public String getEmail(){
        return email;
    }

    public String getName(){
        return name;
    }

    public String getPassword(){
        return password;
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
