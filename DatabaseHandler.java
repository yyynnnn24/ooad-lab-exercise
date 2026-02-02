import java.sql.*;

public class DatabaseHandler {
    // Ensure this matches your file location
    private static final String URL = "jdbc:sqlite:seminar_system.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Connection Error: " + e.getMessage());
        }
        return conn;
    }

    public static void createNewTable() {
        // 1. USERS TABLE
        // Note: I am using 'user_id' as the column name here
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (\n"
                + " user_id text PRIMARY KEY,\n"
                + " username text NOT NULL UNIQUE,\n"
                + " password text NOT NULL,\n"
                + " role text NOT NULL\n"
                + ");";

        // 2. SUBMISSIONS TABLE
        String sqlStudents = "CREATE TABLE IF NOT EXISTS submissions (\n"
                + " submit_id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " title text,\n"
                + " abstract text,\n"
                + " supervisor text,\n"
                + " type text,\n"
                + " filepath text,\n"
                + " student_id text,\n"
                + " FOREIGN KEY (student_id) REFERENCES users(user_id)\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            if (conn != null) {
                stmt.execute(sqlUsers);
                stmt.execute(sqlStudents);
                System.out.println("Tables checked/created successfully.");
                
                insertDefaultUser(conn);
            }
        } catch (SQLException e) {
            System.out.println("Table Creation Error: " + e.getMessage());
        }
    }

    private static void insertDefaultUser(Connection conn) {
        // Check if admin exists
        String checkSql = "SELECT count(*) FROM users WHERE username = 'admin'";
        
        // Insert Admin (Staff)
        String insertAdmin = "INSERT INTO users(user_id, username, password, role) VALUES('a001', 'admin', '123', 'Staff')";
        
        // Insert Student (Student)
        String insertStudent = "INSERT INTO users(user_id, username, password, role) VALUES('s001', 'student1', '123', 'Student')";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            rs.next();
            if (rs.getInt(1) == 0) { 
                stmt.executeUpdate(insertAdmin);
                stmt.executeUpdate(insertStudent);
                System.out.println("Default users inserted.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}