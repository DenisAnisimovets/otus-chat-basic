import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;

    private Server server;
    private DataInputStream in;
    private DataOutputStream out;

    private String username;

    private static int userCount = 0;

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
            String command = args[0];
            switch (command) {
                case "/auth": {
                    String login = args[1];
                    String password = args[2];
                    String username = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                    if (username == null || username.isBlank()) {
                        sendMessage("Указан неверный логин/пароль");
                    } else {
                        this.username = username;
                        sendMessage(username + ", добро пожаловать в чат!");
                        server.subscribe(this);
                        isAuthenticated = true;
                    }
                    break;
                }
                case "/register": {
                    String login = args[1];
                    String nick = args[2];
                    String password = args[3];
                    String role = args[4];
                    boolean isRegistred = server.getAuthenticationProvider().register(login, password, nick, role);
                    if (!isRegistred) {
                        sendMessage("Указанный логин/никнейм уже заняты");
                    } else {
                        this.username = nick;
                        sendMessage(nick + ", добро пожаловать в чат!");
                        server.subscribe(this);
                        isAuthenticated = true;
                        server.subscribe(this);
                    }
                    break;
                }
                default: {
                    sendMessage("Авторизуйтесь сперва");
                }
            }
        }
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
