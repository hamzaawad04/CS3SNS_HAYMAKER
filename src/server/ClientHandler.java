
package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private final ChatServer server;
    private final Socket socket;
    private volatile PrintWriter output;

    public ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    // Handles one users connection
    @Override
    public void run() {
        String username = null;

        try (Socket connection = socket;
             BufferedReader input = new BufferedReader(
                     new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(connection.getOutputStream(), true,
                     StandardCharsets.UTF_8)) {

            output = writer;
            connection.setSoTimeout(30000);
            writer.println("Enter username:");
            String name = input.readLine();

            if (name == null || !name.matches("[A-Za-z0-9_]{1,20}")) {
                writer.println("ERROR: Invalid username.");
                return;
            }

            if (!server.addClient(this, name)) {
                writer.println("ERROR: Username already taken.");
                return;
            }

            username = name;
            connection.setSoTimeout(0);
            writer.println("SYSTEM: Connected as " + username);
            server.broadcast("SYSTEM: " + username + " joined the chat.");

            long windowStart = System.currentTimeMillis();
            int messageCount = 0;
            String message;

            // Reads messages until the user disconnects
            while ((message = input.readLine()) != null) {
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }
                if (message.isBlank()) {
                    continue;
                }
                if (message.length() > 500) {
                    writer.println("ERROR: Message too long.");
                    continue;
                }

                // Stops one user from flooding the chat
                long now = System.currentTimeMillis();
                if (now - windowStart >= 5000) {
                    windowStart = now;
                    messageCount = 0;
                }
                messageCount++;
                if (messageCount > 10) {
                    writer.println("ERROR: Too many messages. Slow down.");
                    continue;
                }

                server.broadcast(username + ": " + message);
            }

        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
        } finally {
            if (username != null) {
                server.removeClient(this, username);
            }
        }
    }

    public synchronized void send(String message) {
        if (output != null) {
            output.println(message);
        }
    }
}
