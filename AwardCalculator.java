import java.sql.*;

// Simple data holder for award result information
class AwardResult {
    public int submitId;          // submission ID
    public String studentId;      // student user ID
    public String studentName;    // student name
    public String submissionTitle;// submission title
    public String type;           // Oral / Poster
    public double total;          // average total score
}

public class AwardCalculator {

    // Compute Best Oral / Best Poster based on submission type
    public static AwardResult computeBestByType(String submissionType) {

        // Use AVG(e.total) to support multiple evaluators fairly
        // If only one evaluator exists, AVG = evaluator's total score
        String sql =
            "SELECT s.submit_id, u.user_id AS student_id, u.username AS student_name, s.title, s.type, " +
            "       AVG(e.total) AS avg_total " +  // calculate average score per submission
            "FROM submissions s " +
            "JOIN users u ON s.student_id = u.user_id " +
            "JOIN evaluations e ON e.submit_id = s.submit_id " +
            "WHERE s.type = ? " +                  // filter by Oral or Poster
            "GROUP BY s.submit_id " +
            "ORDER BY avg_total DESC " +           // highest average score wins
            "LIMIT 1;";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, submissionType);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                AwardResult r = new AwardResult();
                r.submitId = rs.getInt("submit_id");
                r.studentId = rs.getString("student_id");
                r.studentName = rs.getString("student_name");
                r.submissionTitle = rs.getString("title");
                r.type = rs.getString("type");
                r.total = rs.getDouble("avg_total"); // final average score
                return r;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Compute People's Choice award
    public static AwardResult computePeoplesChoice() {

        // People's Choice is defined as the submission with the highest
        // overall average evaluation score (Best Overall),
        // since no audience voting module is implemented
        String sql =
            "SELECT s.submit_id, u.user_id AS student_id, u.username AS student_name, s.title, s.type, " +
            "       AVG(e.total) AS avg_total " +   // average score across all evaluators
            "FROM submissions s " +
            "JOIN users u ON s.student_id = u.user_id " +
            "JOIN evaluations e ON e.submit_id = s.submit_id " +
            "GROUP BY s.submit_id " +
            "ORDER BY avg_total DESC " +            // highest average score wins
            "LIMIT 1;";

        try (Connection conn = DatabaseHandler.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                AwardResult r = new AwardResult();
                r.submitId = rs.getInt("submit_id");
                r.studentId = rs.getString("student_id");
                r.studentName = rs.getString("student_name");
                r.submissionTitle = rs.getString("title");
                r.type = rs.getString("type");
                r.total = rs.getDouble("avg_total"); // final average score
                return r;
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    // Save award results into awards table
    public static void saveAwards(AwardResult bestOral, AwardResult bestPoster, AwardResult peoples) {

        // Each award type is stored once by deleting old record and inserting new one
        upsertAward("BEST_ORAL", bestOral);
        upsertAward("BEST_POSTER", bestPoster);
        upsertAward("PEOPLES_CHOICE", peoples);
    }

    // Helper method to overwrite award result by award type
    private static void upsertAward(String awardType, AwardResult r) {
        if (r == null) return; // no data to save

        String delete = "DELETE FROM awards WHERE award_type = ?";
        String insert = "INSERT INTO awards(award_type, submit_id, total) VALUES(?,?,?)";

        try (Connection conn = DatabaseHandler.connect()) {

            // Remove existing award of the same type
            try (PreparedStatement ps = conn.prepareStatement(delete)) {
                ps.setString(1, awardType);
                ps.executeUpdate();
            }

            // Insert new award result
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setString(1, awardType);
                ps.setInt(2, r.submitId);
                ps.setDouble(3, r.total); // store average score
                ps.executeUpdate();
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
