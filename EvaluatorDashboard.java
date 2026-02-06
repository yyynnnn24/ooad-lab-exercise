import java.awt.*;
import java.awt.event.*;
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

        // Define table columns for assigned submissions (KEEP ORIGINAL)
        String[] cols = {
            "Submit ID", "Student ID", "Student Name",
            "Title", "Research Abstract", "Type", "Status", "My Score", "File Path"
        };

        // Initialize table model with non-editable cells (KEEP ORIGINAL)
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Create table using the model
        assignedTable = new JTable(model);
        add(new JScrollPane(assignedTable), BorderLayout.CENTER);

        // ✅ NEW: double click row to show details dialog
        assignedTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && assignedTable.getSelectedRow() != -1) {
                    int viewRow = assignedTable.getSelectedRow();
                    int row = assignedTable.convertRowIndexToModel(viewRow);
                    showDetailsDialogForRow(row);
                }
            }
        });

        // Panel containing action buttons (KEEP ORIGINAL)
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

        // SQL query retrieves assigned submissions along with evaluation status and score (KEEP ORIGINAL)
        String sql =
            "SELECT s.submit_id, u.user_id AS student_id, u.username AS student_name, " +
            "       s.title, s.abstract, s.type, s.filepath, " +
            "       CASE WHEN e.eval_id IS NULL THEN 'Not Evaluated' ELSE 'Evaluated' END AS status, " +
            "       COALESCE(e.total, '-') AS my_total " +
            "FROM assignments a " +
            "JOIN sessions sess ON sess.session_id = a.session_id " +
            "JOIN users u ON a.student_id = u.user_id " +
            // Only pick the latest submission for that student + session type
            "JOIN submissions s ON s.submit_id = ( " +
            "    SELECT MAX(s2.submit_id) " +
            "    FROM submissions s2 " +
            "    WHERE s2.student_id = u.user_id " +
            "      AND s2.type = sess.session_type " +
            ") "  +
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

    // Opens the uploaded poster/image/PDF file using the system default application (KEEP ORIGINAL)
    private void openSelectedFile() {
        int viewRow = assignedTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a submission first.");
            return;
        }

        int row = assignedTable.convertRowIndexToModel(viewRow);

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

            Desktop.getDesktop().open(f);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Cannot open file: " + ex.getMessage());
        }
    }

    // ✅ NEW: Details dialog for a specific row (shows full content + View File button)
    private void showDetailsDialogForRow(int row) {
        if (row < 0) return;

        int submitId = Integer.parseInt(String.valueOf(model.getValueAt(row, model.findColumn("Submit ID"))));
        String studentId = String.valueOf(model.getValueAt(row, model.findColumn("Student ID")));
        String studentName = String.valueOf(model.getValueAt(row, model.findColumn("Student Name")));

        String title = String.valueOf(model.getValueAt(row, model.findColumn("Title")));
        String abstractText = String.valueOf(model.getValueAt(row, model.findColumn("Research Abstract")));
        String type = String.valueOf(model.getValueAt(row, model.findColumn("Type")));
        String status = String.valueOf(model.getValueAt(row, model.findColumn("Status")));
        String myScore = String.valueOf(model.getValueAt(row, model.findColumn("My Score")));
        String filepath = String.valueOf(model.getValueAt(row, model.findColumn("File Path")));

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        area.setText(
                "Submit ID: " + submitId + "\n" +
                "Student: " + studentName + " (" + studentId + ")\n" +
                "Type: " + type + "\n" +
                "Status: " + status + "\n" +
                "My Score: " + myScore + "\n\n" +
                "Title:\n" + (title == null ? "" : title) + "\n\n" +
                "Research Abstract:\n" + (abstractText == null ? "" : abstractText)
        );

        JScrollPane pane = new JScrollPane(area);
        pane.setPreferredSize(new Dimension(720, 420));

        JButton viewFileBtn = new JButton("View File");
        viewFileBtn.addActionListener(e -> openFileByPath(filepath));

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomBar.add(viewFileBtn);

        JDialog dialog = new JDialog(this, "Submission Details", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(pane, BorderLayout.CENTER);
        dialog.add(bottomBar, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ✅ NEW: open file safely by a given path (re-used by details dialog)
    private void openFileByPath(String filepath) {
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
