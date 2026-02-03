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
        setSize(800, 600); 
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
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 20));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));

        formPanel.add(new JLabel("Research Title:"));
        researchTitle = new JTextField();
        formPanel.add(researchTitle);

        formPanel.add(new JLabel("Research Abstract:"));
        researchAbstract = new JTextArea(5, 20);
        researchAbstract.setLineWrap(true);
        formPanel.add(new JScrollPane(researchAbstract));

        formPanel.add(new JLabel("Supervisor Name:"));
        supervisorName = new JTextField();
        formPanel.add(supervisorName);

        formPanel.add(new JLabel("Type:"));
        String[] types = {"Oral Presentation", "Poster Presentation"};
        comboType = new JComboBox<>(types);
        formPanel.add(comboType);

        formPanel.add(new JLabel("Upload File:"));
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
        formPanel.add(uploadPanel);

        JButton submitBtn = new JButton("Submit Registration");
        submitBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        submitBtn.addActionListener(e -> submitData());

        JPanel btnWrapper = new JPanel();
        btnWrapper.add(submitBtn);
        btnWrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(btnWrapper, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columnNames = {"ID", "Title", "Supervisor", "Type", "Status"};
        
        tableModel = new DefaultTableModel(columnNames, 0); 
        historyTable = new JTable(tableModel);
        
        JScrollPane scrollPane = new JScrollPane(historyTable);
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
            loadHistoryData(); // Refresh history after submission

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    // Load submission history for the current user
    private void loadHistoryData() {
        // Clear existing data
        tableModel.setRowCount(0);

        String sql = "SELECT submit_id, title, supervisor, type FROM submissions WHERE student_id = ?";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, this.currentUserId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("submit_id"));
                row.add(rs.getString("title"));
                row.add(rs.getString("supervisor"));
                row.add(rs.getString("type"));
                
                // Placeholder for status, assuming a 'status' column exists                    
                row.add("Pending Review"); 
                
                tableModel.addRow(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}