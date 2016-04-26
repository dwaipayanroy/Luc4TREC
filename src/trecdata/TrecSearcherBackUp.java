/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trecdata;

import common.TRECQuery;
import common.TRECQueryParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class TrecSearcherBackUp {

    Properties      prop;
    IndexReader     reader;
    IndexSearcher   searcher;
    String          indexPath;
    File            indexFile;
    String          queryPath;
    File            queryFile;      // the query file
    String          stopFilePath;
    Analyzer        analyzer;
    String          runName;
    int             numHits;
    boolean         boolIndexExists;
    String          resPath;        // path of the res file
    FileWriter      resFileWriter;  // the res file writer
    List<TRECQuery> queries;
    TRECQueryParser trecQueryparser;


    public TrecSearcherBackUp(String propPath) throws IOException, Exception {
        prop = new Properties();
        prop.load(new FileReader(propPath));

        /* index path setting */
        System.out.println("Using index at: "+prop.getProperty("indexPath"));
        indexPath = prop.getProperty("indexPath");

        /*
        File indexDir;
        indexDir = new File(indexPath);
        reader = DirectoryReader.open(FSDirectory.open(indexDir));
        */
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile);
        /* index path set */

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexFile.getAbsolutePath());
            System.out.println("Terminating");
            boolIndexExists = false;
            System.exit(1);
        }

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        queryFile = new File(queryPath);
        /* query path set */

        /* constructing the query */
        queries = constructQueries();
        /* constructed the query */

        /* setting reader and searcher */
        reader = DirectoryReader.open(FSDirectory.open(indexFile));

        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new LMJelinekMercerSimilarity(0.6f));

        // +++++ setting the analyzer with English Analyzer with Smart stopword list
        stopFilePath = prop.getProperty("stopFilePath");
        common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        trecQueryparser = new TRECQueryParser(queryPath, analyzer);

        setRunName_ResFileName();
        resFileWriter = new FileWriter(resPath);

        numHits = Integer.parseInt(prop.getProperty("numHits","1000"));
    }

    private void setRunName_ResFileName() {
        runName = queryFile.getName()+"-baseline";
        resPath = "/home/dwaipayan/"+runName + ".res";
        System.out.println("Result will be stored in: "+resPath);
    }

    private List<TRECQuery> constructQueries() throws Exception {

        queryPath = prop.getProperty("queryPath");
        TRECQueryParser parser = new TRECQueryParser(queryPath);
        parser.queryFileParse();
        return parser.queries;
    }    
    
    public void retrieveAll() throws Exception {

        ScoreDoc[] hits = null;
        TopDocs topDocs = null;


        for (TRECQuery query : queries) {

            TopScoreDocCollector collector = TopScoreDocCollector.create(numHits, true);
            Query luceneQuery = trecQueryparser.getAnalyzedQuery(query);

            System.out.println(query.qid+ ": " +luceneQuery.toString("content"));

            searcher.search(luceneQuery, collector);
            topDocs = collector.topDocs();
            hits = topDocs.scoreDocs;
            if(hits == null)
                System.out.println("Nothing found");

            System.out.println("Retrieved results for query: " + query.qid);
            StringBuffer resBuffer = new StringBuffer();
            int hits_length = hits.length;
            System.out.println("Retrieved Length: " + hits_length);
            for (int i = 0; i < hits_length; ++i) {
                int luceneDocId = hits[i].doc;
                Document d = searcher.doc(luceneDocId);
                resBuffer.append(query.qid).append("\tQ0\t").
                    append(d.get("docid")).append("\t").
                    append((i)).append("\t").
                    append(hits[i].score).append("\t").
                    append(runName).append("\n");                
            }
            resFileWriter.write(resBuffer.toString());
        }
        resFileWriter.close();
//        System.out.println("The result is saved in: "+resultsPath);
    }

    public static void main(String[] args) throws IOException, Exception {

        if(0 == args.length) {
            System.out.println("Usage: java Searcher <propfile-path>");
            args = new String[2];
            args[0] = "wt10g.search.properties";
        }

        TrecSearcherBackUp searcher = new TrecSearcherBackUp(args[0]);

        searcher.retrieveAll();
    }

}

