import java.sql.*;
import java.util.List;

public class ScheduleReportBuilder implements ReportBuilder {

    @Override
    public ReportData build() {
        ReportData r = new ReportData();
        r.title = "Schedule Report";
        r.columns = List.of("Session Type", "Date", "Time", "Venue", "Student", "Title", "Evaluator");

        String sql =
            "SELECT se.session_type, se.date, se.time, se.venue, " +
            "stu.username AS student_name, sub.title, eva.username AS evaluator_name " +
            "FROM sessions se " +
            "LEFT JOIN assignments a ON a.session_id = se.session_id " +
            "LEFT JOIN users stu ON stu.user_id = a.student_id " +
            "LEFT JOIN submissions sub ON sub.student_id = a.student_id " +
            "LEFT JOIN users eva ON eva.user_id = a.evaluator_id " +
            "ORDER BY se.date, se.time";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean any = false;
            while (rs.next()) {
                any = true;
                r.rows.add(List.of(
                        safe(rs.getString("session_type")),
                        safe(rs.getString("date")),
                        safe(rs.getString("time")),
                        safe(rs.getString("venue")),
                        safe(rs.getString("student_name")),
                        safe(rs.getString("title")),
                        safe(rs.getString("evaluator_name"))
                ));
            }
            if (!any) r.warnings.add("No schedule data found.");
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
