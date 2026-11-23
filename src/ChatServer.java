
    // Imports for networking, security and managing clients
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

        // Stores connected clients and usernames
        private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
        private final Set<String> usernames = ConcurrentHashMap.newKeySet();

        // Limits the number of connected clients
        private final Semaphore places = new Semaphore(MAX_CLIENTS);
    }
