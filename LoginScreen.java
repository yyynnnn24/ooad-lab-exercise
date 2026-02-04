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

        // Initialize DB table on startup
        DatabaseHandler.createNewTable();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 500); // Increased height slightly to fit the logo
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- HEADER SECTION (Logo + Title) ---
        JPanel headerPanel = new JPanel(new BorderLayout());

        // 1. Load "mmu.png"
        // Make sure mmu.png is in the same folder as your .java files!
        ImageIcon originalIcon = new ImageIcon("MMU.png"); 
        
        // 2. Resize the image (e.g., width 150, height 80)
        // This prevents the logo from being too huge
        Image img = originalIcon.getImage();
        Image scaledImg = img.getScaledInstance(250, 80, Image.SCALE_SMOOTH);
        ImageIcon logoIcon = new ImageIcon(scaledImg);

        // 3. Add Logo to Label
        JLabel logoLabel = new JLabel(logoIcon, SwingConstants.CENTER);
        logoLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); // Top padding

        // 4. Add Title
        JLabel titleLabel = new JLabel("FCI Seminar Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(14, 10, 20, 10));

        // 5. Add both to the Header Panel
        headerPanel.add(logoLabel, BorderLayout.NORTH);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Add header to the main frame
        add(headerPanel, BorderLayout.NORTH);

        // --- FORM SECTION ---
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(3, 2, 10, 10));

        formPanel.add(new JLabel("User ID:"));
        userIDField = new JTextField(15);
        formPanel.add(userIDField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        formPanel.add(new JLabel("Role:"));
        // Matches your latest requirement (Student, Evaluator, Coordinator)
        String[] roles = {"Student", "Evaluator", "Coordinator"};
        roleComboBox = new JComboBox<>(roles);
        formPanel.add(roleComboBox);

        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.add(formPanel);
        add(wrapperPanel, BorderLayout.CENTER);

        // --- BUTTON SECTION ---
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

                if (userID.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginScreen.this,
                            "Please enter both User ID and password.",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = "SELECT user_id, username, role FROM users WHERE user_id = ? AND password = ? AND role = ?";

                try (Connection conn = DatabaseHandler.connect();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {

                    pstmt.setString(1, userID);
                    pstmt.setString(2, password);
                    pstmt.setString(3, selectedRole);

                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        String dbUserId = rs.getString("user_id");
                        String dbUsername = rs.getString("username");

                        if ("Student".equals(selectedRole)) {
                            JOptionPane.showMessageDialog(LoginScreen.this, "Login Successful! Redirecting...");
                            LoginScreen.this.dispose();
                            new StudentRegistration(dbUserId, dbUsername);
                        } else if ("Coordinator".equals(selectedRole)) {
                            JOptionPane.showMessageDialog(LoginScreen.this, "Login Successful! Opening Coordinator Dashboard...");
                            LoginScreen.this.dispose();
                            new CoordinatorDashboard(dbUserId, dbUsername);
                        } else if ("Evaluator".equals(selectedRole)) {
                            JOptionPane.showMessageDialog(LoginScreen.this, "Login Successful! Opening Evaluator Dashboard...");
                            LoginScreen.this.dispose();
                            // Assuming you have this class ready
                            new EvaluatorDashboard(dbUserId, dbUsername);
                        } else {
                            JOptionPane.showMessageDialog(LoginScreen.this,
                                    "Login Successful as " + selectedRole + ".",
                                    "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(LoginScreen.this,
                                "Invalid User ID, Password, or Role.\n",
                                "Login Failed",
                                JOptionPane.ERROR_MESSAGE);
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