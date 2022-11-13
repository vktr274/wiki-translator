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

public class TranslationMapper {
    private static Stream<CSVRecord> getStream(String uri) throws IOException {
        return Files
            .walk(Paths.get(uri))
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
