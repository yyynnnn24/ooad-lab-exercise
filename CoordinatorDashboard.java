import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CoordinatorDashboard extends JFrame {
    private JTable submissionsTable;
    private DefaultTableModel tableModel;

    // Save staff info for later use
    private String currentStaffId;
    private String currentStaffName;

    public CoordinatorDashboard(String staffId, String staffName) {
        super("Coordinator Dashboard - " + staffName);
        this.currentStaffId = staffId;
        this.currentStaffName = staffName;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- TOP PANEL (Header + User Info) ---
        JPanel topContainer = new JPanel(new BorderLayout());
        
        // 1. Title
        JLabel header = new JLabel("Seminar Session Management", SwingConstants.CENTER);
        header.setFont(new Font("Serif", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topContainer.add(header, BorderLayout.CENTER);

        // 2. User Info & Logout
        JPanel userPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("  Welcome, " + staffName + " (ID: " + staffId + ")");
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(255, 100, 100));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> {
            this.dispose();
            new LoginScreen(); 
        });

        userPanel.add(welcomeLabel, BorderLayout.WEST);
        userPanel.add(logoutBtn, BorderLayout.EAST);
        userPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        topContainer.add(userPanel, BorderLayout.SOUTH);
        
        // Add the whole top container to the frame
        add(topContainer, BorderLayout.NORTH);

        // --- CENTER: LIST OF STUDENT SUBMISSIONS ---
        String[] columnNames = {"Student ID", "Name", "Title", "Type", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent editing table directly
            }
        };
        submissionsTable = new JTable(tableModel);
        add(new JScrollPane(submissionsTable), BorderLayout.CENTER);

        // --- BOTTOM: ACTION BUTTONS ---
        JPanel btnPanel = new JPanel();
        JButton createSessionBtn = new JButton("Create New Session");
        JButton assignBtn = new JButton("Assign Selected to Session");
        JButton viewSessionsBtn = new JButton("View All Sessions");
        JButton refreshBtn = new JButton("Refresh List");
        JButton awardBtn = new JButton("Compute Awards");

        btnPanel.add(createSessionBtn);
        btnPanel.add(assignBtn);
        btnPanel.add(viewSessionsBtn);
        btnPanel.add(refreshBtn);
        btnPanel.add(awardBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // --- BUTTON ACTIONS ---
        createSessionBtn.addActionListener(e -> openCreateSessionDialog());
        refreshBtn.addActionListener(e -> loadSubmissions());
        viewSessionsBtn.addActionListener(e -> showAllSessions());
        
        // Placeholder for Award Dashboard
        awardBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Award Calculation Feature Coming Soon!"));

        assignBtn.addActionListener(e -> {
            int selectedRow = submissionsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a student from the list first.");
            } else {
                String studentId = (String) tableModel.getValueAt(selectedRow, 0);
                String studentName = (String) tableModel.getValueAt(selectedRow, 1);
                openAssignDialog(studentId, studentName);
            }
        });

        // Load data on startup
        loadSubmissions(); 

        setVisible(true);
    }

    private void loadSubmissions() {
        tableModel.setRowCount(0); 
        // Note: This query requires the 'assignments' table to exist
        String sql = "SELECT u.user_id, u.username, s.title, s.type, " +
                     "(SELECT count(*) FROM assignments a WHERE a.student_id = u.user_id) as is_assigned " +
                     "FROM submissions s JOIN users u ON s.student_id = u.user_id";

        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("user_id");
                String name = rs.getString("username");
                String title = rs.getString("title");
                String type = rs.getString("type");
                int assignedCount = rs.getInt("is_assigned");
                
                String status = (assignedCount > 0) ? "Assigned" : "Pending";
                tableModel.addRow(new Object[]{id, name, title, type, status});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }
    }

    // --- HELPER: CHECK FOR CONFLICTS ---
    private boolean isSlotBooked(String date, String time, String venue) {
        String sql = "SELECT count(*) FROM sessions WHERE date = ? AND time = ? AND venue = ?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, date);
            pstmt.setString(2, time);
            pstmt.setString(3, venue);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Returns true if count > 0 (Slot is taken)
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void openCreateSessionDialog() {
        JTextField dateField = new JTextField("2026-02-15");
        JTextField timeField = new JTextField("09:00"); 
        JTextField venueField = new JTextField("Room 101");
        String[] types = {"Oral Presentation", "Poster Presentation"};
        JComboBox<String> typeBox = new JComboBox<>(types);

        Object[] message = {
            "Date (YYYY-MM-DD):", dateField,
            "Time (HH:MM):", timeField, 
            "Venue:", venueField,
            "Session Type:", typeBox
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Create Session", JOptionPane.OK_CANCEL_OPTION);
        
        if (option == JOptionPane.OK_OPTION) {
            // --- INPUT VALIDATION & CONFLICT CHECK ---
            String date = dateField.getText().trim();
            String time = timeField.getText().trim();
            String venue = venueField.getText().trim();
            String type = (String) typeBox.getSelectedItem();

            // 1. Check if any field is empty
            if (date.isEmpty() || time.isEmpty() || venue.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Error: All fields are required!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return; 
            }

            // 2. Check Date Format (Simple YYYY-MM-DD check)
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(this, "Error: Date must be in YYYY-MM-DD format (e.g. 2026-03-15).", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 3. Check Time Format (HH:MM)
            if (!time.matches("([01]\\d|2[0-3]):[0-5]\\d")) {
                JOptionPane.showMessageDialog(this, "Error: Time must be in HH:MM format (24-hour) and contain only numbers.\nExample: 09:00 or 14:30", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 4. Check for Conflicts (Same Date + Time + Venue)
            if (isSlotBooked(date, time, venue)) {
                JOptionPane.showMessageDialog(this, 
                    "Conflict Error: The venue '" + venue + "' is already booked at " + time + " on " + date + ".\nPlease choose a different time or venue.", 
                    "Booking Conflict", 
                    JOptionPane.ERROR_MESSAGE);
                return; 
            }

            // If we pass all checks, THEN save
            saveSession(date, time, venue, type);
        }
    }

    private void saveSession(String date, String time, String venue, String type) {
        String sql = "INSERT INTO sessions(date, time, venue, session_type) VALUES(?,?,?,?)";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            pstmt.setString(2, time); 
            pstmt.setString(3, venue);
            pstmt.setString(4, type);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Session Created Successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage());
        }
    }

    // --- THE ASSIGNMENT LOGIC ---
    private void openAssignDialog(String studentId, String studentName) {
        // 1. Get List of Sessions
        ArrayList<String> sessionList = new ArrayList<>();
        ArrayList<Integer> sessionIds = new ArrayList<>();
        
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM sessions")) {
            while (rs.next()) {
                sessionIds.add(rs.getInt("session_id"));
                sessionList.add(rs.getString("date") + " " + rs.getString("time") + 
                                " - " + rs.getString("venue") + " (" + rs.getString("session_type") + ")");
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // 2. Get List of Evaluators
        ArrayList<String> evaluatorList = new ArrayList<>();
        ArrayList<String> evaluatorIds = new ArrayList<>();
        
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT user_id, username FROM users WHERE role = 'Evaluator'")) {
            while (rs.next()) {
                evaluatorIds.add(rs.getString("user_id"));
                evaluatorList.add(rs.getString("username"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (sessionList.isEmpty() || evaluatorList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You need at least one Session and one Evaluator created first!");
            return;
        }

        JComboBox<String> sessionBox = new JComboBox<>(sessionList.toArray(new String[0]));
        JComboBox<String> evaluatorBox = new JComboBox<>(evaluatorList.toArray(new String[0]));

        Object[] message = {
            "Assign Student: " + studentName,
            "Select Session:", sessionBox,
            "Select Evaluator:", evaluatorBox
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Assign Presentation", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            int selectedSessionIdx = sessionBox.getSelectedIndex();
            int selectedEvaluatorIdx = evaluatorBox.getSelectedIndex();

            assignToDatabase(studentId, sessionIds.get(selectedSessionIdx), evaluatorIds.get(selectedEvaluatorIdx));
        }
    }

    private void assignToDatabase(String studentId, int sessionId, String evaluatorId) {
        String sql = "INSERT INTO assignments(session_id, student_id, evaluator_id) VALUES(?,?,?)";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);
            pstmt.setString(2, studentId);
            pstmt.setString(3, evaluatorId);
            pstmt.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Assignment Successful!");
            loadSubmissions(); // Refresh the table to show "Assigned"
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void showAllSessions() {
        JTextArea sessionText = new JTextArea(10, 50); 
        sessionText.setEditable(false);
        
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM sessions")) {
            sessionText.append("ID | Date       | Time  | Venue      | Type\n");
            sessionText.append("----------------------------------------------------------\n");
            while (rs.next()) {
                sessionText.append(rs.getInt("session_id") + " | " + 
                                   rs.getString("date") + " | " + 
                                   rs.getString("time") + " | " + 
                                   rs.getString("venue") + " | " + 
                                   rs.getString("session_type") + "\n");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        JOptionPane.showMessageDialog(this, new JScrollPane(sessionText), "All Sessions", JOptionPane.INFORMATION_MESSAGE);
    }
}