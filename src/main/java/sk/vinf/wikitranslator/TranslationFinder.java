package sk.vinf.wikitranslator;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;

import io.github.cdimascio.dotenv.Dotenv;

public class TranslationFinder {
    public static void find(String lang) throws SQLException, IOException {
        var dotenv = Dotenv.load();
        
        var user = dotenv.get("USER");
        var pw = dotenv.get("PW");

        var connSk = DriverManager.getConnection(
            "jdbc:mysql://localhost/wiki_sk" +
            "?user=" + user +
            "&password=" + pw
        );
        var connLang = DriverManager.getConnection(
            "jdbc:mysql://localhost/wiki_" + lang + 
            "?user=" + user + 
            "&password=" + pw
        );
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
            var langRes = psLang.executeQuery();
            if (!langRes.next()) {
                System.out.println("No page_id");
                continue;
            }
            var from = res.getInt("ll_from");
            var to = langRes.getInt("page_id");
            psLang.close();
            System.out.println(from + " - " + to);
            printer.printRecord(from, to);
        }
        printer.close();
        connSk.close();
        connLang.close();
        ps.close();
    }

    public static void conjunctionSpark() throws StreamingQueryException, TimeoutException, IOException {
        var sparkConf = new SparkConf().setAppName("WikiTranslator").setMaster("spark://localhost:7077");
        SparkSession sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
        sparkSession.sparkContext().setLogLevel("ERROR");

        StructType csvSchemaSkCs = new StructType()
            .add("sk_id", "string")
            .add("cs_id", "string");

        StructType csvSchemaSkHu = new StructType()
            .add("sk_id", "string")
            .add("hu_id", "string");

        Dataset<Row> csvSkCs = sparkSession
            .read()
            .option("header", "true") 
            .schema(csvSchemaSkCs)
            .csv("sk-cs.csv");

        Dataset<Row> csvSkHu = sparkSession
            .read()
            .option("header", "true") 
            .schema(csvSchemaSkHu)
            .csv("sk-hu.csv");
        
        Dataset<Row> joined = csvSkCs.join(
            csvSkHu,
            csvSkCs.col("sk_id").equalTo(csvSkHu.col("sk_id"))
        ).select(csvSkCs.col("sk_id"), csvSkCs.col("cs_id"), csvSkHu.col("hu_id"));
        joined.write().option("header", "true").csv("sk-cs-hu-spark");
    }
}