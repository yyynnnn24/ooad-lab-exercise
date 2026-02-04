import java.awt.*;
import java.io.File;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;    // Used for displaying tabular data in JTable

// EvaluatorDashboard allows evaluators to view assigned submissions,
// open uploaded presentation files, and perform evaluations.
public class EvaluatorDashboard extends JFrame {

    // Stores the currently logged-in evaluator's ID
    private String evaluatorId;

    // Stores the evaluator's display name
    private String evaluatorName;

    // JTable used to display assigned submissions
    private JTable assignedTable;

    // Table model backing the JTable
    private DefaultTableModel model;

    // Constructor initializes the evaluator dashboard UI
    public EvaluatorDashboard(String evaluatorId, String evaluatorName) {
        super("Evaluator Dashboard - " + evaluatorName);
        this.evaluatorId = evaluatorId;
        this.evaluatorName = evaluatorName;

        // Basic window settings
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header label for the dashboard title
        JLabel header = new JLabel("My Assigned Presentations", SwingConstants.CENTER);
        header.setFont(new Font("Serif", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        // Top panel containing evaluator info and logout button
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Label showing evaluator identity
        JLabel infoLabel = new JLabel("Evaluator: " + evaluatorName + " (" + evaluatorId + ")");
        infoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Logout button to return to login screen
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(220, 80, 80));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> {
            this.dispose();          // Close evaluator dashboard
            new LoginScreen();       // Return to login screen
        });

        // Place evaluator info and logout button on the top panel
        topPanel.add(infoLabel, BorderLayout.WEST);
        topPanel.add(logoutBtn, BorderLayout.EAST);

        // Combine top panel and header into a single north wrapper
        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(topPanel, BorderLayout.NORTH);
        northWrapper.add(header, BorderLayout.CENTER);

        add(northWrapper, BorderLayout.NORTH);

        // Define table columns for assigned submissions
        String[] cols = {
            "Submit ID", "Student ID", "Student Name",
            "Title", "Research Abstract", "Type", "Status", "My Score", "File Path"
        };

        // Initialize table model with non-editable cells
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Prevent editing directly in the table
            }
        };

        // Create table using the model
        assignedTable = new JTable(model);
        add(new JScrollPane(assignedTable), BorderLayout.CENTER);

        // Panel containing action buttons
        JPanel btnPanel = new JPanel();
        JButton refreshBtn = new JButton("Refresh");
        JButton evaluateBtn = new JButton("Evaluate Selected");
        JButton viewFileBtn = new JButton("View File");

        btnPanel.add(viewFileBtn);
        btnPanel.add(evaluateBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Reload assigned submissions from database
        refreshBtn.addActionListener(e -> loadAssigned());

        // Open uploaded presentation file
        viewFileBtn.addActionListener(e -> openSelectedFile());

        // Open evaluation dialog for selected submission
        evaluateBtn.addActionListener(e -> openEvaluateDialog());

        // Load data when dashboard is first displayed
        loadAssigned();
        setVisible(true);
    }

    // Loads all submissions assigned to the current evaluator
    private void loadAssigned() {
        model.setRowCount(0); // Clear existing table rows

        // SQL query retrieves assigned submissions along with evaluation status and score
        String sql =
    "SELECT s.submit_id, u.user_id AS student_id, u.username AS student_name, " +
    "       s.title, s.abstract, s.type, s.filepath, " +
    "       CASE WHEN e.eval_id IS NULL THEN 'Not Evaluated' ELSE 'Evaluated' END AS status, " +
    "       COALESCE(e.total, '-') AS my_total " +
    "FROM assignments a " +
    "JOIN sessions sess ON sess.session_id = a.session_id " +
    "JOIN users u ON a.student_id = u.user_id " +
    "JOIN submissions s ON s.student_id = u.user_id AND s.type = sess.session_type " +
    "LEFT JOIN evaluations e ON e.submit_id = s.submit_id AND e.evaluator_id = a.evaluator_id " +
    "WHERE a.evaluator_id = ? " +
    "ORDER BY s.submit_id DESC;";


        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, evaluatorId);
            ResultSet rs = ps.executeQuery();

            // Populate table with query results
            while (rs.next()) {
                int submitId = rs.getInt("submit_id");
                String studentId = rs.getString("student_id");
                String studentName = rs.getString("student_name");
                String title = rs.getString("title");
                String abstractText = rs.getString("abstract");
                String type = rs.getString("type");
                String status = rs.getString("status");
                String myTotal = String.valueOf(rs.getObject("my_total"));
                String filepath = rs.getString("filepath");

                model.addRow(new Object[]{
                    submitId, studentId, studentName,
                    title, abstractText, type, status, myTotal, filepath
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
        }
    }

    // Opens the evaluation dialog for the selected submission
    private void openEvaluateDialog() {
        int row = assignedTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student first.");
            return;
        }

        int submitId = (int) model.getValueAt(row, 0);
        String studentName = (String) model.getValueAt(row, 2);
        String title = (String) model.getValueAt(row, 3);

        // Launch evaluation dialog
        EvaluationDialog dialog =
                new EvaluationDialog(this, evaluatorId, submitId, studentName, title);
        dialog.setVisible(true);

        // Refresh table after evaluation is completed
        loadAssigned();
    }

    // Opens the uploaded poster/image/PDF file using the system default application
    private void openSelectedFile() {
        int viewRow = assignedTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a submission first.");
            return;
        }

        // Convert table view index to model index
        int row = assignedTable.convertRowIndexToModel(viewRow);

        // Retrieve file path column safely by column name
        int fpCol = model.findColumn("File Path");
        if (fpCol == -1) {
            JOptionPane.showMessageDialog(this, "File Path column not found.");
            return;
        }

        String filepath = (String) model.getValueAt(row, fpCol);
        if (filepath == null || filepath.trim().isEmpty() ||
            "null".equalsIgnoreCase(filepath.trim())) {
            JOptionPane.showMessageDialog(this, "No file uploaded for this submission.");
            return;
        }

        try {
            File f = new File(filepath);
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this, "File not found:\n" + filepath);
                return;
            }

            // Open file with OS default viewer
            Desktop.getDesktop().open(f);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot open file: " + ex.getMessage());
        }
    }

    // Allows external components to refresh the evaluator dashboard
    public void refresh() {
        loadAssigned();
    }
}
