import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginScreen extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private JButton loginButton;

    public LoginScreen() {
        super("FCI Seminar Management System");

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

        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField(15); 
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        formPanel.add(new JLabel("Role:"));
        String[] roles = {"Student", "Evaluator", "Staff"};
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

        
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                String role = (String) roleComboBox.getSelectedItem();

                if(username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginScreen.this,
                            "Please enter both username and password.",
                            "Input Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if(username.equals("admin") && password.equals("123")) {
        
                    // Credentials are correct, now check Role
                    if(role.equals("Student")) {
                        // SUCCESS: Transition to next screen
                        JOptionPane.showMessageDialog(LoginScreen.this, "Login Successful! Redirecting...");
                        LoginScreen.this.dispose(); // Close Login Window
                        new StudentRegistration(username);  // Open Student Window
                    } else {
                        // Valid credentials, but Role is not Student (Just for testing)
                        JOptionPane.showMessageDialog(LoginScreen.this, 
                            "Login Successful as " + role + ".\n(This dashboard is not ready yet)", 
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    }

                } else {
                    // FAILED: Wrong credentials
                    JOptionPane.showMessageDialog(LoginScreen.this, 
                        "Invalid Username or Password.\n(Try: admin / 123)", 
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                }
                
            }
        });
        
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new LoginScreen();
            }
        });
    }
}