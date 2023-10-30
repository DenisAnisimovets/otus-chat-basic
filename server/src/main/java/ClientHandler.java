import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientHandler {
    private Socket socket;

    private Server server;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;

    private static int userCount = 0;

    private Connection connection;

    static final String DB_URL = "jdbc:postgresql://localhost:5432/postgres";
    static final String USER = "postgres";
    static final String PASS = "root";

    public String getUsername() {
        return username;
    }

    public ClientHandler(Socket socket, Server server) throws IOException {
        this.socket = socket;
        this.server = server;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        // server.subscribe(this);
        new Thread(() -> {
            try {
                authenticateUser(server);
                while (true) {
                    // /exit -> disconnect()
                    // /w user message -> user
                    String message = in.readUTF();
                    boolean doBroadcast = true;
                    if(message.startsWith("/")) {
                        if(message.equals("/exit")) {
                            break;
                        }
                    }
                    if(message.startsWith("/w")) {
                        doBroadcast = false;
                        try {
                            String[] parts = message.split(" ");
                            server.sendMessageToUser(parts[2], parts[1]);
                        } finally {

                        }
                    }
                    if(message.startsWith("/kick")) {
                        String[] parts = message.split(" ");
                        User currentUser = server.getAuthenticationProvider().findUserByUserName(username);
                        if(currentUser.getRole().equals(Roles.ADMIN)) {
                            ClientHandler clientHandler = server.findClientHandlerByUserName(parts[1]);
                            if(clientHandler != null) {
                                server.unsubscribe(clientHandler);
                            }
                        } else {
                            System.out.println("Пользователь не админ. Он не может удалять из чата");
                        }
                    }
                    if(doBroadcast) {
                        server.broadcastMessage(message);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    private void authenticateUser(Server server) throws IOException {
        boolean isAuthenticated = false;
        while (!isAuthenticated) {
            String message = in.readUTF();
//            /auth login password
//            /register login nick password
            String[] args = message.split(" ");
            System.out.println("команда " + args[0]);
            String command = args[0];
            switch (command) {
                case "/auth": {
                    String login = args[1];
                    String password = args[2];
                    try {
                        connection = getConnection();
                        PreparedStatement st = null;
                        System.out.println("Ищем в базе" + login);
                        st = connection.prepareStatement("SELECT login FROM logins where login = ?");
                        st.setString(1, login);
                        ResultSet resultSet = st.executeQuery();
                        if(resultSet.next()) {
                            username = resultSet.getString(1);
                            sendMessage(username + ", добро пожаловать в чат!");
                            server.subscribe(this);
                            isAuthenticated = true;
                        } else {
                            System.out.println("такого пользователя нет");
                        }
                    } catch (SQLException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
//                    String username = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
//                    if(username == null || username.isBlank()) {
//                        sendMessage("Указан неверный логин/пароль");
//                    } else {
//                        this.username = username;
//                        sendMessage(username + ", добро пожаловать в чат!");
//                        server.subscribe(this);
//                        isAuthenticated = true;
//                    }
                    break;
                }
                case "/register": {
                    System.out.println("Пытаемся зарегать пользака");
                    String login = args[1];
                    String nick = args[2];
                    String password = args[3];
                    String role = args[4];
                    //boolean isRegistred = server.getAuthenticationProvider().register(login, password, nick, role);
                    boolean isRegistred = false;
                    try {
                        connection = getConnection();
                        PreparedStatement st = null;
                        System.out.println("Ищем в базе" + login);
                        st = connection.prepareStatement("SELECT login FROM logins where login = ?");
                        st.setString(1, login);
                        ResultSet resultSet = st.executeQuery();
                        if(resultSet.next()) {
                            isRegistred = true;
                            System.out.println("такой пользователь есть");
                        } else {
                            System.out.println("такого пользователя нет");
                        }
                    } catch (SQLException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    if(isRegistred) {
                        sendMessage("Указанный логин/никнейм уже заняты");
                    } else {
                        try {
                            connection = getConnection();
                            PreparedStatement st = null;
                            st = connection.prepareStatement("INSERT INTO logins (login, role) VALUES (?, ?)");
                            st.setString(1, login);
                            st.setString(2, role);
                            int isDone = st.executeUpdate();
                            if(isDone > 0) {
                                this.username = nick;
                                sendMessage(nick + ", добро пожаловать в чат!");
                                server.subscribe(this);
                                isAuthenticated = true;
                                server.subscribe(this);
                            }
                        } catch (SQLException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
                default: {
                    sendMessage("Авторизуйтесь сперва");
                }
            }
        }

    }

    private Connection getConnection() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public void disconnect() {
        server.unsubscribe(this);
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
            disconnect();
        }
    }
}
