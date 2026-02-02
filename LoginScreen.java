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
                
                JOptionPane.showMessageDialog(LoginScreen.this,
                        "Username: " + username + "\nPassword: " + password + "\nRole: " + role,
                        "Login Info",
                        JOptionPane.INFORMATION_MESSAGE);
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