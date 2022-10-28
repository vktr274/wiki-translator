package sk.vinf.wikitranslator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.FSDirectory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class LuceneManager {
    private CSVParser getParser(String uri) throws IOException {
        return CSVParser.parse(
            Files.newBufferedReader(Path.of(uri)),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );
    }

    private HashMap<String, List<String>> getIdMapping(String lang) throws IOException {
        var gson = new Gson();
        var type = new TypeToken<HashMap<String, List<String>>>(){}.getType();
        var reader = Files.newBufferedReader(Path.of(lang + "-map.json"));

        return gson.fromJson(reader, type);
    }

    private boolean checkInput(String input) {
        var parts = input.split(":");
        if (
            parts.length < 3 ||
            !Set.of("sk", "cs", "hu").contains(parts[0]) ||
            !Set.of("T", "t", "T|t", "T&t", "t|T", "t&T").contains(parts[1])
        ) {
            return false;
        }
        return true;
    }

    public void start() throws IOException, IllegalArgumentException, NullPointerException, ParseException {
        var scanner = new Scanner(System.in, "Cp852");
        var gson = new Gson();

        while (true) {
            System.out.println("Enter query");
            var input = scanner.nextLine();
            if (input.equals("exit")) {
                break;
            }
            var result = new HashMap<String, ArrayList<HashMap<String, HashMap<String, String>>>>();
            var searchResult = search(input);
            result.put("searchResult", searchResult);
            var fw = new FileWriter(new File("output.json"), StandardCharsets.UTF_8);
            var json = gson.toJson(result);
            fw.write(json);
            fw.close();
        }

        scanner.close();
    }

    public ArrayList<HashMap<String, HashMap<String, String>>> search(String inputQuery) throws IllegalArgumentException, IOException, ParseException, NullPointerException {
        if (!checkInput(inputQuery)) {
            throw new IllegalArgumentException("Query should have a lang:at:qtext syntax");
        }
        var queryParts = inputQuery.split(":");
        var lang = queryParts[0];
        var at = queryParts[1];
        var qtext = queryParts[2];

        Analyzer analyzer = null;
        
        if (lang.equals("sk") || lang.equals("cs")) {
            analyzer = new CzechAnalyzer();
        } else if (lang.equals("hu")) {
            analyzer = new HungarianAnalyzer();
        } else {
            throw new NullPointerException("Analyzer is null");
        }

        var directory = FSDirectory.open(Path.of("index", lang));
        var ireader = DirectoryReader.open(directory);
        var isearcher = new IndexSearcher(ireader);

        var boolQueryBuilder = new BooleanQuery.Builder();

        var queryParserTitle = new QueryParser("title", analyzer);
        var queryParserText = new QueryParser("text", analyzer);

        Query queryTitle = null;
        Query queryText = null;
        BooleanQuery boolQuery = null;

        if (at.equals("T&t") || at.equals("t&T")) {
            queryTitle = queryParserTitle.parse(qtext);
            queryText = queryParserText.parse(qtext);

            boolQueryBuilder
                .add(queryTitle, Occur.MUST)
                .add(queryText, Occur.MUST);
        } else if (at.equals("T|t") || at.equals("t|T")) {
            queryTitle = queryParserTitle.parse(qtext);
            queryText = queryParserText.parse(qtext);
            
            boolQueryBuilder
                .add(queryTitle, Occur.SHOULD)
                .add(queryText, Occur.SHOULD);
        } else if (at.equals("T")) {
            queryTitle = queryParserTitle.parse(qtext);
            boolQueryBuilder.add(queryTitle, Occur.MUST);
        } else if (at.equals("t")) {
            queryText = queryParserText.parse(qtext);
            boolQueryBuilder.add(queryText, Occur.MUST);
        }

        boolQuery = boolQueryBuilder.build();
        var topDocs = isearcher.search(boolQuery, 5);
        var idMapping = getIdMapping(lang);

        var lang2 = "";
        var lang3 = "";

        if (lang.equals("sk")) {
            lang2 = "cs";
            lang3 = "hu";
        } else if (lang.equals("cs")) {
            lang2 = "sk";
            lang3 = "hu";
        } else if (lang.equals("hu")) {
            lang2 = "sk";
            lang3 = "cs";
        }

        String[] langs = { lang2, lang3 };
        
        ArrayList<HashMap<String, HashMap<String, String>>> searchResult = new ArrayList<>();
        var scoreDocs = topDocs.scoreDocs;

        System.out.println("ID Language Title");
        System.out.println("--------------------------------------");

        for (var i = 0; i < scoreDocs.length; i++) {
            var hit = isearcher.doc(scoreDocs[i].doc);
            var hitId = hit.get("id");
            var hitText = hit.get("text");
            var hitTitle = hit.get("title");
            var translationIds = idMapping.get(hitId);
            
            HashMap<String, HashMap<String, String>> map = new HashMap<>();
            HashMap<String, String> mapLang = new HashMap<>();

            System.out.println(hitId + " " + lang.toUpperCase() + " " + hitTitle);

            mapLang.put("id", hitId);
            mapLang.put("title", hitTitle);
            mapLang.put("text", hitText);
            map.put(lang, mapLang);
            
            for (var j = 0; j < translationIds.size(); j++) {
                mapLang = new HashMap<>();
                var queryId = new TermQuery(new Term("id", translationIds.get(j)));
                var directoryLang = FSDirectory.open(Path.of("index", langs[j]));
                var ireaderLang = DirectoryReader.open(directoryLang);
                var isearcherLang = new IndexSearcher(ireaderLang);
                var searchLang = isearcherLang.search(queryId, 1);
                hit = isearcherLang.doc(searchLang.scoreDocs[0].doc);

                hitId = hit.get("id");
                hitText = hit.get("text");
                hitTitle = hit.get("title");

                System.out.println(hitId + " " + langs[j].toUpperCase() + " " + hitTitle);

                mapLang.put("id", hitId);
                mapLang.put("title", hitTitle);
                mapLang.put("text", hitText);
                map.put(langs[j], mapLang);
            }
            searchResult.add(map);
            System.out.println("--------------------------------------");
        }

        analyzer.close();
        return searchResult;
    }

    public void indexLanguage(String lang) throws IOException, NullPointerException {
        var directory = FSDirectory.open(Path.of("index", lang));

        Analyzer analyzer = null;
        CSVParser parser = null;

        if (lang.equals("sk")) {
            analyzer = new CzechAnalyzer();
            parser = getParser("documents-sk.csv");
        } else if (lang.equals("cs")) {
            analyzer = new CzechAnalyzer();
            parser = getParser("documents-cs.csv");
        } else if (lang.equals("hu")) {
            analyzer = new HungarianAnalyzer();
            parser = getParser("documents-hu.csv");
        } else {
            throw new NullPointerException("Analyzer|CSVParser is null");
        }

        var config = new IndexWriterConfig(analyzer);
        var iwriter = new IndexWriter(directory, config);

        for (var record : parser) {
            var doc = new Document();

            var id = record.get("id");
            var title = record.get(lang + "_title");
            var text = record.get(lang + "_text");

            doc.add(new StringField("id", id, Field.Store.YES));
            doc.add(new TextField("title", title, Field.Store.YES));
            doc.add(new TextField("text", text, Field.Store.YES));

            iwriter.addDocument(doc);
        }
        iwriter.close();
        parser.close();
        analyzer.close();
    }
}
