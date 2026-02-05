import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Exporter {

    public static String exportCSV(ReportData r, String outPath) {

        if (!outPath.endsWith(".csv")) {
            outPath = outPath + ".csv";
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outPath));

            // Write column headers
            for (int i = 0; i < r.columns.size(); i++) {
                bw.write(r.columns.get(i));
                if (i < r.columns.size() - 1) bw.write(",");
            }
            bw.newLine();

            // Write rows
            for (List<String> row : r.rows) {
                for (int i = 0; i < row.size(); i++) {
                    bw.write(row.get(i));
                    if (i < row.size() - 1) bw.write(",");
                }
                bw.newLine();
            }

            bw.close();
            return outPath;

        } 
        catch (Exception e) {
            System.out.println("Export CSV failed");
            return null;
        }
    }


    public static String exportTXT(ReportData r, String outPath) {

        if (!outPath.endsWith(".txt")) {
            outPath = outPath + ".txt";
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outPath));

            bw.write(r.title);
            bw.newLine();
            bw.newLine();

            // Warnings
            for (String w : r.warnings) {
                bw.write("WARNING: " + w);
                bw.newLine();
            }

            bw.newLine();

            // Column headers
            for (String c : r.columns) {
                bw.write(c + " | ");
            }
            bw.newLine();

            // Rows
            for (List<String> row : r.rows) {
                for (String v : row) {
                    bw.write(v + " | ");
                }
                bw.newLine();
            }

            bw.close();
            return outPath;

        } 
        catch (Exception e) {
            System.out.println("Export TXT failed");
            return null;
        }
    }
}

