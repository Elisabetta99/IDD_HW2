package lucenex;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilterFactory;
import org.apache.lucene.analysis.en.EnglishPossessiveFilterFactory;
import org.apache.lucene.analysis.miscellaneous.HyphenatedWordsFilterFactory;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.TrimFilterFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.suggest.analyzing.SuggestStopFilterFactory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Test;

public class Homework1 {
	private ArrayList<File> queue;
	private IndexWriter writer;
	private String docPath;
	
	@Before 
	public void setup() {
		queue = new ArrayList<File>();
		docPath = "target/docs";
	}

	private void addFiles(File file) {
		if (!file.exists()) {
			System.out.println(file + "Does not exist");
		}
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				addFiles(f);
			}
		} else {
			String filename = file.getName().toLowerCase();
			/* Only index text files */
			if (filename.endsWith(".txt")) {
				queue.add(file);
			} else {
				System.out.println("Skipped: " + filename);
			}
		}
	}



	private void indexDocs(String fileName, Directory directory, Codec codec) throws IOException {
		/* Content analyzer */
		Analyzer contentAnalyzer = CustomAnalyzer.builder()
				.withTokenizer(WhitespaceTokenizerFactory.class)
				.addTokenFilter(HyphenatedWordsFilterFactory.class)
				.addTokenFilter(EnglishPossessiveFilterFactory.class)
				.addTokenFilter(WordDelimiterGraphFilterFactory.class)
				.addTokenFilter(LowerCaseFilterFactory.class)
				.addTokenFilter(EnglishMinimalStemFilterFactory.class)
				.addTokenFilter(SuggestStopFilterFactory.class)
				.build();

		Map<String,String> params=new HashMap<>();
		params.put("pattern", ".txt");
		params.put("replacement", " ");

		/* Title analyzer */
		Analyzer titleAnalyzer = CustomAnalyzer.builder()
				.addCharFilter(PatternReplaceCharFilterFactory.class, params)
				.withTokenizer(WhitespaceTokenizerFactory.class)
				.addTokenFilter(TrimFilterFactory.class)
				.addTokenFilter(LowerCaseFilterFactory.class)
				.addTokenFilter(WordDelimiterGraphFilterFactory.class)
				.build();

		/* Define different processing options on a per Fields basis */
		Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
		perFieldAnalyzers.put("titolo", titleAnalyzer);
		perFieldAnalyzers.put("contenuto", contentAnalyzer);
		
		Analyzer analyzer = new PerFieldAnalyzerWrapper(contentAnalyzer, perFieldAnalyzers);
		
		/* Text processing semantics for indexing Fields */
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		if (codec != null) {
			config.setCodec(codec);
		}
		writer = new IndexWriter(directory, config);
		writer.deleteAll();

		addFiles(new File(fileName));      
		for (File f : queue) {
			FileReader fr = null;
			try {
				Document doc = new Document();
				fr = new FileReader(f);
				doc.add(new TextField("contenuto", fr));
				doc.add(new TextField("titolo", f.getName(), Field.Store.YES));
				writer.addDocument(doc);
			} catch (Exception e) {
				System.out.println("Impossibile aggiungere al writer: " + f);
			} finally {
				fr.close();
			}
		}

		queue.clear();

		writer.commit();
		writer.close();
	}

	
}
