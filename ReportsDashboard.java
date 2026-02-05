import javax.swing.*;
import java.awt.*;

public class ReportsDashboard extends JFrame {
    public ReportsDashboard() {
        super("Generate Reports");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(new ReportsPanel(), BorderLayout.CENTER);
    }
}

