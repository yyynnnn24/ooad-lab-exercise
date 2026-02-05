import java.sql.*;
import java.util.List;

public class AwardReportBuilder implements ReportBuilder {

    @Override
    public ReportData build() {
        ReportData r = new ReportData();
        r.title = "Award Agenda";
        r.columns = List.of("Award Type","Student","Title","Type","Score");

        String sql =
            "SELECT a.award_type, stu.username AS student_name, sub.title, sub.type, a.total " +
            "FROM awards a " +
            "JOIN submissions sub ON sub.submit_id = a.submit_id " +
            "JOIN users stu ON stu.user_id = sub.student_id " +
            "ORDER BY a.award_type";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean any = false;
            while (rs.next()) {
                any = true;
                r.rows.add(List.of(
                        safe(rs.getString("award_type")),
                        safe(rs.getString("student_name")),
                        safe(rs.getString("title")),
                        safe(rs.getString("type")),
                        safe(rs.getString("total"))
                ));
            }
            if (!any) r.warnings.add("No awards found (compute awards first).");
        } 
        catch (Exception e) {
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
