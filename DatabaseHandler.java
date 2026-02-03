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
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (\n"
                + " user_id text PRIMARY KEY,\n"
                + " username text NOT NULL UNIQUE,\n"
                + " password text NOT NULL,\n"
                + " role text NOT NULL\n"
                + ");";

        // 2. SUBMISSIONS TABLE (Existing)
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

        // --- NEW TABLES FOR MEMBER 2 (COORDINATOR) ---
        
        // 3. SESSIONS TABLE (Stores Date, Venue, Type)
        String sqlSessions = "CREATE TABLE IF NOT EXISTS sessions (\n"
                + " session_id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " date text,\n"
                + " time text,\n"
                + " venue text,\n"
                + " session_type text\n" // Oral or Poster
                + ");";

        // 4. ASSIGNMENTS TABLE (Links Session, Student, and Evaluator)
        String sqlAssignments = "CREATE TABLE IF NOT EXISTS assignments (\n"
                + " assign_id integer PRIMARY KEY AUTOINCREMENT,\n"
                + " session_id integer,\n"
                + " student_id text,\n"
                + " evaluator_id text,\n"
                + " FOREIGN KEY (session_id) REFERENCES sessions(session_id),\n"
                + " FOREIGN KEY (student_id) REFERENCES users(user_id),\n"
                + " FOREIGN KEY (evaluator_id) REFERENCES users(user_id)\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            if (conn != null) {
                // Execute existing tables
                stmt.execute(sqlUsers);
                stmt.execute(sqlStudents);
                
                // Execute NEW tables
                stmt.execute(sqlSessions);
                stmt.execute(sqlAssignments);
                
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
        
        // Insert Coordinator (Coordinator)
        String insertCoordinator = "INSERT INTO users(user_id, username, password, role) VALUES('a001', 'admin', '123', 'Coordinator')";
        
        // Insert Student (Student)
        String insertStudent = "INSERT INTO users(user_id, username, password, role) VALUES('s001', 'student1', '123', 'Student')";
        
        // NEW: Insert Evaluator (So you have someone to assign in your dashboard)
        String insertEvaluator = "INSERT INTO users(user_id, username, password, role) VALUES('e001', 'evaluator1', '123', 'Evaluator')";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {
            rs.next();
            if (rs.getInt(1) == 0) { 
                stmt.executeUpdate(insertCoordinator);
                stmt.executeUpdate(insertStudent);
                stmt.executeUpdate(insertEvaluator); 
                System.out.println("Default users inserted.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}