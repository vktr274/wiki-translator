package sk.vinf.wikitranslator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SearchEngine {
    private final Gson gson;
    private final List<CSVRecord> docs;
    private final HashMap<String, HashSet<String>> indexSk;
    private final HashMap<String, HashSet<String>> indexCs;
    private final HashMap<String, HashSet<String>> indexHu;

    SearchEngine() throws IOException {
        gson = new Gson();
        var parser = CSVParser.parse(
            Files.newBufferedReader(Path.of("documents.csv")),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );
        docs = parser.getRecords();
        parser.close();
        indexSk = loadIndex("sk");
        indexCs = loadIndex("cs");
        indexHu = loadIndex("hu");
    }

    private boolean checkInput(String input) {
        var parts = input.split(":");
        if (parts.length < 2 || !Set.of("sk", "cs", "hu").contains(parts[0])) {
            return false;
        }
        return true;
    }

    private ArrayList<String> tokenize(String input) {
        var tokenizer = new StringTokenizer(input, Indexer.delim);
        var res = new ArrayList<String>();
        while (tokenizer.hasMoreTokens()) {
            var token = tokenizer.nextToken().toLowerCase().replace("'", "");;
            if (!token.isEmpty()) {
                token = StringUtils.stripAccents(token);
                res.add(token);
            }
        }
        return res;
    }

    private void search(String lang, ArrayList<String> tokens) throws IOException, NumberFormatException {
        var result = new ArrayList<String>();

        for (var token : tokens) {
            if (lang.equals("sk") && indexSk.containsKey(token)) {
                result.addAll(indexSk.get(token));
            } else if (lang.equals("cs") && indexCs.containsKey(token)) {
                result.addAll(indexCs.get(token));
            } else if (lang.equals("hu") && indexHu.containsKey(token)) {
                result.addAll(indexHu.get(token));
            }
        }
        var counter = new HashMap<String, Integer>();
        for (var string : result) {
            counter.put(string, 1 + (counter.containsKey(string) ? counter.get(string) : 0));
        }

        var finalResult = new ArrayList<String>(counter.keySet());
        finalResult.sort((s1, s2) -> {
            return counter.get(s2) - counter.get(s1);
        });
        
        var lang1 = "";
        var lang2 = "";

        if (lang.equals("sk")) {
            lang1 = "cs";
            lang2 = "hu";
        } else if (lang.equals("cs")) {
            lang1 = "sk";
            lang2 = "hu";
        } else if (lang.equals("hu")) {
            lang1 = "sk";
            lang2 = "cs";
        }

        var count = 1;
        for (var id : finalResult) {
            var langOrig = docs.get(Integer.parseInt(id) - 1).get(lang);
            var lang1Res = docs.get(Integer.parseInt(id) - 1).get(lang1);
            var lang2Res = docs.get(Integer.parseInt(id) - 1).get(lang2);
            System.out.println(count + ". " + langOrig + " (" + lang + ") = " + lang1Res + " (" + lang1 + ") = " + lang2Res + " (" + lang2 + ")");
            count++;
        }
    }

    private HashMap<String, HashSet<String>> loadIndex(String lang) throws IOException {
        var reader = Files.newBufferedReader(Paths.get(lang + "-inverted-index.json"));
        var type = new TypeToken<HashMap<String, HashSet<String>>>(){}.getType();

        HashMap<String, HashSet<String>> json = gson.fromJson(reader, type);
        reader.close();
        return json;
    }

    public void start() throws IOException {
        var scanner = new Scanner(System.in, "Cp852");

        while (true) {
            System.out.println("Enter query");
            var input = scanner.nextLine();
            if (input.equals("exit")) {
                break;
            }
            if (checkInput(input)) {
                var splitInput = input.split(":");
                var lang = splitInput[0];
                var tokens = tokenize(splitInput[1]);
                search(lang, tokens);
            } else {
                System.out.println("Incorrect query");
            }
        }

        scanner.close();
    }
}
