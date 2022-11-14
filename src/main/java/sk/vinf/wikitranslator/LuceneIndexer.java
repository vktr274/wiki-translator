package sk.vinf.wikitranslator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Field;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Class for indexing using Apache Lucene.
 */
public class LuceneIndexer {
    /**
     * getStream walks the dir directory and returns a stream of JSONs
     * of documents represented by HashMaps.
     * @param dir directory of the JSON files of documents to get indexed
     * @return Stream<HashMap<String, String>> stream of JSONs from the directory dir
     * @throws IOException
     * @throws JsonParseException
     * @throws JsonSyntaxException
     */
    private static Stream<HashMap<String, String>> getStream(String dir) throws IOException, JsonParseException, JsonSyntaxException {
        var type = new TypeToken<HashMap<String, String>>(){}.getType();
        var gson = new Gson();
        return Files
            .walk(Paths.get(dir))
            .filter(Files::isRegularFile)
            .filter(file -> file.getFileName().toString().endsWith(".json"))
            .map(file -> {
                List<HashMap<String, String>> json = new ArrayList<>();
                try {
                    var reader = Files.newBufferedReader(file);
                    var line = "";
                    while ((line = reader.readLine()) != null) {
                        HashMap<String, String> map = gson.fromJson(line, type);
                        json.add(map);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return json;
            })
            .flatMap(List::stream)
            .parallel();
    }

    /**
     * indexLanguage creates an index of documents in a language specified by lang
     * @param lang language of documents to be indexed
     * @throws IOException
     * @throws NullPointerException
     */
    public static void indexLanguage(String lang) throws IOException, NullPointerException {
        var directory = FSDirectory.open(Path.of("index", lang));

        Analyzer analyzer = null;
        Stream<HashMap<String, String>> stream = null;

        if (lang.equals("sk")) {
            // There is no SlovakAnalyzer, using CzechAnalyzer instead
            analyzer = new CzechAnalyzer();
            stream = getStream("documents-sk-spark");
        } else if (lang.equals("cs")) {
            analyzer = new CzechAnalyzer();
            stream = getStream("documents-cs-spark");
        } else if (lang.equals("hu")) {
            analyzer = new HungarianAnalyzer();
            stream = getStream("documents-hu-spark");
        } else {
            throw new NullPointerException("Analyzer|CSVParser is null");
        }

        var config = new IndexWriterConfig(analyzer);
        var iwriter = new IndexWriter(directory, config);

        var it = stream.iterator();

        while (it.hasNext()) {
            var record = it.next();
            var doc = new Document();

            var id = record.get("id");
            var title = record.get("title");
            var text = record.get("text");

            doc.add(new StringField("id", id, Field.Store.YES));
            doc.add(new TextField("title", title, Field.Store.YES));
            doc.add(new TextField("text", text, Field.Store.YES));

            iwriter.addDocument(doc);
        }
        iwriter.close();
        stream.close();
        analyzer.close();
    }
}
