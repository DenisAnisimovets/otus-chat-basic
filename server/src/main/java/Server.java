import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private final AuthenticationProvider authenticationProvider;

    public ClientHandler findClientHandlerByUserName(String username) {
        for (ClientHandler clientHandler : clients
        ) {
            if(clientHandler.getUsername().equals(username)) return clientHandler;
        }
        return null;
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public Server(int port, AuthenticationProvider authenticationProvider) {
        this.port = port;
        this.authenticationProvider = authenticationProvider;
        clients = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
//            try {
//                Class.forName("org.postgresql.Driver");
//            } catch (ClassNotFoundException e) {
//                System.out.println("PostgreSQL JDBC Driver is not found. Include it in your library path ");
//                e.printStackTrace();
//                return;
//            }
            while (true) {
                System.out.println("Сервер запущен на порту " + port);
                Socket socket = serverSocket.accept();
                new ClientHandler(socket, this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastMessage("Клиент: " + clientHandler.getUsername() + " вошел в чат");
    }

    public synchronized void broadcastMessage(String message) {
        System.out.println(message + " это сообщение");
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public synchronized void sendMessageToUser(String message, String username) {
        System.out.println(message + " это сообщение");
        for (ClientHandler client : clients) {
            if(client.getUsername().equals(username)) {
                client.sendMessage(message);
            }
        }
    }

    public void unsubscribe(User user) {
        for (ClientHandler clintHandler : clients
        ) {
            if(clintHandler.getUsername().equals(user.getUsername())) {
                unsubscribe(clintHandler);
            }
        }
        ;
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Клиент: " + clientHandler.getUsername() + " вышел из чата");
    }
}
