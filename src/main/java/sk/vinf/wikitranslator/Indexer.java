package sk.vinf.wikitranslator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

// v1 - Custom indexing class
@Deprecated
public class Indexer {
    private final CSVParser parser;
    private final Gson gson;
    public final static String delim = " \t\n\r\f._-,/()=+\"#?!\\%*,;~@&$€@§{}[]<>:^0123456789";

    Indexer() throws IOException {
        parser = CSVParser.parse(
            Files.newBufferedReader(Path.of("documents.csv")),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );

        gson = new Gson();
    }

    public void createForwardIndex() throws IOException {
        HashMap<String, ArrayList<String>> mapForwardSk = new HashMap<>();
        HashMap<String, ArrayList<String>> mapForwardCs = new HashMap<>();
        HashMap<String, ArrayList<String>> mapForwardHu = new HashMap<>();

        var fwSkForward = new FileWriter(new File("sk-forward-index.json"));
        var fwCsForward = new FileWriter(new File("cs-forward-index.json"));
        var fwHuForward = new FileWriter(new File("hu-forward-index.json"));

        for (var record : parser) {
            var id = record.get("id");
            var sk = record.get("sk");
            var cs = record.get("cs");
            var hu = record.get("hu");

            var tokenizerSk = new StringTokenizer(sk, delim);
            var tokenizerCs = new StringTokenizer(cs, delim);
            var tokenizerHu = new StringTokenizer(hu, delim);

            var skList = new ArrayList<String>();
            var csList = new ArrayList<String>();
            var huList = new ArrayList<String>();

            while (tokenizerSk.hasMoreTokens()) {
                var token = tokenizerSk.nextToken().toLowerCase().replace("'", "");
                if (token.length() > 1) {
                    token = StringUtils.stripAccents(token);
                    skList.add(token);
                }
            }
            while (tokenizerCs.hasMoreTokens()) {
                var token = tokenizerCs.nextToken().toLowerCase().replace("'", "");
                if (token.length() > 1) {
                    token = StringUtils.stripAccents(token);
                    csList.add(token);
                }
            }
            while (tokenizerHu.hasMoreTokens()) {
                var token = tokenizerHu.nextToken().toLowerCase().replace("'", "");
                if (token.length() > 1 && !Set.of("ös","os", "es", "as").contains(token)) {
                    token = StringUtils.stripAccents(token);
                    huList.add(token);
                }
            }

            mapForwardSk.put(id, skList);
            mapForwardCs.put(id, csList);
            mapForwardHu.put(id, huList);
        }
        fwSkForward.write(gson.toJson(mapForwardSk));
        fwCsForward.write(gson.toJson(mapForwardCs));
        fwHuForward.write(gson.toJson(mapForwardHu));

        fwSkForward.close();
        fwCsForward.close();
        fwHuForward.close();
    }

    public void createInvertedIndex() throws IOException {
        HashMap<String, HashSet<String>> mapInvertedSk = new HashMap<>();
        HashMap<String, HashSet<String>> mapInvertedCs = new HashMap<>();
        HashMap<String, HashSet<String>> mapInvertedHu = new HashMap<>();

        var fwSkInverted = new FileWriter(new File("sk-inverted-index.json"));
        var fwCsInverted = new FileWriter(new File("cs-inverted-index.json"));
        var fwHuInverted = new FileWriter(new File("hu-inverted-index.json"));

        for (var record : parser) {
            var id = record.get("id");
            var sk = record.get("sk");
            var cs = record.get("cs");
            var hu = record.get("hu");

            var tokenizerSk = new StringTokenizer(sk, delim);
            var tokenizerCs = new StringTokenizer(cs, delim);
            var tokenizerHu = new StringTokenizer(hu, delim);

            while (tokenizerSk.hasMoreTokens()) {
                var token = tokenizerSk.nextToken().toLowerCase().replace("'", "");
                if (token.length() > 1) {
                    token = StringUtils.stripAccents(token);
                    if (mapInvertedSk.containsKey(token)) {
                        mapInvertedSk.get(token).add(id);
                    } else {
                        mapInvertedSk.put(token, new HashSet<>(List.of(id)));
                    }
                }
            }
            while (tokenizerCs.hasMoreTokens()) {
                var token = tokenizerCs.nextToken().toLowerCase().replace("'", "");
                if (token.length() > 1) {
                    token = StringUtils.stripAccents(token);
                    if (mapInvertedCs.containsKey(token)) {
                        mapInvertedCs.get(token).add(id);
                    } else {
                        mapInvertedCs.put(token, new HashSet<>(List.of(id)));
                    }
                }
            }
            while (tokenizerHu.hasMoreTokens()) {
                var token = tokenizerHu.nextToken().toLowerCase().replace("'", "");
                if (token.length() > 1 && !Set.of("ös","os", "es", "as").contains(token)) {
                    token = StringUtils.stripAccents(token);
                    if (mapInvertedHu.containsKey(token)) {
                        mapInvertedHu.get(token).add(id);
                    } else {
                        mapInvertedHu.put(token, new HashSet<>(List.of(id)));
                    }
                }
            }
        }
        
        fwSkInverted.write(gson.toJson(mapInvertedSk));
        fwCsInverted.write(gson.toJson(mapInvertedCs));
        fwHuInverted.write(gson.toJson(mapInvertedHu));

        fwSkInverted.close();
        fwCsInverted.close();
        fwHuInverted.close();
    }
}
