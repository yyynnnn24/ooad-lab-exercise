import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.swing.*;

public class StudentRegistration extends JFrame {

    private JTextField researchTitle;
    private JTextArea researchAbstract;
    private JTextField supervisorName;
    private JComboBox<String> comboType;
    private JLabel filePath;
    private JButton uploadBtn;
    private JButton submitBtn;

    private String selectedFilePath = "";
    
    // We need to store the User ID to save it to the database later
    private String currentUserId; 

    // UPDATE: Constructor now accepts userId AND username
    public StudentRegistration(String userId, String username) {
        this.currentUserId = userId; // Save the ID

        setTitle("Student Registration");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE); // Don't exit app, just close window
        setSize(600, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- FIXED LAYOUT (Header) ---
        // We create a panel to hold BOTH labels
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        
        JLabel titleLabel = new JLabel("Student Registration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel welcomeLabel = new JLabel("Welcome back, " + username + " (ID: " + userId + ")", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        topPanel.add(titleLabel);
        topPanel.add(welcomeLabel);
        
        add(topPanel, BorderLayout.NORTH); 
        // -----------------------------

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        formPanel.add(new JLabel("Research Title:"));
        researchTitle = new JTextField(20);
        formPanel.add(researchTitle);

        formPanel.add(new JLabel("Research Abstract:"));
        researchAbstract = new JTextArea(10, 20);
        researchAbstract.setLineWrap(true);
        researchAbstract.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(researchAbstract);
        formPanel.add(scrollPane);

        formPanel.add(new JLabel("Supervisor Name:"));
        supervisorName = new JTextField(20);
        formPanel.add(supervisorName);

        formPanel.add(new JLabel("Type:"));
        String[] types = {"Oral Presentation", "Poster Presentation"};
        comboType = new JComboBox<>(types);
        formPanel.add(comboType);

        formPanel.add(new JLabel("Upload File:"));
        JPanel uploadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filePath = new JLabel("No file chosen");
        uploadBtn = new JButton("Upload");
        uploadPanel.add(filePath);
        uploadPanel.add(uploadBtn);
        formPanel.add(uploadPanel);

        uploadBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(StudentRegistration.this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                selectedFilePath = selectedFile.getAbsolutePath();
                filePath.setText(selectedFile.getName()); // Just show filename to keep it clean
            }
        });

        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.add(formPanel);
        add(wrapperPanel, BorderLayout.CENTER);

        submitBtn = new JButton("Submit");
        JPanel btnPanel = new JPanel();
        btnPanel.add(submitBtn);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        add(btnPanel, BorderLayout.SOUTH);

        // --- DATABASE LOGIC ---
        submitBtn.addActionListener(e -> {
            submitData();
        });

        setVisible(true);
    }

    private void submitData() {
        String title = researchTitle.getText();
        String abs = researchAbstract.getText();
        String supervisor = supervisorName.getText();
        String type = (String) comboType.getSelectedItem();

        // Basic Validation
        if(title.isEmpty() || supervisor.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // SQL Insert
        String sql = "INSERT INTO submissions(title, abstract, supervisor, type, filepath, student_id) VALUES(?,?,?,?,?,?)";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, title);
            pstmt.setString(2, abs);
            pstmt.setString(3, supervisor);
            pstmt.setString(4, type);
            pstmt.setString(5, selectedFilePath);
            pstmt.setString(6, this.currentUserId); // Using the ID we saved in the constructor

            pstmt.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "Registration Submitted Successfully!");
            
            // Optional: Close window or clear fields
            // dispose(); 

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // Main method for testing isolated UI (Fake login)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentRegistration("s001", "Student"));
    }
}