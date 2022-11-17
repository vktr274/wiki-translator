package sk.vinf.wikitranslator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.gson.Gson;

/**
 * Class for mapping translation IDs
 */
public class TranslationMapper {
    /**
     * getStream walks the dir directory and returns a stream of CSV records.
     * @param dir directory of the joined CSV file (sk-cs-hu-spark is used in this project)
     * @return stream of CSVRecords from the directory dir
     * @throws IOException
     */
    private static Stream<CSVRecord> getStream(String dir) throws IOException {
        return Files
            .walk(Paths.get(dir))
            .filter(Files::isRegularFile)
            .filter(file -> file.getFileName().toString().endsWith(".csv"))
            .map(file -> {
                List<CSVRecord> csv = new ArrayList<>();
                try {
                    var parser = CSVParser.parse(
                        Files.newBufferedReader(file),
                        CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
                    );
                    csv = parser.getRecords();
                    parser.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return csv;
            })
            .flatMap(List::stream)
            .parallel();
    }

    /**
     * mapLanguages creates a mapping for each article ID in each language
     * to IDs of their translations. The output are 3 JSON files:
     * sk-map.json with this schema {"sk_id": [ "cs_id", "hu_id" ], ...}
     * cs-map.json with this schema {"cs_id": [ "sk_id", "hu_id" ], ...}
     * hu-map.json with this schema {"hu_id": [ "sk_id", "cs_id" ], ...}
     * @throws IOException
     */
    public static void mapLanguages() throws IOException {
        var gson = new Gson();

        var fwSk = new FileWriter(new File("sk-map.json"));
        var fwCs = new FileWriter(new File("cs-map.json"));
        var fwHu = new FileWriter(new File("hu-map.json"));

        HashMap<String, List<String>> skMap = new HashMap<>();
        HashMap<String, List<String>> csMap = new HashMap<>();
        HashMap<String, List<String>> huMap = new HashMap<>();

        var stream = getStream("sk-cs-hu-spark");
        var it = stream.iterator();

        while (it.hasNext()) {
            var record = it.next();

            var skId = record.get("sk_id");
            var csId = record.get("cs_id");
            var huId = record.get("hu_id");

            skMap.put(skId, List.of(csId, huId));
            csMap.put(csId, List.of(skId, huId));
            huMap.put(huId, List.of(skId, csId));
        }

        stream.close();

        fwSk.write(gson.toJson(skMap));
        fwCs.write(gson.toJson(csMap));
        fwHu.write(gson.toJson(huMap));

        fwSk.close();
        fwCs.close();
        fwHu.close();
    }
}
