import java.awt.*;
import java.sql.*;
import javax.swing.*;

// EvaluationDialog allows an evaluator to score an assigned submission
// This dialog supports creating and updating evaluations 
public class EvaluationDialog extends JDialog {

    private String evaluatorId; // logged-in evaluator ID
    private int submitId;       // submission being evaluated

    // Rubric score inputs
    private JSpinner clarity;
    private JSpinner methodology;
    private JSpinner results;
    private JSpinner presentation;

    private JTextArea comments; // qualitative feedback

    public EvaluationDialog(Frame owner, String evaluatorId, int submitId, String studentName, String title) {
        super(owner, "Evaluate: " + studentName, true);
        this.evaluatorId = evaluatorId;
        this.submitId = submitId;

        setSize(520, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Display student name and submission title
        JLabel header = new JLabel("<html><b>" + studentName + "</b><br/>" + title + "</html>");
        header.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(header, BorderLayout.NORTH);

        // Form layout for evaluation rubric
        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Score spinners (0–5 scale)
        clarity = spinner0to5();
        methodology = spinner0to5();
        results = spinner0to5();
        presentation = spinner0to5();

        form.add(new JLabel("Problem Clarity (0-5):"));
        form.add(clarity);

        form.add(new JLabel("Methodology (0-5):"));
        form.add(methodology);

        form.add(new JLabel("Results (0-5):"));
        form.add(results);

        form.add(new JLabel("Presentation (0-5):"));
        form.add(presentation);

        form.add(new JLabel("Comments:"));
        comments = new JTextArea(5, 20);
        comments.setLineWrap(true);
        comments.setWrapStyleWord(true);
        form.add(new JScrollPane(comments));

        add(form, BorderLayout.CENTER);

        // Action buttons
        JPanel btn = new JPanel();
        JButton saveBtn = new JButton("Save / Update");
        JButton cancelBtn = new JButton("Cancel");
        btn.add(saveBtn);
        btn.add(cancelBtn);
        add(btn, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(e -> saveOrUpdate());

        // Load existing evaluation if evaluator has already evaluated this submission
        loadExistingIfAny();
    }

    // Spinner model with values from 0 to 5 (inclusive)
    private JSpinner spinner0to5() {
        return new JSpinner(new SpinnerNumberModel(0, 0, 5, 1));
    }

    // Load previous evaluation to support update instead of duplicate insert
    private void loadExistingIfAny() {
        String sql = "SELECT clarity, methodology, results, presentation, comments " +
                     "FROM evaluations WHERE evaluator_id = ? AND submit_id = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, evaluatorId);
            ps.setInt(2, submitId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                clarity.setValue(rs.getInt("clarity"));
                methodology.setValue(rs.getInt("methodology"));
                results.setValue(rs.getInt("results"));
                presentation.setValue(rs.getInt("presentation"));
                comments.setText(rs.getString("comments"));
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
        }
    }

    // Save a new evaluation or update existing one
    private void saveOrUpdate() {
        int c = (int) clarity.getValue();
        int m = (int) methodology.getValue();
        int r = (int) results.getValue();
        int p = (int) presentation.getValue();
        String com = comments.getText();

        // Total score is calculated as simple sum (0–20)
        double total = c + m + r + p;

        // Authorization check: evaluator must be assigned to this submission
        if (!isAuthorized()) {
            JOptionPane.showMessageDialog(this, "Not authorized to evaluate this submission.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Update existing evaluation if present
        if (existsEvaluation()) {
            String sql = "UPDATE evaluations SET clarity=?, methodology=?, results=?, presentation=?, total=?, comments=? " +
                         "WHERE evaluator_id=? AND submit_id=?";

            try (Connection conn = DatabaseHandler.connect();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setInt(1, c);
                ps.setInt(2, m);
                ps.setInt(3, r);
                ps.setInt(4, p);
                ps.setDouble(5, total);
                ps.setString(6, com);
                ps.setString(7, evaluatorId);
                ps.setInt(8, submitId);

                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Evaluation updated! Total = " + total);
                dispose();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }

        } else {
            // Insert new evaluation record
            String sql = "INSERT INTO evaluations(evaluator_id, submit_id, clarity, methodology, results, presentation, total, comments) " +
                         "VALUES(?,?,?,?,?,?,?,?)";

            try (Connection conn = DatabaseHandler.connect();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, evaluatorId);
                ps.setInt(2, submitId);
                ps.setInt(3, c);
                ps.setInt(4, m);
                ps.setInt(5, r);
                ps.setInt(6, p);
                ps.setDouble(7, total);
                ps.setString(8, com);

                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Evaluation saved! Total = " + total);
                dispose();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            }
        }
    }

    // Check if evaluation already exists for this evaluator and submission
    private boolean existsEvaluation() {
        String sql = "SELECT count(*) FROM evaluations WHERE evaluator_id=? AND submit_id=?";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, evaluatorId);
            ps.setInt(2, submitId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) {
            return false;
        }
    }

    // Ensure evaluator is assigned to the student owning this submission
    private boolean isAuthorized() {
        String sql =
            "SELECT count(*) " +
            "FROM assignments a " +
            "JOIN submissions s ON s.student_id = a.student_id " +
            "WHERE a.evaluator_id = ? AND s.submit_id = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, evaluatorId);
            ps.setInt(2, submitId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException ex) {
            return false;
        }
    }
}
