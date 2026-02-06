import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

// EvaluatorDashboard allows evaluators to view assigned submissions,
// open uploaded presentation files, and perform evaluations.
public class EvaluatorDashboard extends JFrame {

    private String evaluatorId;
    private String evaluatorName;

    private JTable assignedTable;
    private DefaultTableModel model;

    public EvaluatorDashboard(String evaluatorId, String evaluatorName) {
        super("Evaluator Dashboard - " + evaluatorName);
        this.evaluatorId = evaluatorId;
        this.evaluatorName = evaluatorName;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel header = new JLabel("My Assigned Presentations", SwingConstants.CENTER);
        header.setFont(new Font("Serif", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel infoLabel = new JLabel("Evaluator: " + evaluatorName + " (" + evaluatorId + ")");
        infoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(220, 80, 80));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> {
            this.dispose();
            new LoginScreen();
        });

        topPanel.add(infoLabel, BorderLayout.WEST);
        topPanel.add(logoutBtn, BorderLayout.EAST);

        JPanel northWrapper = new JPanel(new BorderLayout());
        northWrapper.add(topPanel, BorderLayout.NORTH);
        northWrapper.add(header, BorderLayout.CENTER);
        add(northWrapper, BorderLayout.NORTH);

        //  Clean table: only key info + clickable "ⓘ Info" column
        String[] cols = {
                "Submit ID", "Student ID", "Student Name",
                "Type", "Status", "My Score",
                "Details"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Only the info button column is editable (to receive click events).
                return column == findColumn("Details");
            }
        };

        assignedTable = new JTable(model);
        assignedTable.setRowHeight(28);
        add(new JScrollPane(assignedTable), BorderLayout.CENTER);

        // Setup "ⓘ Info" button column (no image, blue link style)
        setupInfoButtonColumn();

        JPanel btnPanel = new JPanel();
        JButton refreshBtn = new JButton("Refresh");
        JButton evaluateBtn = new JButton("Evaluate Selected");

        btnPanel.add(evaluateBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> loadAssigned());
        evaluateBtn.addActionListener(e -> openEvaluateDialog());

        // Optional: double click row to open details
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

        loadAssigned();
        setVisible(true);
    }

    // Loads all submissions assigned to the current evaluator
    private void loadAssigned() {
        model.setRowCount(0);

        String sql =
                "SELECT s.submit_id, u.user_id AS student_id, u.username AS student_name, " +
                "       s.title, s.abstract, s.type, s.filepath, " +
                "       CASE WHEN e.eval_id IS NULL THEN 'Not Evaluated' ELSE 'Evaluated' END AS status, " +
                "       COALESCE(e.total, '-') AS my_total " +
                "FROM assignments a " +
                "JOIN sessions sess ON sess.session_id = a.session_id " +
                "JOIN users u ON a.student_id = u.user_id " +
                "JOIN submissions s ON s.submit_id = ( " +
                "    SELECT MAX(s2.submit_id) " +
                "    FROM submissions s2 " +
                "    WHERE s2.student_id = u.user_id " +
                "      AND s2.type = sess.session_type " +
                ") " +
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
                String type = rs.getString("type");
                String status = rs.getString("status");
                String myTotal = String.valueOf(rs.getObject("my_total"));

                model.addRow(new Object[]{
                        submitId, studentId, studentName,
                        type, status, myTotal,
                        "ⓘ Info"
                });
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
        }
    }

    // Opens the evaluation dialog for the selected submission
    private void openEvaluateDialog() {
        int viewRow = assignedTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a submission first.");
            return;
        }
        int row = assignedTable.convertRowIndexToModel(viewRow);

        int submitId = Integer.parseInt(String.valueOf(model.getValueAt(row, model.findColumn("Submit ID"))));
        String studentName = String.valueOf(model.getValueAt(row, model.findColumn("Student Name")));

        // Fetch full title from DB
        String title = "";
        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement("SELECT title FROM submissions WHERE submit_id = ?")) {
            ps.setInt(1, submitId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) title = rs.getString("title");
        } catch (SQLException ex) {
            title = "(Unknown Title)";
        }

        EvaluationDialog dialog =
                new EvaluationDialog(this, evaluatorId, submitId, studentName, title);
        dialog.setVisible(true);

        loadAssigned();
    }

    // Open file by submitId (used from details dialog)
    private void openFileBySubmitId(int submitId) {
        String filepath = null;

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT filepath FROM submissions WHERE submit_id = ?")) {

            ps.setInt(1, submitId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) filepath = rs.getString("filepath");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage());
            return;
        }

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

    // Details dialog: NO file path text; View File button at bottom-right; no Close button
    private void showDetailsDialogForRow(int row) {
        if (row < 0) return;

        int submitId = Integer.parseInt(String.valueOf(model.getValueAt(row, model.findColumn("Submit ID"))));
        String studentId = String.valueOf(model.getValueAt(row, model.findColumn("Student ID")));
        String studentName = String.valueOf(model.getValueAt(row, model.findColumn("Student Name")));
        String type = String.valueOf(model.getValueAt(row, model.findColumn("Type")));
        String status = String.valueOf(model.getValueAt(row, model.findColumn("Status")));
        String myScore = String.valueOf(model.getValueAt(row, model.findColumn("My Score")));

        String fullTitle = "";
        String fullAbstract = "";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT title, abstract FROM submissions WHERE submit_id = ?")) {

            ps.setInt(1, submitId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                fullTitle = rs.getString("title");
                fullAbstract = rs.getString("abstract");
            }

        } catch (SQLException ex) {
            fullAbstract = "(Cannot load details: " + ex.getMessage() + ")";
        }

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
                "Title:\n" + (fullTitle == null ? "" : fullTitle) + "\n\n" +
                "Research Abstract:\n" + (fullAbstract == null ? "" : fullAbstract)
        );

        JScrollPane pane = new JScrollPane(area);
        pane.setPreferredSize(new Dimension(720, 420));

        JButton viewFileBtn = new JButton("View File");
        viewFileBtn.addActionListener(e -> openFileBySubmitId(submitId));

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

    public void refresh() {
        loadAssigned();
    }

    // ============================================================
    // "ⓘ Info" button column (no image, single-click, blue link style)
    // ============================================================

    private void setupInfoButtonColumn() {
        int infoCol = model.findColumn("Details");
        if (infoCol == -1) return;

        assignedTable.getColumnModel().getColumn(infoCol)
                .setCellRenderer(new InfoButtonRenderer());

        assignedTable.getColumnModel().getColumn(infoCol)
                .setCellEditor(new InfoButtonEditor(new JCheckBox()));

        assignedTable.getColumnModel().getColumn(infoCol).setMaxWidth(90);
        assignedTable.getColumnModel().getColumn(infoCol).setMinWidth(90);
    }

    static class InfoButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public InfoButtonRenderer() {
            setText("ⓘ Info");
            setFocusPainted(false);

            // Blue link style
            setForeground(new Color(30, 90, 200));
            setBorderPainted(false);
            setContentAreaFilled(false);

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {

            return this;
        }
    }

    class InfoButtonEditor extends DefaultCellEditor implements ActionListener {
        private final JButton button = new JButton("ⓘ Info");
        private int currentViewRow = -1;

        public InfoButtonEditor(JCheckBox checkBox) {
            super(checkBox);

            button.setFocusPainted(false);
            button.setForeground(new Color(30, 90, 200));
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            button.addActionListener(this);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            currentViewRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "ⓘ Info";
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
            if (currentViewRow < 0) return;
            int modelRow = assignedTable.convertRowIndexToModel(currentViewRow);
            showDetailsDialogForRow(modelRow);
        }
    }
}
