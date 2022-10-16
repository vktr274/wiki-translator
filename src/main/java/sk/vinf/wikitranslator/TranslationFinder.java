package sk.vinf.wikitranslator;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

import io.github.cdimascio.dotenv.Dotenv;

public class TranslationFinder {
    private final Dotenv dotenv;
    private final String lang;
    private final Connection connSk;
    private final Connection connLang;

    TranslationFinder(String lang) throws SQLException {
        dotenv = Dotenv.load();
        this.lang = lang;
        var user = dotenv.get("USER");
        var pw = dotenv.get("PW");

        connSk = DriverManager.getConnection(
            "jdbc:mysql://localhost/wiki_sk" +
            "?user=" + user +
            "&password=" + pw
        );
        connLang = DriverManager.getConnection(
            "jdbc:mysql://localhost/wiki_" + lang + 
            "?user=" + user + 
            "&password=" + pw
        );
    }

    public void close() throws SQLException {
        connSk.close();
        connLang.close();
    }

    public void find() throws SQLException, IOException {
        if (connLang == null || connSk == null) {
            return;
        }

        // Find article IDs in Slovak and article titles in lang
        var ps = connSk.prepareStatement(
        "SELECT ll_from, ll_title FROM wiki_sk.langlinks WHERE ll_lang = ?"
        );
        ps.setString(1, lang);
        var res = ps.executeQuery();

        var printer = new CSVPrinter(new FileWriter("sk-" + lang + ".csv"), CSVFormat.DEFAULT);
        printer.printRecord("sk_id", "sk_title", lang + "_title");

        while (res.next()) {
            var llTitle = res.getString("ll_title").split(":");
            if (llTitle.length == 0) {
                System.out.println("Title 0");
                continue;
            }
            var langTitle = llTitle[llTitle.length - 1];
            if (langTitle.isEmpty()) {
                System.out.println(lang + " title missing");
                continue;
            }
            var fromId = res.getInt("ll_from");
            var psFrom = connSk.prepareStatement(
                "SELECT page_title FROM wiki_sk.page WHERE page_id = ?"
            );
            psFrom.setInt(1, fromId);
            var fromTitle = psFrom.executeQuery();
            if (!fromTitle.next()) {
                System.out.println("sk title missing");
                continue;
            }
            var fromTitleStr = fromTitle.getString("page_title").replace("_", " ");
            System.out.println("ID: " + fromId + " - " + fromTitleStr + " = " + langTitle);
            printer.printRecord(fromId, fromTitleStr, langTitle);
        }
        printer.close();
        connSk.close();
        connLang.close();
        ps.close();
    }

    public static void conjuction() throws IOException {
        var skCsCsv = Path.of("sk-cs.csv");
        var skHuCsv = Path.of("sk-hu.csv");

        var printer = new CSVPrinter(new FileWriter("sk-cs-hu.csv"), CSVFormat.DEFAULT);
        printer.printRecord("sk_title", "cs_title", "hu_title");

        var skCsParser = CSVParser.parse(
            skCsCsv,
            Charset.forName("UTF-8"),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );
        var skHuParser = CSVParser.parse(
            skHuCsv,
            Charset.forName("UTF-8"),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );

        var skCsRecords = skCsParser.getRecords();
        var skHuRecords = skHuParser.getRecords();
        skCsParser.close();
        skHuParser.close();

        if (skCsRecords.size() < skHuRecords.size()) {
            for (var recordSkCs : skCsRecords) {
                for (var recordSkHu : skHuRecords) {
                    var skId = recordSkCs.get("sk_id");
                    if (skId.equals(recordSkHu.get("sk_id"))) {
                        var skTitle = recordSkCs.get("sk_title");
                        var csTitle = recordSkCs.get("cs_title");
                        var huTitle = recordSkHu.get("hu_title");
                        printer.printRecord(
                            skTitle,
                            csTitle,
                            huTitle
                        );
                        System.out.println(skTitle + "-" + csTitle + "-" + huTitle);
                    }
                }
            }
        } else {
            for (var recordSkHu : skHuRecords) {
                for (var recordSkCs : skCsRecords) {
                    var skId = recordSkCs.get("sk_id");
                    if (skId.equals(recordSkHu.get("sk_id"))) {
                        var skTitle = recordSkCs.get("sk_title");
                        var csTitle = recordSkCs.get("cs_title");
                        var huTitle = recordSkHu.get("hu_title");
                        printer.printRecord(
                            skTitle,
                            csTitle,
                            huTitle
                        );
                        System.out.println(skTitle + "-" + csTitle + "-" + huTitle);
                    }
                }
            }
        }
        printer.close();
    }
}