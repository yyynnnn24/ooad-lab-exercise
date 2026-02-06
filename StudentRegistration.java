import java.awt.*;
import java.sql.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class StudentRegistration extends JFrame {

    private String currentUserId;
    private String currentUsername;

    // UI Components for the Form
    private JTextField researchTitle;
    private JTextArea researchAbstract;
    private JTextField supervisorName;
    private JComboBox<String> comboType;
    private JLabel filePath;
    private String selectedFilePath = "";
    
    // UI Components for History
    private JTable historyTable;
    private DefaultTableModel tableModel;

    public StudentRegistration(String userId, String username) {
        this.currentUserId = userId;
        this.currentUsername = username;

        setTitle("Student Dashboard - " + username);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 650); 
        setLocationRelativeTo(null);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("New Registration", createSubmissionPanel());
        tabbedPane.addTab("My History & Status", createHistoryPanel());
        
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("  Welcome, " + username + " (ID: " + userId + ")");
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(255, 100, 100));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.addActionListener(e -> {
            this.dispose();
            new LoginScreen(); 
        });
        
        topPanel.add(welcomeLabel, BorderLayout.WEST);
        topPanel.add(logoutBtn, BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        loadHistoryData();
        setVisible(true);
    }

    private JPanel createSubmissionPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Inner panel for the actual form container
        JPanel formPanel = new JPanel(new BorderLayout());
        
        // ADDING THE BOX MARGIN: TitledBorder creates the neat frame
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(20, 40, 20, 40), 
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), " Registration Details ")
        ));
        
        // Padding inside the box - this holds the actual input components
        JPanel paddingPanel = new JPanel(new GridLayout(5, 2, 10, 20));
        paddingPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        paddingPanel.add(new JLabel("Research Title:"));
        researchTitle = new JTextField();
        paddingPanel.add(researchTitle);

        paddingPanel.add(new JLabel("Research Abstract:"));
        researchAbstract = new JTextArea(5, 20);
        researchAbstract.setLineWrap(true);
        researchAbstract.setWrapStyleWord(true);
        paddingPanel.add(new JScrollPane(researchAbstract));

        paddingPanel.add(new JLabel("Supervisor Name:"));
        supervisorName = new JTextField();
        paddingPanel.add(supervisorName);

        paddingPanel.add(new JLabel("Type:"));
        String[] types = {"Oral Presentation", "Poster Presentation"};
        comboType = new JComboBox<>(types);
        paddingPanel.add(comboType);

        paddingPanel.add(new JLabel("Upload File:"));
        JPanel uploadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filePath = new JLabel("No file chosen  ");
        JButton uploadBtn = new JButton("Browse...");
        
        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                filePath.setText(fileChooser.getSelectedFile().getName());
            }
        });
        uploadPanel.add(uploadBtn);
        uploadPanel.add(filePath);
        paddingPanel.add(uploadPanel);

        // CRITICAL FIX: Add the components to formPanel so they appear inside the box
        formPanel.add(paddingPanel, BorderLayout.CENTER); 

        JButton submitBtn = new JButton("Submit Registration");
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.addActionListener(e -> submitData());

        JPanel btnWrapper = new JPanel();
        btnWrapper.add(submitBtn);
        btnWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(btnWrapper, BorderLayout.SOUTH);
        
        return mainPanel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columnNames = {"ID", "Title", "Date", "Venue", "Evaluator", "Status", "Grade", "Comment", "Abstract"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        }; 
        historyTable = new JTable(tableModel);
        
        historyTable.getColumnModel().getColumn(8).setMinWidth(0);
        historyTable.getColumnModel().getColumn(8).setMaxWidth(0);
        historyTable.getColumnModel().getColumn(8).setPreferredWidth(0);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(50);

        historyTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) { 
                    int row = historyTable.getSelectedRow();
                    if (row != -1) {
                        String id = String.valueOf(tableModel.getValueAt(row, 0));
                        String title = String.valueOf(tableModel.getValueAt(row, 1));
                        String date = String.valueOf(tableModel.getValueAt(row, 2));
                        String venue = String.valueOf(tableModel.getValueAt(row, 3));
                        String eval = String.valueOf(tableModel.getValueAt(row, 4));
                        String status = String.valueOf(tableModel.getValueAt(row, 5));
                        String grade = String.valueOf(tableModel.getValueAt(row, 6));
                        String comment = String.valueOf(tableModel.getValueAt(row, 7));
                        String fullAbstract = String.valueOf(tableModel.getValueAt(row, 8));

                        JTextArea textArea = new JTextArea(
                            "Submission ID: " + id + "\n" +
                            "Title: " + title + "\n\n" +
                            "--- Research Abstract ---\n" + fullAbstract + "\n\n" +
                            "--- Schedule ---\n" +
                            "Date: " + date + "\n" +
                            "Venue: " + venue + "\n" +
                            "Evaluator: " + eval + "\n\n" +
                            "--- Result ---\n" +
                            "Status: " + status + "\n" +
                            "Grade: " + grade + "\n" +
                            "Comment: " + comment
                        );
                        textArea.setLineWrap(true);
                        textArea.setWrapStyleWord(true);
                        textArea.setEditable(false);
                        textArea.setBackground(new Color(245, 245, 245));
                        
                        JScrollPane scrollPane = new JScrollPane(textArea);
                        scrollPane.setPreferredSize(new Dimension(500, 450));
                        
                        JOptionPane.showMessageDialog(StudentRegistration.this, scrollPane, "Full Submission Details", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(historyTable);
        panel.add(new JLabel("  Double-click a row to view full abstract and schedule details", SwingConstants.LEFT), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JButton refreshBtn = new JButton("Refresh Status");
        refreshBtn.addActionListener(e -> loadHistoryData());
        panel.add(refreshBtn, BorderLayout.SOUTH);

        return panel;
    }
    
    private void submitData() {
        String title = researchTitle.getText();
        String abs = researchAbstract.getText();
        String supervisor = supervisorName.getText();
        String type = (String) comboType.getSelectedItem();

        if(title.isEmpty() || supervisor.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill required fields.");
            return;
        }

        String sql = "INSERT INTO submissions(title, abstract, supervisor, type, filepath, student_id) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, abs);
            pstmt.setString(3, supervisor);
            pstmt.setString(4, type);
            pstmt.setString(5, selectedFilePath);
            pstmt.setString(6, this.currentUserId);

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Submitted Successfully!");
            
            researchTitle.setText("");
            researchAbstract.setText("");
            supervisorName.setText("");
            filePath.setText("No file chosen");
            loadHistoryData(); 

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

   private void loadHistoryData() {
        tableModel.setRowCount(0);

        String sql = "SELECT s.submit_id, s.title, s.abstract, sess.date, sess.venue, u_eval.username as eval_name, " +
                     "e.total, e.comments, " +
                     "(SELECT count(*) FROM assignments a_check WHERE a_check.student_id = s.student_id) as is_assigned " +
                     "FROM submissions s " +
                     "LEFT JOIN assignments a ON s.student_id = a.student_id " +
                     "LEFT JOIN sessions sess ON a.session_id = sess.session_id " +
                     "LEFT JOIN users u_eval ON a.evaluator_id = u_eval.user_id " +
                     "LEFT JOIN evaluations e ON s.submit_id = e.submit_id " +
                     "WHERE s.student_id = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, this.currentUserId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Vector<String> row = new Vector<>();
                
                row.add(rs.getString("submit_id"));
                row.add(rs.getString("title"));
                row.add(rs.getString("date") != null ? rs.getString("date") : "TBA");
                row.add(rs.getString("venue") != null ? rs.getString("venue") : "TBA");
                row.add(rs.getString("eval_name") != null ? rs.getString("eval_name") : "TBA");

                int assignedCount = rs.getInt("is_assigned");
                double score = rs.getDouble("total");
                boolean isGraded = !rs.wasNull();
                String comments = rs.getString("comments");

                String status = "Pending Review";
                String gradeDisplay = "-";
                String commentDisplay = "-";

                if (isGraded) {
                    status = "Graded";
                    gradeDisplay = String.valueOf(score);
                    commentDisplay = (comments != null) ? comments : "No comments";
                } else if (assignedCount > 0) {
                    status = "Assigned";
                }
                
                row.add(status);
                row.add(gradeDisplay);
                row.add(commentDisplay);
                row.add(rs.getString("abstract"));

                tableModel.addRow(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading history: " + e.getMessage());
        }
    }
}