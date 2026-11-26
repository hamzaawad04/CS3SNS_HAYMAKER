package server;

import java.sql.*;

public class DatabaseManager {

    private static final String DB_NAME = "HM_Messenger";
    private static final String URL = "jdbc:mysql://localhost:3306/" + DB_NAME;
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void initializeDatabase() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS userdetails (
                user_id INT AUTO_INCREMENT PRIMARY KEY,
	            username VARCHAR(255) NOT NULL UNIQUE,
	            password VARCHAR(255) NOT NULL
            )
        """;

        try (Connection connect = getConnection();
            Statement statement = connect.createStatement()) {
            statement.execute(createUsersTable);
            System.out.println("Database initialised");
        } catch(SQLException e) {
            System.out.println("Initialisation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean registerUser(String username, String password) {
        String sql = "INSERT INTO userdetails (username, password) VALUES (?, ?)";

        try (Connection connect = getConnection();
            PreparedStatement statement = connect.prepareStatement(sql)) {

            String hashedPassword = PasswordHasher.hashPassword(password.toCharArray());
            statement.setString(1, username);
            statement.setString(2, hashedPassword);

            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean validateUser(String username, String password) {
        String sql = "SELECT password FROM userdetails WHERE username = ?";

        try (Connection connect = getConnection();
            PreparedStatement statement = connect.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet row = statement.executeQuery()) {
                if (row.next()) {
                    String storedHash = row.getString("password");
                    return PasswordHasher.verify(password.toCharArray(), storedHash);
                }
            }
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static void main(String[] args) {
        initializeDatabase();
        boolean created = registerUser("hamza", "password123");
        System.out.println("Test user created: " + created);
        System.out.println("Valid login test: " + validateUser("hamza", "password123"));
        System.out.println("Invalid login test: " + validateUser("hamza", "wrongpass"));
    }
}
