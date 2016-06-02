/**
 * TODO: queryField[] is not used. 
 * It works on only the title of the query.
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
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class CollectionSearcher {

    String          propPath;
    Properties      prop;
    IndexReader     reader;
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
    String          resPath;        // path of the res file
    FileWriter      resFileWriter;  // the res file writer
    List<TRECQuery> queries;
    TRECQueryParser trecQueryparser;
    int             simFuncChoice;
    float           param1, param2;

    public CollectionSearcher(String propPath) throws IOException, Exception {

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

        /* setting reader and searcher */
        reader = DirectoryReader.open(FSDirectory.open(indexFile));

        searcher = new IndexSearcher(reader);
        setSimilarityFunction(simFuncChoice, param1, param2);

        setRunName_ResFileName();

        File fl = new File(resPath);
        //if file exists, delete it
        if(fl.exists())
            System.out.println(fl.delete());

        resFileWriter = new FileWriter(resPath, true);

        /* res path set */
        numHits = Integer.parseInt(prop.getProperty("numHits", "1000"));

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

    private void setRunName_ResFileName() {

        runName = queryFile.getName()+searcher.getSimilarity().
            toString().replace(" ", "-").replace("(", "").replace(")", "");
        if(null == prop.getProperty("resPath"))
            resPath = "/home/dwaipayan/";
        else
            resPath = prop.getProperty("resPath");
        if(!resPath.endsWith("/"))
            resPath = resPath+"/";
        resPath = resPath+runName;
        System.out.println("Result will be stored in: "+resPath);
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


        for (TRECQuery query : queries) {

            hits = retrieve(query);
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
        System.out.println("The result is saved in: "+resPath);

    }

    public static void main(String[] args) throws IOException, Exception {

        CollectionSearcher collSearcher = null;

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
        collSearcher = new CollectionSearcher(args[0]);

        collSearcher.retrieveAll();
    }

}
