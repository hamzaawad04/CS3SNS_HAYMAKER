
package server;

// Networking, security and client management
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class ChatServer {

    private static final int MAX_CLIENTS = 30;

    // Tracks connected users
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final Set<String> usernames = ConcurrentHashMap.newKeySet();
    private final Semaphore availablePlaces = new Semaphore(MAX_CLIENTS);

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "tls";
        int port = 5555;

        if (!mode.equals("plain") && !mode.equals("tls")) {
            System.err.println("Usage: java server.ChatServer [plain|tls] [port]");
            return;
        }

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
                if (port < 1 || port > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.err.println("Port must be between 1 and 65535.");
                return;
            }
        }

        if (mode.equals("tls") && System.getProperty("javax.net.ssl.keyStore") == null) {
            System.err.println("TLS mode requires a server keystore.");
            return;
        }

        try {
            new ChatServer().start(mode, port);
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // Accepts clients until the server is full
    private void start(String mode, int port) throws IOException {
        try (ServerSocket serverSocket = createSocket(mode, port)) {
            System.out.println("Chat server running in " + mode + " mode on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();

                if (!availablePlaces.tryAcquire()) {
                    socket.close();
                    continue;
                }

                Thread clientThread = new Thread(() -> {
                    try {
                        new ClientHandler(this, socket).run();
                    } finally {
                        availablePlaces.release();
                    }
                });
                clientThread.start();
            }
        }
    }

    // Uses a secure socket when TLS is selected
    private ServerSocket createSocket(String mode, int port) throws IOException {
        if (mode.equals("plain")) {
            return new ServerSocket(port);
        }

        SSLServerSocket socket = (SSLServerSocket)
                SSLServerSocketFactory.getDefault().createServerSocket(port);
        socket.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});
        return socket;
    }

    boolean addClient(ClientHandler client, String username) {
        String name = username.toLowerCase(Locale.ROOT);
        if (!usernames.add(name)) {
            return false;
        }

        clients.add(client);
        System.out.println(username + " joined");
        return true;
    }

    void removeClient(ClientHandler client, String username) {
        clients.remove(client);
        usernames.remove(username.toLowerCase(Locale.ROOT));
        System.out.println(username + " left");
        broadcast("SYSTEM: " + username + " left the chat.");
    }

    // Sends a message to everyone connected
    void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }
}
