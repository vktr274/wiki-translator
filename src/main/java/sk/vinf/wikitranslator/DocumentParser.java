package sk.vinf.wikitranslator;

import java.util.concurrent.TimeoutException;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;

/**
 * Class for article parsing from JSON files that were created by the WikiExtractor tool.
 */
public class DocumentParser {
    /**
     * createDocsSpark initiates a connection to a local Apache Spark cluster which has to have
     * 1 master node and at least 1 worker node running. Then it defines Datasets
     * of Slovak, Czech and Hungarian JSONs of articles and a Dataset of the CSV file in
     * sk-cs-hu-spark. Executing the method results in 3 document directories:
     * documents-sk-spark, documents-cs-spark and documents-hu-spark. Each directory
     * contains several JSON files of created documents with 3 fields: id, title and text.
     * @throws StreamingQueryException
     * @throws TimeoutException
     */
    public static void createDocsSpark() throws StreamingQueryException, TimeoutException {
        var sparkConf = new SparkConf()
            .setAppName("WikiTranslator")
            .setMaster("spark://localhost:7077");
        SparkSession sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
        sparkSession.sparkContext().setLogLevel("ERROR");

        StructType jsonSchema = new StructType()
            .add("id", "string")
            .add("revid", "string")
            .add("url", "string")
            .add("title", "string")
            .add("text", "string");

        StructType csvSchema = new StructType()
            .add("sk_id", "string")
            .add("cs_id", "string")
            .add("hu_id", "string");
        
        Dataset<Row> jsonSk = sparkSession
            .read()
            .option("recursiveFileLookup","true")
            .schema(jsonSchema)
            .json("dataset/sk-articles/output");

        Dataset<Row> jsonCs = sparkSession
            .read()
            .option("recursiveFileLookup","true")
            .schema(jsonSchema)
            .json("dataset/cs-articles/output");

        Dataset<Row> jsonHu = sparkSession
            .read()
            .option("recursiveFileLookup","true")
            .schema(jsonSchema)
            .json("dataset/hu-articles/output");

        Dataset<Row> langIds = sparkSession
            .read()
            .option("header", "true")
            .schema(csvSchema)
            .csv("sk-cs-hu-spark");

        /* 
         * The next 3 left anti joins are done because the Wikipedia dump does
         * not contain some articles in each language with IDs in the CSV file
         * in sk-cs-hu-spark directory. Each left anti join finds these articles
         * and excludes then from the loaded Dataset of the mentioned CSV file. 
         * 
        */
        var toRemoveSk = langIds.join(
            jsonSk,
            jsonSk.col("id").equalTo(langIds.col("sk_id")),
            "left_anti"
        );
        langIds = langIds.except(toRemoveSk);

        var toRemoveCs = langIds.join(
            jsonCs,
            jsonCs.col("id").equalTo(langIds.col("cs_id")),
            "left_anti"
        );
        langIds = langIds.except(toRemoveCs);

        var toRemoveHu = langIds.join(
            jsonHu,
            jsonHu.col("id").equalTo(langIds.col("hu_id")),
            "left_anti"
        );
        langIds = langIds.except(toRemoveHu);

        /*
         * The next 3 joins are done to extract only articles that have their
         * ID in the loaded Dataset of the CSV file from sk-cs-hu-spark.
         */
        jsonSk = jsonSk.join(
            langIds,
            jsonSk.col("id").equalTo(langIds.col("sk_id"))
        ).select(
            jsonSk.col("id"),
            jsonSk.col("title"),
            jsonSk.col("text")
        );

        jsonCs = jsonCs.join(
            langIds,
            jsonCs.col("id").equalTo(langIds.col("cs_id"))
        ).select(
            jsonCs.col("id"),
            jsonCs.col("title"),
            jsonCs.col("text")
        );

        jsonHu = jsonHu.join(
            langIds,
            jsonHu.col("id").equalTo(langIds.col("hu_id"))
        ).select(
            jsonHu.col("id"),
            jsonHu.col("title"),
            jsonHu.col("text")
        );

        /*
         * Extracted articles are stored in directories for each language.
         * Using count() adds execution time so the lines are commented out.
         */
        jsonSk.write().json("documents-sk-spark");
        //System.out.println("Saved " + jsonSk.count() + " SK documents");

        jsonCs.write().json("documents-cs-spark");
        //System.out.println("Saved " + jsonCs.count() + " CS documents");

        jsonHu.write().json("documents-hu-spark");
        //System.out.println("Saved " + jsonHu.count() + " HU documents");

        sparkSession.close();
    }
}
