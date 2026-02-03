import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CoordinatorDashboard extends JFrame {
    private JTable submissionsTable;
    private DefaultTableModel tableModel;

    public CoordinatorDashboard(String staffId, String staffName) {
        super("Coordinator Dashboard - " + staffName);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- HEADER ---
        JLabel header = new JLabel("Seminar Session Management", SwingConstants.CENTER);
        header.setFont(new Font("Serif", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(header, BorderLayout.NORTH);

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

        loadSubmissions(); // Load data on startup

        // --- BOTTOM: ACTION BUTTONS ---
        JPanel btnPanel = new JPanel();
        JButton createSessionBtn = new JButton("Create New Session");
        JButton assignBtn = new JButton("Assign Selected to Session");
        JButton viewSessionsBtn = new JButton("View All Sessions"); // New Feature
        JButton refreshBtn = new JButton("Refresh List");

        btnPanel.add(createSessionBtn);
        btnPanel.add(assignBtn);
        btnPanel.add(viewSessionsBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // --- BUTTON ACTIONS ---
        createSessionBtn.addActionListener(e -> openCreateSessionDialog());
        refreshBtn.addActionListener(e -> loadSubmissions());
        
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

        viewSessionsBtn.addActionListener(e -> showAllSessions());

        setVisible(true);
    }

    private void loadSubmissions() {
        tableModel.setRowCount(0); 
        // Logic: Get students and check if they are already in the 'assignments' table
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
        }
    }

    private void openCreateSessionDialog() {
        JTextField dateField = new JTextField("2026-02-15");
        JTextField venueField = new JTextField("Room 101");
        String[] types = {"Oral Presentation", "Poster Presentation"};
        JComboBox<String> typeBox = new JComboBox<>(types);

        Object[] message = {
            "Date (YYYY-MM-DD):", dateField,
            "Venue:", venueField,
            "Session Type:", typeBox
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Create Session", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            saveSession(dateField.getText(), venueField.getText(), (String)typeBox.getSelectedItem());
        }
    }

    private void saveSession(String date, String venue, String type) {
        String sql = "INSERT INTO sessions(date, venue, session_type) VALUES(?,?,?)";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date);
            pstmt.setString(2, venue);
            pstmt.setString(3, type);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Session Created Successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    // --- NEW: THE ASSIGNMENT LOGIC ---
    private void openAssignDialog(String studentId, String studentName) {
        // 1. Get List of Sessions
        ArrayList<String> sessionList = new ArrayList<>();
        ArrayList<Integer> sessionIds = new ArrayList<>();
        
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM sessions")) {
            while (rs.next()) {
                sessionIds.add(rs.getInt("session_id"));
                sessionList.add(rs.getString("date") + " - " + rs.getString("venue") + " (" + rs.getString("session_type") + ")");
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
        // A simple popup to view created sessions
        JTextArea sessionText = new JTextArea(10, 40);
        sessionText.setEditable(false);
        
        try (Connection conn = DatabaseHandler.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM sessions")) {
            sessionText.append("ID | Date | Venue | Type\n");
            sessionText.append("------------------------------------------------\n");
            while (rs.next()) {
                sessionText.append(rs.getInt("session_id") + " | " + 
                                   rs.getString("date") + " | " + 
                                   rs.getString("venue") + " | " + 
                                   rs.getString("session_type") + "\n");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        
        JOptionPane.showMessageDialog(this, new JScrollPane(sessionText), "All Sessions", JOptionPane.INFORMATION_MESSAGE);
    }
}