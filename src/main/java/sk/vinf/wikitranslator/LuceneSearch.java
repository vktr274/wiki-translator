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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.index.DirectoryReader;
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
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Class for searching using Apache Lucene.
 */
public class LuceneSearch {
    private final HashMap<String, List<String>> translationMapSk;
    private final HashMap<String, List<String>> translationMapCs;
    private final HashMap<String, List<String>> translationMapHu;

    LuceneSearch() throws JsonIOException, JsonSyntaxException, IOException {
        var gson = new Gson();
        var type = new TypeToken<HashMap<String, List<String>>>(){}.getType();

        translationMapSk = gson.fromJson(Files.newBufferedReader(Path.of("sk-map.json")), type);
        translationMapCs = gson.fromJson(Files.newBufferedReader(Path.of("cs-map.json")), type);
        translationMapHu = gson.fromJson(Files.newBufferedReader(Path.of("hu-map.json")), type);
    }

    /**
     * checkInput checks the syntax validity of the user input.
     * @param input user input
     * @return boolean true if valid else false
     */
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

    /**
     * start starts the translator search loop.
     * A successful search results in a JSON file output.json.
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws NullPointerException
     * @throws ParseException
     */
    public void start() throws IOException, IllegalArgumentException, NullPointerException, ParseException {
        var scanner = new Scanner(System.in, "Cp852");
        var gson = new Gson();

        while (true) {
            System.out.println("Enter query");
            var input = scanner.nextLine();
            if (input.toLowerCase().equals("exit")) {
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

    /**
     * search uses the index created by Apache Lucene. It first searches for documents
     * in the language input by the user. After that it uses translationMapSk or
     * translationMapCs or translationMapHu to find IDs of translations of the documents
     * in the input language and finally it searches for these translations by IDs.
     * Output of this search is the constructed search result.
     * @param inputQuery
     * @return constructed search result
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws ParseException
     * @throws NullPointerException
     */
    private ArrayList<HashMap<String, HashMap<String, String>>> search(String inputQuery) throws
    IllegalArgumentException, IOException, ParseException, NullPointerException {
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
            throw new NullPointerException("Invalid language. Analyzer is null.");
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
        var topDocs = isearcher.search(boolQuery, 10);

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
            
            List<String> translationIds = new ArrayList<>();
            if (lang.equals("sk")) {
                translationIds = translationMapSk.get(hitId);
            } else if (lang.equals("cs")) {
                translationIds = translationMapCs.get(hitId);
            } else if (lang.equals("hu")) {
                translationIds = translationMapHu.get(hitId);
            }
            
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
}
