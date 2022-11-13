package sk.vinf.wikitranslator;

import java.util.concurrent.TimeoutException;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.StructType;

public class DocumentParser {
    public static void createDocsSpark() throws StreamingQueryException, TimeoutException {
        var sparkConf = new SparkConf().setAppName("WikiTranslator").setMaster("spark://localhost:7077");
        var sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
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

        jsonSk.write().json("documents-sk-spark");
        jsonCs.write().json("documents-cs-spark");
        jsonHu.write().json("documents-hu-spark");

        sparkSession.close();
    }
}
