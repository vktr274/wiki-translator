package sk.vinf.wikitranslator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
/* import java.util.Iterator; */
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class DocumentManager {
    private final CSVParser parser;
    private final Gson gson;
    
    DocumentManager() throws IOException {
        parser = CSVParser.parse(
            Files.newBufferedReader(Path.of("sk-cs-hu.csv")),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );
        gson = new Gson();
    }

    public void close() throws IOException {
        parser.close();
    }

    public static boolean isInt(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private ArrayList<HashMap<String, String>> parseJson(Path path) {
        var res = new ArrayList<HashMap<String, String>>();
        var type = new TypeToken<HashMap<String, String>>(){}.getType();

        try {
            var reader = Files.newBufferedReader(path);
            var line = "";
            while ((line = reader.readLine()) != null) {
                HashMap<String, String> map = gson.fromJson(line, type);
                res.add(map);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    private void createOne(CSVPrinter printer, Stream<HashMap<String, String>> stream, List<CSVRecord> records, String lang) throws IOException {
        var i = 0;
        var it = stream.iterator();

        System.out.println("Started " + lang);

        while (it.hasNext()) {
            var article = it.next();
            for (var record : records) {
                if (article.get("id").equals(record.get(lang + "_id"))) {
                    i++;
                    printer.printRecord(article.get("id"), article.get("title"), article.get("text"));
                    if (i % 10000 == 0) {
                        System.out.println("Extracted " + i + " " + lang + " articles");
                    }
                    break;
                }
            }
        }
        System.out.println("Extracted " + i + " " + lang + " articles");
    }

    private Stream<HashMap<String, String>> getStream(String uri) throws IOException {
        return Files
            .walk(Paths.get(uri))
            .filter(Files::isRegularFile)
            .map(this::parseJson)
            .flatMap(List::stream)
            .parallel();
    }

    private List<CSVRecord> ignoreDuplicates(List<CSVRecord> records) {
        var result = new ArrayList<CSVRecord>();
        var testSetSk = new HashSet<String>();
        var testSetCs = new HashSet<String>();
        var testSetHu = new HashSet<String>();

        for (var record : records) {
            var idSk = record.get("sk_id");
            var idCs = record.get("cs_id");
            var idHu = record.get("hu_id");

            if (testSetSk.add(idSk) && testSetCs.add(idCs) && testSetHu.add(idHu)) {
                result.add(record);
            }
        }

        return result;
    }

    public void createDocs() throws IOException, NumberFormatException {
        var printerSk = new CSVPrinter(new FileWriter(new File("documents-sk.csv")), CSVFormat.DEFAULT);
        var printerCs = new CSVPrinter(new FileWriter(new File("documents-cs.csv")), CSVFormat.DEFAULT);
        var printerHu = new CSVPrinter(new FileWriter(new File("documents-hu.csv")), CSVFormat.DEFAULT);

        printerSk.printRecord("id", "sk_title", "sk_text");
        printerCs.printRecord("id", "cs_title", "cs_text");
        printerHu.printRecord("id", "hu_title", "hu_text");
        
        Stream<HashMap<String, String>> skArticles = getStream("dataset/sk-articles/output");
        Stream<HashMap<String, String>> csArticles = getStream("dataset/cs-articles/output");
        Stream<HashMap<String, String>> huArticles = getStream("dataset/hu-articles/output");

        var records = ignoreDuplicates(parser.getRecords());

        // get Slovak articles first
        createOne(printerSk, skArticles, records, "sk");
        printerSk.close();
        skArticles.close();

        // get Czech articles
        createOne(printerCs, csArticles, records, "cs");
        printerCs.close();
        csArticles.close();

        // get Hungarian articles
        createOne(printerHu, huArticles, records, "hu");
        printerHu.close();
        huArticles.close();
    }

    // Method for cleaning documents in v1
    @Deprecated
    public void clean() throws IOException {
        var printer = new CSVPrinter(new FileWriter("documents.csv"), CSVFormat.DEFAULT);
        var index = 0;
        var removed = 0;
        var id = 1;

        printer.printRecord("id", "sk", "cs", "hu");

        for (var record : parser) {
            var titleSk = record.get("sk_title")
                .replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .replace('\u02bb', '\'')
                .replace('\u201e', '\"')
                .replace('\u201c', '\"')
                .replace('\u201d', '\"')
                .replace('\u2013', '-')
                .replace("\u2026", "...");
            var titleCs = record.get("cs_title")
                .replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .replace('\u02bb', '\'')
                .replace('\u201e', '\"')
                .replace('\u201c', '\"')
                .replace('\u201d', '\"')
                .replace('\u2013', '-')
                .replace("\u2026", "...");
            var titleHu = record.get("hu_title")
                .replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .replace('\u02bb', '\'')
                .replace('\u201e', '\"')
                .replace('\u201c', '\"')
                .replace('\u201d', '\"')
                .replace('\u2013', '-')
                .replace("\u2026", "...");

            index++;
            var titleSkLower = titleSk.toLowerCase();

            if (isInt(titleSk) || isInt(titleCs) || isInt(titleHu)) {
                System.out.println("Removed at idx " + (index - 1));
                removed++;
                continue;
            } else if (titleSk.charAt(0) == '.' || titleCs.charAt(0) == '.' || titleHu.charAt(0) == '.') {
                System.out.println("Removed at idx " + (index - 1));
                removed++;
                continue;
            } else if (titleSk.length() == 1 || titleCs.length() == 1 || titleHu.length() == 1) {
                System.out.println("Removed at idx " + (index - 1));
                removed++;
                continue;
            } else if (titleSkLower.equals(titleCs.toLowerCase()) && titleSkLower.equals(titleHu.toLowerCase())) {
                removed++;
                System.out.println("Removed at idx " + (index - 1));
                continue;
            }

            printer.printRecord(id, titleSk, titleCs, titleHu);
            id++;
        }
        
        System.out.println("Removed " + removed + " records");
        printer.close();
    }
}
