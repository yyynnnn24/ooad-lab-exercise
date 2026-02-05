import java.sql.*;
import java.util.List;

public class FinalEvalReportBuilder implements ReportBuilder {

    @Override
    public ReportData build() {
        ReportData r = new ReportData();
        r.title = "Final Evaluation Report";
        r.columns = List.of("Submit ID","Student","Title","Clarity","Methodology","Results","Presentation","Total","Comments");

        String sql =
            "SELECT e.submit_id, stu.username AS student_name, sub.title, " +
            "e.clarity, e.methodology, e.results, e.presentation, e.total, e.comments " +
            "FROM evaluations e " +
            "JOIN submissions sub ON sub.submit_id = e.submit_id " +
            "JOIN users stu ON stu.user_id = sub.student_id " +
            "ORDER BY e.total DESC";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean any = false;
            while (rs.next()) {
                any = true;
                r.rows.add(List.of(
                        safe(rs.getString("submit_id")),
                        safe(rs.getString("student_name")),
                        safe(rs.getString("title")),
                        safe(rs.getString("clarity")),
                        safe(rs.getString("methodology")),
                        safe(rs.getString("results")),
                        safe(rs.getString("presentation")),
                        safe(rs.getString("total")),
                        safe(rs.getString("comments"))
                ));
            }
            if (!any) r.warnings.add("No evaluation data found.");
        } catch (Exception e) {
            r.warnings.add("DB error: " + e.getMessage());
        }
        return r;
    }

    private String safe(String s) {
    if (s == null) {
        return "";
    } 
    else {
        return s;
    }
    }
}
