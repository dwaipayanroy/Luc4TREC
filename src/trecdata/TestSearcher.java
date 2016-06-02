/**
 * TODO: queryField[] is not used. 
 * It works on only the title of the query.
 */
package trecdata;

import common.CollectionStatistics;
import common.TRECQuery;
import common.TRECQueryParser;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */
public class TestSearcher {

    String          propPath;
    Properties      prop;
    IndexReader     indexReader;
    IndexSearcher   searcher;
    String          indexPath;
    File            indexFile;
    String          stopFilePath;
    String          queryPath;
    File            queryFile;      // the query file
    int             queryFieldFlag; // 1. title; 2. +desc, 3. +narr
    String          []queryFields;  // to contain the fields of the query to be used for search
    Analyzer        analyzer;
    String          runName;
    int             numHits;
    boolean         boolIndexExists;
    List<TRECQuery> queries;
    TRECQueryParser trecQueryparser;
    int             simFuncChoice;
    float           param1, param2;

    CollectionStatistics    collStat;

    public TestSearcher(String propPath) throws IOException, Exception {

        this.propPath = propPath;
        prop = new Properties();
        try {
            prop.load(new FileReader(propPath));
        } catch (IOException ex) {
            System.err.println("Error: Properties file missing in "+propPath);
            System.exit(1);
        }
        //----- Properties file loaded

        // +++++ setting the analyzer with English Analyzer with Smart stopword list
        stopFilePath = prop.getProperty("stopFilePath");
        common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        //+++++ index path setting 
        indexPath = prop.getProperty("indexPath");
        indexFile = new File(indexPath);
        Directory indexDir = FSDirectory.open(indexFile);

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexPath);
            boolIndexExists = false;
            System.exit(1);
        }
        //----- index path set

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        queryFile = new File(queryPath);
        queryFieldFlag = Integer.parseInt(prop.getProperty("queryFieldFlag"));
        queryFields = new String[queryFieldFlag-1];
        /* query path set */
        // TODO: queryFields unused

        /* constructing the query */
        trecQueryparser = new TRECQueryParser(queryPath, analyzer);
        queries = constructQueries();
        /* constructed the query */

        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        if (null != prop.getProperty("param1"))
            param1 = Float.parseFloat(prop.getProperty("param1"));
        if (null != prop.getProperty("param2"))
            param2 = Float.parseFloat(prop.getProperty("param2"));

        /* setting indexReader and searcher */
        indexReader = DirectoryReader.open(FSDirectory.open(indexFile));

        searcher = new IndexSearcher(indexReader);
        setSimilarityFunction(simFuncChoice, param1, param2);


        numHits = Integer.parseInt(prop.getProperty("numHits", "1000"));

        collStat = new CollectionStatistics(indexPath, "content");
        collStat.buildCollectionStat();
    }

    private void setSimilarityFunction(int choice, float param1, float param2) {

        switch(choice) {
            case 0:
                searcher.setSimilarity(new DefaultSimilarity());
                break;
            case 1:
                searcher.setSimilarity(new BM25Similarity(param1, param2));
                break;
            case 2:
                searcher.setSimilarity(new LMJelinekMercerSimilarity(param1));
                break;
            case 3:
                searcher.setSimilarity(new LMDirichletSimilarity(param1));
                break;
        }
    }

    private List<TRECQuery> constructQueries() throws Exception {

        trecQueryparser.queryFileParse();
        return trecQueryparser.queries;
    }

    public ScoreDoc[] retrieve(TRECQuery query) throws Exception {

        ScoreDoc[] hits = null;
        TopDocs topDocs = null;

        TopScoreDocCollector collector = TopScoreDocCollector.create(numHits, true);
        Query luceneQuery = trecQueryparser.getAnalyzedQuery(query);

        System.out.println(query.qid+ ": " +luceneQuery.toString("content"));

        searcher.search(luceneQuery, collector);
        topDocs = collector.topDocs();
        hits = topDocs.scoreDocs;
        if(hits == null)
            System.out.println("Nothing found");

        return hits;
    }

    public void retrieveAll() throws Exception {

        ScoreDoc[] hits = null;

        float MAXratio = 0;
        float MINratio = 0;
        String MAXterm="", MINterm="";
        String MAXdoc="", MINdoc="";


        for (int i = 0; i < collStat.getDocCount(); ++i) {
            int luceneDocId = i;
            System.out.println("Reading document with lucene docid: " +luceneDocId+": "+searcher.doc(luceneDocId).get("docid"));

            Terms terms = indexReader.getTermVector(luceneDocId, "content");
            if(null == terms) {
                System.err.println("Error: Term vectors not found");
//                System.exit(1);
                continue;
            }
            TermsEnum iterator = terms.iterator(null);
            BytesRef byteRef = null;

            long docSize = 0;
            long maxTf = 0;
            long minTf = 0;
            String localMaxTerm = "", localMinTerm="";
            while((byteRef = iterator.next()) != null) {
            //* for each word in the document
                String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
                long termFreq = iterator.totalTermFreq();    // tf of 't'
                docSize += termFreq;
                if(maxTf < termFreq) {
                    maxTf = termFreq;
                    localMaxTerm = term;
                }
                if(minTf == 0 || minTf > termFreq) {
                    minTf = termFreq;
                    localMinTerm = term;
                }
            }
            float max_tf_by_size = (float)maxTf/(float)docSize;
            float min_tf_by_size = (float)minTf/(float)docSize;

            if(MAXratio < max_tf_by_size) {
                MAXratio = max_tf_by_size;
                MAXterm = localMaxTerm;
                MAXdoc = searcher.doc(luceneDocId).get("docid");
            }
            if(MINratio == 0 || MINratio > min_tf_by_size) {
                MINratio = min_tf_by_size;
                    MINterm = localMinTerm;
                    MINdoc = searcher.doc(luceneDocId).get("docid");
            }
        }

        System.out.println("MAX: "+ MAXratio+"\tTerm: "+MAXterm+"\tDocName: "+MAXdoc);
        System.out.println("MIN: "+ MINratio+"\tTerm: "+MINterm+"\tDocName: "+MINdoc);
    }

    public static void main(String[] args) throws IOException, Exception {

        TestSearcher collSearcher = null;

        String usage = "java TrecSearcher <properties-file>\n"
            + "Properties file must contain:\n"
            + "1. indexPath: Path of the index\n"
            + "2. queryPath: path of the query file (in proper xml format)\n"
            + "3. queryFieldFlag: 1-title, 2-title+desc, 3-title+desc+narr\n"
            + "4. similarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity\n"
            + "5. param1: \n"
            + "6. [param2]: optional if using BM25";

        /* // uncomment this if wants to run from inside Netbeans IDE
        args = new String[1];
        args[0] = "trec678.search.properties";
        //*/

        if(0 == args.length) {
            System.out.println(usage);
            System.exit(1);
        }

        System.out.println("Using properties file: "+args[0]);
        collSearcher = new TestSearcher(args[0]);

        collSearcher.retrieveAll();
    }

}
