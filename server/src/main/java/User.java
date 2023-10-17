public class User {
    private String login;
    private String password;
    private String username;
    private Roles role;

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public Roles getRole() {return role;}

    public User(String login, String password, String username, String role) {
        this.login = login;
        this.password = password;
        this.username = username;
        this.role = Roles.valueOf(role);
    }
}
