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

        var ps = connSk.prepareStatement(
        "SELECT ll_from, ll_title FROM wiki_sk.langlinks WHERE ll_lang = ?"
        );
        ps.setString(1, lang);
        var res = ps.executeQuery();

        var printer = new CSVPrinter(new FileWriter("sk-" + lang + ".csv"), CSVFormat.DEFAULT);
        printer.printRecord("sk_id", lang + "_id");

        while (res.next()) {
            var llTitle = res.getString("ll_title").split(":", 2);
            if (llTitle.length == 0) {
                System.out.println("Title 0");
                continue;
            }
            var title = llTitle[llTitle.length - 1].replace(" ", "_");
            if (title.isEmpty()) {
                System.out.println("Title missing");
                continue;
            }
            var psLang = connLang.prepareStatement("SELECT page_id FROM wiki_" + lang + ".page WHERE page_title = ?");
            psLang.setString(1, title);
            var resID = psLang.executeQuery();
            if (!resID.next()) {
                System.out.println("No page_id");
                continue;
            }
            var from = res.getInt("ll_from");
            var to = resID.getInt("page_id");
            psLang.close();
            System.out.println(from + " - " + to);
            printer.printRecord(from, to);
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
        printer.printRecord("sk_id", "cs_id", "hu_id");

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
                        var csId = recordSkCs.get("cs_id");
                        var huId = recordSkHu.get("hu_id");
                        printer.printRecord(
                            skId,
                            csId,
                            huId
                        );
                        System.out.println(skId + "-" + csId + "-" + huId);
                    }
                }
            }
        } else {
            for (var recordSkHu : skHuRecords) {
                for (var recordSkCs : skCsRecords) {
                    var skId = recordSkCs.get("sk_id");
                    if (skId.equals(recordSkHu.get("sk_id"))) {
                        var csId = recordSkCs.get("cs_id");
                        var huId = recordSkHu.get("hu_id");
                        printer.printRecord(
                            skId,
                            csId,
                            huId
                        );
                        System.out.println(skId + "-" + csId + "-" + huId);
                    }
                }
            }
        }
        printer.close();
    }
}