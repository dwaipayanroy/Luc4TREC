/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class ClueWebSpamFiltering {

    public static final String FIELD_DOCID = "docid";
    public static final String FIELD_SPAMSCORE = "spam-score";

    IndexWriter indexWriter;
    File        indexFile;          // place where the index will be stored
    File        spamScoreFile;      //

    IndexReader     spamScoreIndexReader;
    IndexSearcher   spamScoreIndexSearcher;
    float           spamScoreThreshold;     // under which, the document will be treated as a spam
    TopScoreDocCollector collectorDocSearch;
    ScoreDoc[]      hits;
    TopDocs         topDocs;
    Query           queryDocid;



    /**
     * Called at the time of indexing the spam score.
     * @param indexPath
     * @param spamScorePath
     * @throws IOException 
     */
    public ClueWebSpamFiltering(String indexPath, String spamScorePath) throws IOException {

        indexFile = new File(indexPath);
        spamScoreFile = new File(spamScorePath);
        Directory indexDir = FSDirectory.open(indexFile.toPath());
        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            System.exit(0);
        }
        else {
            System.out.println("Creating the index in: " + indexFile.getAbsolutePath());

            IndexWriterConfig iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            indexWriter = new IndexWriter(indexDir, iwcfg);
        }

    }

    /**
     * Called at the time of spam filtering, during the document indexing.
     * @param spamScoreIndexPath
     * @param spamScoreThreshold
     * @throws IOException 
     */
    public ClueWebSpamFiltering(String spamScoreIndexPath, float spamScoreThreshold) throws IOException {

        this.spamScoreThreshold = spamScoreThreshold;

        spamScoreIndexReader = DirectoryReader.open(FSDirectory.open((new File(spamScoreIndexPath)).toPath()));
        spamScoreIndexSearcher = new IndexSearcher(spamScoreIndexReader);
        spamScoreIndexSearcher.setSimilarity(new DefaultSimilarity());

    }

    /**
     * Depending on 'spamScoreThreshold', determines whether the document with docid is spam, or not;
     * Document having spamScore less-than spamScoreThreshold, is spam document.
     * @param docid
     * @return true / false depending on the spam status (spam / not-spam) of the document.
     */
    public boolean isSpam(String docid) throws IOException {

        boolean spamStatus = true;
        float spamScore;

        hits = null;
        topDocs = null;

        collectorDocSearch = TopScoreDocCollector.create(1);

        queryDocid = new TermQuery(new Term(FIELD_DOCID, docid));

        spamScoreIndexSearcher.search(queryDocid, collectorDocSearch);
        topDocs = collectorDocSearch.topDocs();
        hits = topDocs.scoreDocs;

        if(hits != null) {
            Document d = spamScoreIndexSearcher.doc(hits[0].doc);
            spamScore = Float.parseFloat(d.get(FIELD_SPAMSCORE));

            if (spamScore >= spamScoreThreshold)
                spamStatus = false;
            else
                System.err.println("Ignoring " + docid + " as spam with score: " + spamScore);
        }
        else
            System.err.println("Spam score not present for: "+docid);
        return spamStatus;

    }

    private void createIndex() throws FileNotFoundException, IOException {

        String docid;
        float spamScore;
        Document doc;

        FileInputStream fis = new FileInputStream(spamScoreFile);

	BufferedReader br = new BufferedReader(new InputStreamReader(fis));
 
	String line = null;
	while ((line = br.readLine()) != null) {
            System.out.println(line);
            doc = new Document();
            String tokens[] = line.split(" ");
            spamScore = Float.parseFloat(tokens[0]);
            docid = tokens[1];
            doc.add(new StringField(FIELD_DOCID, docid, Field.Store.YES));
            doc.add(new FloatField(FIELD_SPAMSCORE, spamScore, Field.Store.YES));
            indexWriter.addDocument(doc);
	}
        indexWriter.close();
 
	br.close();
    }
    
    public static void main(String[] args) throws IOException {

        String usage = "Usage: java ClueWebSpamFiltering"
                        + " [-index INDEX_PATH] [-spam SPAM-SCORE-FILE-PATH] [-percentile SPAM-SCORE]\n\n"
                        + "This indexes the spam-score-file in SPAM-SCORE-FILE-PATH, creating a Lucene index "
                        + "in INDEX_PATH.";

        if(args.length == 0) {
            System.out.println(usage);
            System.exit(0);
        }

        String indexPath = null;
        String spamScorePath = null;
        float spamScore = -1;

        for (int i = 0; i < args.length; i++) {
            switch(args[i]){
                case "-index":
                    indexPath = args[i+1];
                    i++;
                    break;
                case "-spam":
                    spamScorePath = args[i+1];
                    i++;
                    break;
                case "-percentile":
                    spamScore = Float.parseFloat(args[i+1]);
                    i++;
                    break;
            }
        }
//        indexPath = "/home/dwaipayan/spamIndex";
//        spamScorePath = "/home/dwaipayan/spam-score-doc-list-sample-clueweb.txt";
//        spamScore = 70;

        ClueWebSpamFiltering spamScoreIndexer;

        if(spamScore == -1) {
        // index the spam score file
            spamScoreIndexer = new ClueWebSpamFiltering(indexPath, spamScorePath);
            spamScoreIndexer.createIndex();
        }

        // For unit testing
        if(false)
        {
            spamScoreIndexer = new ClueWebSpamFiltering(indexPath, spamScore);

            FileInputStream fis = new FileInputStream(new File(spamScorePath));

            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line = null;
            while ((line = br.readLine()) != null) {
                System.out.print(line + " ");
                String tokens[] = line.split(" ");
                System.out.println(spamScoreIndexer.isSpam(tokens[1]));
            }
            br.close();
        }

    }

}
