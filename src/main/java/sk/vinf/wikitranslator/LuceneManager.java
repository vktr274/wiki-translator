package sk.vinf.wikitranslator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        while (true) {
            System.out.println("Enter query");
            var input = scanner.nextLine();
            if (input.equals("exit")) {
                break;
            }
            search(input);
        }

        scanner.close();
    }

    private void search(String inputQuery) throws IllegalArgumentException, IOException, ParseException, NullPointerException {
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
        
        var out = new OutputStreamWriter(new FileOutputStream(new File("output.txt")), StandardCharsets.UTF_8);
        out.write("ID Language Title\n");
        out.write("---------------------------------------------\n");

        for (var scoreDoc : topDocs.scoreDocs) {
            var hit = isearcher.doc(scoreDoc.doc);
            var idFound = hit.get("id");
            var translationIds = idMapping.get(idFound);
            var hitText = hit.get("text");

            out.write(idFound + " " + lang.toUpperCase() + " \"" + hit.get("title") + "\"\n");
            out.write(hitText.substring(0, hitText.length() >= 256 ? 256 : hitText.length()) + "\n");

            for (var i = 0; i < translationIds.size(); i++) {
                var queryId = new TermQuery(new Term("id", translationIds.get(i)));
                var directoryLang = FSDirectory.open(Path.of("index", langs[i]));
                var ireaderLang = DirectoryReader.open(directoryLang);
                var isearcherLang = new IndexSearcher(ireaderLang);
                var searchLang = isearcherLang.search(queryId, 1);
                var hitLang = isearcherLang.doc(searchLang.scoreDocs[0].doc);
                
                out.write(
                    hitLang.get("id") + " " +
                    langs[i].toUpperCase() + " \"" +
                    hitLang.get("title") + "\"\n"
                );

                hitText = hitLang.get("text");
                out.write(hitText.substring(0, hitText.length() >= 256 ? 256 : hitText.length()) + "\n");
            }

            out.write("---------------------------------------------\n");
        }

        out.close();
        analyzer.close();
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
