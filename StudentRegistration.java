import java.awt.*;
import java.io.File;
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

    public StudentRegistration(String username) {
        setTitle("Student Registration");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Student Registration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(titleLabel, BorderLayout.NORTH);

        JLabel welcomeLabel = new JLabel("Welcome back, " + username, SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Serif", Font.BOLD, 18));
        welcomeLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(welcomeLabel, BorderLayout.NORTH); // Put it at the top (North is usually better for titles)
        
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
                filePath.setText(selectedFilePath);
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

        submitBtn.addActionListener(e -> {
            String title = researchTitle.getText();
            String abs = researchAbstract.getText();
            String supervisor = supervisorName.getText();
            String type = (String) comboType.getSelectedItem();

            String message = "Research Title: " + title + "\n"
                    + "Research Abstract: " + abs + "\n"
                    + "Supervisor Name: " + supervisor + "\n"
                    + "Type: " + type + "\n"
                    + "Uploaded File: " + (selectedFilePath.isEmpty() ? "No file chosen" : selectedFilePath);

            javax.swing.JOptionPane.showMessageDialog(StudentRegistration.this, message, "Submission Details", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentRegistration("Student"));
    }
}
