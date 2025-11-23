import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CombineCompanyData {

    private static final String INPUT_FOLDER = "data/companies";
    private static final String OUTPUT_FILE = "data/combined_companies.csv";

    public static void main(String[] args) {
        try {
            Path inputPath = Paths.get(INPUT_FOLDER);
            if (!Files.exists(inputPath)) {
                Files.createDirectories(inputPath);
                System.out.println("Created input folder: " + inputPath.toAbsolutePath());
                System.out.println("Please add your company CSV files and re-run.");
                return;
            }

            List<File> csvFiles = listCsvFiles(INPUT_FOLDER);
            if (csvFiles.isEmpty()) {
                System.out.println("No CSV files found in folder: " + INPUT_FOLDER);
                return;
            }

            // Collect a global union set of headers preserving order
            LinkedHashSet<String> globalHeaders = new LinkedHashSet<>();
            List<Map<String, String>> allRows = new ArrayList<>();

            for (File file : csvFiles) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String headerLine = br.readLine();
                    if (headerLine == null) continue;
                    String[] headers = headerLine.split(",", -1);
                    for (int i = 0; i < headers.length; i++) {
                        headers[i] = headers[i].trim();
                    }

                    // Add file headers to global set
                    globalHeaders.addAll(Arrays.asList(headers));

                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] cols = line.split(",", -1);
                        Map<String, String> rowMap = new LinkedHashMap<>();
                        for (int i = 0; i < headers.length; i++) {
                            String value = (i < cols.length) ? cols[i].trim() : "";
                            rowMap.put(headers[i], value);
                        }

                        if (isRowValid(rowMap)) {
                            allRows.add(rowMap);
                        }
                    }
                }
            }

            // Write combined CSV with all global headers and all rows filled accordingly
            writeCombinedCsv(OUTPUT_FILE, globalHeaders, allRows);
            System.out.println("Combined CSV written to: " + OUTPUT_FILE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<File> listCsvFiles(String folderPath) throws IOException {
        List<File> files = new ArrayList<>();
        Files.list(Paths.get(folderPath))
             .filter(p -> p.toString().toLowerCase().endsWith(".csv"))
             .forEach(p -> files.add(p.toFile()));
        return files;
    }

    // Row is valid only if all cells are non-empty and not null or "null"
    private static boolean isRowValid(Map<String, String> row) {
        for (String val : row.values()) {
            if (val == null || val.isEmpty() || val.equalsIgnoreCase("null")) {
                return false; // reject row if any cell is null, empty or literal "null"
            }
        }
        return true;
    }

    private static void writeCombinedCsv(String outputPath, LinkedHashSet<String> headers, List<Map<String, String>> rows) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath))) {
            // Write global headers
            bw.write(String.join(",", headers));
            bw.newLine();

            // Write all rows consistent with headers order
            for (Map<String, String> row : rows) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    String val = row.get(header);
                    // Replace null or literal "null" by empty string for safety (though rows with nulls are skipped)
                    if (val == null || val.equalsIgnoreCase("null")) {
                        val = "";
                    }
                    values.add(val);
                }
                bw.write(String.join(",", values));
                bw.newLine();
            }
        }
    }
}
