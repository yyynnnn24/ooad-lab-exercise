import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

// EvaluatorDashboard allows evaluators to view and evaluate assigned submissions
// This dashboard is part of the Evaluation module implemented by Member 3
public class EvaluatorDashboard extends JFrame {

    private String evaluatorId;   // logged-in evaluator ID
    private String evaluatorName; // evaluator display name

    private JTable assignedTable; // table showing assigned submissions
    private DefaultTableModel model;

    public EvaluatorDashboard(String evaluatorId, String evaluatorName) {
        super("Evaluator Dashboard - " + evaluatorName);
        this.evaluatorId = evaluatorId;
        this.evaluatorName = evaluatorName;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header label
        JLabel header = new JLabel("My Assigned Presentations", SwingConstants.CENTER);
        header.setFont(new Font("Serif", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(header, BorderLayout.NORTH);

        // Table columns (submit_id is used internally for evaluation linking)
        String[] cols = {"Submit ID", "Student ID", "Student Name", "Title", "Type", "Status", "My Score"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // prevent direct editing in table
            }
        };

        assignedTable = new JTable(model);
        add(new JScrollPane(assignedTable), BorderLayout.CENTER);

        // Action buttons
        JPanel btnPanel = new JPanel();
        JButton refreshBtn = new JButton("Refresh");
        JButton evaluateBtn = new JButton("Evaluate Selected");
        btnPanel.add(evaluateBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Reload assigned submissions from database
        refreshBtn.addActionListener(e -> loadAssigned());

        // Open evaluation dialog for selected submission
        evaluateBtn.addActionListener(e -> openEvaluateDialog());

        loadAssigned(); // initial data load
        setVisible(true);
    }

    // Load submissions assigned to this evaluator
    private void loadAssigned() {
        model.setRowCount(0); // clear existing rows

        // Assignment links evaluator -> student -> submission
        // LEFT JOIN evaluations allows checking evaluation status and score
        String sql =
            "SELECT s.submit_id, u.user_id AS student_id, u.username AS student_name, " +
            "       s.title, s.type, " +
            "       CASE WHEN e.eval_id IS NULL THEN 'Not Evaluated' ELSE 'Evaluated' END AS status, " +
            "       COALESCE(e.total, '-') AS my_total " +
            "FROM assignments a " +
            "JOIN users u ON a.student_id = u.user_id " +
            "JOIN submissions s ON s.student_id = u.user_id " +
            "LEFT JOIN evaluations e ON e.submit_id = s.submit_id AND e.evaluator_id = a.evaluator_id " +
            "WHERE a.evaluator_id = ? " +
            "ORDER BY s.submit_id DESC;";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, evaluatorId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int submitId = rs.getInt("submit_id");
                String studentId = rs.getString("student_id");
                String studentName = rs.getString("student_name");
                String title = rs.getString("title");
                String type = rs.getString("type");
                String status = rs.getString("status");
                String myTotal = String.valueOf(rs.getObject("my_total"));

                model.addRow(new Object[]{
                        submitId, studentId, studentName, title, type, status, myTotal
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
        }
    }

    // Open evaluation dialog for selected submission
    private void openEvaluateDialog() {
        int row = assignedTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a student first.");
            return;
        }

        int submitId = (int) model.getValueAt(row, 0);
        String studentName = (String) model.getValueAt(row, 2);
        String title = (String) model.getValueAt(row, 3);

        // Launch EvaluationDialog for scoring
        EvaluationDialog dialog =
                new EvaluationDialog(this, evaluatorId, submitId, studentName, title);
        dialog.setVisible(true);

        // Refresh table after evaluation is saved or updated
        loadAssigned();
    }

    // Public refresh method used by EvaluationDialog if needed
    public void refresh() {
        loadAssigned();
    }
}
