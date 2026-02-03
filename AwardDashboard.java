import java.awt.*;
import javax.swing.*;

// AwardDashboard provides a simple UI for staff to compute and view award winners
// This dashboard displays Best Oral, Best Poster, and People's Choice results
public class AwardDashboard extends JFrame {

    private JTextArea output; // area to display computed award results

    public AwardDashboard() {
        super("Awards & Ceremony");

        // Close only this window without exiting the entire application
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header label for award computation section
        JLabel header = new JLabel("Compute Awards", SwingConstants.CENTER);
        header.setFont(new Font("Serif", Font.BOLD, 20));
        header.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        add(header, BorderLayout.NORTH);

        // Text area used to display award winners
        output = new JTextArea();
        output.setEditable(false); // read-only display
        add(new JScrollPane(output), BorderLayout.CENTER);

        // Buttons for computing and clearing award results
        JPanel btnPanel = new JPanel();
        JButton computeBtn = new JButton("Compute Winners");
        JButton clearBtn = new JButton("Clear");
        btnPanel.add(computeBtn);
        btnPanel.add(clearBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // Trigger award computation
        computeBtn.addActionListener(e -> compute());

        // Clear displayed output
        clearBtn.addActionListener(e -> output.setText(""));

        setVisible(true);
    }

    // Compute all awards using AwardCalculator
    private void compute() {
        output.setText(""); // reset output area

        // Compute Best Oral based on highest average score among Oral presentations
        AwardResult bestOral = AwardCalculator.computeBestByType("Oral Presentation");

        // Compute Best Poster based on highest average score among Poster presentations
        AwardResult bestPoster = AwardCalculator.computeBestByType("Poster Presentation");

        // Compute People's Choice as highest overall average evaluation score
        AwardResult peoples = AwardCalculator.computePeoplesChoice();

        output.append("=== AWARD WINNERS ===\n\n");
        output.append(format("Best Oral", bestOral));
        output.append(format("Best Poster", bestPoster));
        output.append(format("People's Choice", peoples));

        // Save computed award results into awards table for record keeping
        AwardCalculator.saveAwards(bestOral, bestPoster, peoples);

        output.append("\n(Saved into awards table)\n");
    }

    // Format award result for display
    private String format(String title, AwardResult r) {
        if (r == null) return title + ": No data\n\n";

        return title + ":\n" +
                "  Student: " + r.studentName + " (" + r.studentId + ")\n" +
                "  Submission: " + r.submissionTitle + "\n" +
                "  Total Score: " + r.total + "\n\n";
    }
}
