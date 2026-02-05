import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ReportsPanel extends JPanel {

    private final DefaultTableModel model = new DefaultTableModel();
    private final JTable table = new JTable(model);
    private final JTextArea msg = new JTextArea(4, 30);

    private ReportData current = null;

    public ReportsPanel() {
        setLayout(new BorderLayout(10,10));

        JButton b1 = new JButton("Schedule Report");
        JButton b2 = new JButton("Final Evaluation Report");
        JButton b3 = new JButton("Award Agenda");
        JButton csv = new JButton("Export CSV");
        JButton txt = new JButton("Export TXT");

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(b1); top.add(b2); top.add(b3); top.add(csv); top.add(txt);
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        msg.setEditable(false);
        add(new JScrollPane(msg), BorderLayout.SOUTH);

        b1.addActionListener(e -> load(new ScheduleReportBuilder()));
        b2.addActionListener(e -> load(new FinalEvalReportBuilder()));
        b3.addActionListener(e -> load(new AwardReportBuilder()));

        csv.addActionListener(e -> exportCSV());
        txt.addActionListener(e -> exportTXT());
    }

    private void load(ReportBuilder builder) {
        current = builder.build();
        render(current);
    }

    private void render(ReportData r) {
        model.setRowCount(0);
        model.setColumnCount(0);

        for (String c : r.columns) model.addColumn(c);
        for (List<String> row : r.rows) model.addRow(row.toArray(new String[0]));

        msg.setText("");
        if (!r.warnings.isEmpty()) {
            for (String w : r.warnings) msg.append("WARNING: " + w + "\n");
        } else {
            msg.append("Generated: " + r.title + "\n");
        }
    }

    private void exportCSV() {
        if (current == null) { JOptionPane.showMessageDialog(this, "Generate a report first."); return; }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String saved = Exporter.exportCSV(current, fc.getSelectedFile().getAbsolutePath());
        JOptionPane.showMessageDialog(this, "Saved: " + saved);
    }

    private void exportTXT() {
        if (current == null) { JOptionPane.showMessageDialog(this, "Generate a report first."); return; }
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String saved = Exporter.exportTXT(current, fc.getSelectedFile().getAbsolutePath());
        JOptionPane.showMessageDialog(this, "Saved: " + saved);
    }
}
