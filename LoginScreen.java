import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;

public class LoginScreen extends JFrame {

    private JTextField userIDField; 
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private JButton loginButton;

    public LoginScreen() {
        super("FCI Seminar Management System");

        // Initialize DB table on startup to avoid "no such table" errors
        DatabaseHandler.createNewTable(); 

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400); 
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("FCI Seminar Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(3, 2, 10, 10)); 

        formPanel.add(new JLabel("User ID:")); 
        userIDField = new JTextField(15); 
        formPanel.add(userIDField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        formPanel.add(new JLabel("Role:"));
        String[] roles = {"Student", "Evaluator", "Coordinator"};
        roleComboBox = new JComboBox<>(roles);
        formPanel.add(roleComboBox);

        JPanel wrapperPanel = new JPanel(new GridBagLayout()); 
        wrapperPanel.add(formPanel); 
        add(wrapperPanel, BorderLayout.CENTER);

        loginButton = new JButton("Login");
        JPanel btnPanel = new JPanel(); 
        btnPanel.add(loginButton);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));
        add(btnPanel, BorderLayout.SOUTH);

        // --- AUTHENTICATION LOGIC ---
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userID = userIDField.getText();
                String password = new String(passwordField.getPassword());
                String selectedRole = (String) roleComboBox.getSelectedItem();

                if(userID.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginScreen.this,
                            "Please enter both User ID and password.",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Query: Check if ID, Password, and Role match
                String sql = "SELECT user_id, username, role FROM users WHERE user_id = ? AND password = ? AND role = ?";

                try (Connection conn = DatabaseHandler.connect();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                    pstmt.setString(1, userID);
                    pstmt.setString(2, password);
                    pstmt.setString(3, selectedRole); 
                    
                    ResultSet rs = pstmt.executeQuery();

                    if(rs.next()) {
                        // --- LOGIN SUCCESS ---
                        String dbUserId = rs.getString("user_id");
                        String dbUsername = rs.getString("username"); 

                        if("Student".equals(selectedRole)) {
                            JOptionPane.showMessageDialog(LoginScreen.this, "Login Successful! Redirecting...");
                            LoginScreen.this.dispose(); 
                            new StudentRegistration(dbUserId, dbUsername);  
                        } 
                        // --- UPDATED PART FOR MEMBER 2 (COORDINATOR) ---
                        else if ("Staff".equals(selectedRole)) {
                            JOptionPane.showMessageDialog(LoginScreen.this, "Login Successful! Opening Coordinator Dashboard...");
                            LoginScreen.this.dispose();
                            
                            // This opens the file you created in the previous step
                            new CoordinatorDashboard(dbUserId, dbUsername);
                        } 
                        // -----------------------------------------------
                        else {
                            JOptionPane.showMessageDialog(LoginScreen.this, 
                                "Login Successful as " + selectedRole + ".\n(Evaluator dashboard is not ready yet)", 
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(LoginScreen.this, 
                            "Invalid User ID, Password, or Role.\n", 
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(LoginScreen.this, "Database Error: " + ex.getMessage());
                }
            }
        });
        
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginScreen());
    }
}