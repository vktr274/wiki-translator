package sk.vinf.wikitranslator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import com.google.gson.Gson;

public class Mapper {
    private final CSVParser parser;
    private final Gson gson;

    Mapper() throws IOException {
        parser = CSVParser.parse(
            Files.newBufferedReader(Path.of("sk-cs-hu.csv")),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );
        gson = new Gson();
    }

    public void close() throws IOException {
        parser.close();
    }

    public void mapLanguages() throws IOException {
        var fwSk = new FileWriter(new File("sk-map.json"));
        var fwCs = new FileWriter(new File("cs-map.json"));
        var fwHu = new FileWriter(new File("hu-map.json"));

        HashMap<String, List<String>> skMap = new HashMap<>();
        HashMap<String, List<String>> csMap = new HashMap<>();
        HashMap<String, List<String>> huMap = new HashMap<>();

        for (var record : parser) {
            var skId = record.get("sk_id");
            var csId = record.get("cs_id");
            var huId = record.get("hu_id");

            skMap.put(skId, List.of(csId, huId));
            csMap.put(csId, List.of(skId, huId));
            huMap.put(huId, List.of(skId, csId));
        }

        fwSk.write(gson.toJson(skMap));
        fwCs.write(gson.toJson(csMap));
        fwHu.write(gson.toJson(huMap));
    }
}
