package sk.vinf.wikitranslator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

@Deprecated
public class ArticleParser {
    private BufferedReader bzIn = null;

    ArticleParser(Path path) {
        try {
            bzIn = new BufferedReader(
                new InputStreamReader(
                    new BZip2CompressorInputStream(
                        new BufferedInputStream(
                            Files.newInputStream(path)
                        )
                    )
                )
            );
            bzIn.mark(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean parse(int narticles, String lang) {
        var pageStartRegex = "^.*<page>.*$";
        var pageEndRegex = "^.*</page>.*$";
        var titleRegex = "^.*<title>(.*)</title>.*$";
        var idRegex = "^.*?<id>(\\d*)</id>.*$";

        var line = "";
        var title = "";
        var id = "";
        var page = "";

        var titlePattern = Pattern.compile(titleRegex, Pattern.DOTALL);
        var idPattern = Pattern.compile(idRegex, Pattern.DOTALL);

        var ret = true;

        try {
            var skCsHuParser = CSVParser.parse(
                Files.newBufferedReader(Path.of("sk-cs-hu.csv")),
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
            );
            var printer = new CSVPrinter(new FileWriter(lang + "-docs.csv"), CSVFormat.DEFAULT);

            if (lang.equals("sk")) {
                printer.printRecord("id", "cs_id", "hu_id", "title", "text");
            } else if (lang.equals("cs")) {
                printer.printRecord("id", "sk_id", "hu_id", "title", "text");
            } else if (lang.equals("hu")) {
                printer.printRecord("id", "sk_id", "cs_id", "title", "text");
            }

            var records = skCsHuParser.getRecords();
            if (narticles > 0) {
                records = records.subList(0, narticles);
            }
            records.sort((r1, r2) -> {
                return r1.get(lang +"_id").compareTo(r2.get(lang +"_id"));
            });

            var count = 0;
            
            while ((line = bzIn.readLine()) != null) {
                page = "";
                if (count == records.size()) {
                    break;
                }
                // Find page start
                if (line.matches(pageStartRegex)) {
                    page += line;
                    line = bzIn.readLine();
                    while (!line.matches(pageEndRegex)) {
                        page += line;
                        line = bzIn.readLine();
                    }
                    page += line;

                    var titleMatcher = titlePattern.matcher(page);
                    if (!titleMatcher.matches()) {
                        System.out.println("Title RegEx failed");
                        System.out.println(page);
                        ret = false;
                        break;
                    }
                    title = titleMatcher.group(1);

                    var idMatcher = idPattern.matcher(page);
                    if (!idMatcher.matches()) {
                        System.out.println("ID RegEx failed");
                        System.out.println(page);
                        ret = false;
                        break;
                    }
                    id = idMatcher.group(1);

                    var langId = records.get(count).get(lang + "_id");
                    if (!id.equals(langId)) {
                        continue;
                    }

                    if (lang.equals("sk")) {
                        printer.printRecord(
                            langId,
                            records.get(count).get("cs_id"),
                            records.get(count).get("hu_id"),
                            title
                        );
                    } else if (lang.equals("cs")) {
                        printer.printRecord(
                            langId,
                            records.get(count).get("sk_id"),
                            records.get(count).get("hu_id"),
                            title
                        );
                    } else if (lang.equals("hu")) {
                            printer.printRecord(langId,
                            records.get(count).get("sk_id"),
                            records.get(count).get("cs_id"),
                            title
                        );
                    }

                    count++;
                    System.out.println("No." + count + " ID: " + id + " - " + title);
                }
            }

            skCsHuParser.close();
            printer.close();
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        }
        return ret;
    }
}
