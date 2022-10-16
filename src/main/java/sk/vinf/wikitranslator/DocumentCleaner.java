package sk.vinf.wikitranslator;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

public class DocumentCleaner {
    private final CSVParser parser;
    
    DocumentCleaner() throws IOException {
        parser = CSVParser.parse(
            Files.newBufferedReader(Path.of("sk-cs-hu.csv")),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );
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
