package sk.vinf.wikitranslator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class LuceneIndexer {
    private CSVParser getParser(String uri) throws IOException {
        return CSVParser.parse(
            Files.newBufferedReader(Path.of(uri)),
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        );
    }

    public void indexLanguage(String lang) throws IOException, NullPointerException {
        Directory directory = FSDirectory.open(Path.of("index", lang));

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
            var title = record.get(lang + "_title");
            var text = record.get(lang + "_text");

            doc.add(new Field("title", title, TextField.TYPE_STORED));
            doc.add(new Field("text", text, TextField.TYPE_STORED));

            iwriter.addDocument(doc);
        }       
        iwriter.close();
    }
}
