/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searcher;

import common.EnglishAnalyzerWithSmartStopword;
import common.Luc4TRECQuery;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import org.apache.lucene.search.similarities.AfterEffect;
import org.apache.lucene.search.similarities.AfterEffectB;
import org.apache.lucene.search.similarities.AfterEffectL;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.BasicModel;
import org.apache.lucene.search.similarities.BasicModelBE;
import org.apache.lucene.search.similarities.BasicModelD;
import org.apache.lucene.search.similarities.BasicModelG;
import org.apache.lucene.search.similarities.BasicModelIF;
import org.apache.lucene.search.similarities.BasicModelIn;
import org.apache.lucene.search.similarities.BasicModelIne;
import org.apache.lucene.search.similarities.BasicModelP;
import org.apache.lucene.search.similarities.DFRSimilarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Normalization;
import org.apache.lucene.search.similarities.NormalizationH1;
import org.apache.lucene.search.similarities.NormalizationH2;
import org.apache.lucene.search.similarities.NormalizationH3;
import org.apache.lucene.search.similarities.NormalizationZ;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class Searcher {

    String          propPath;
    public Properties      prop;
    public IndexReader     indexReader;
    public IndexSearcher   indexSearcher;
    public String          indexPath;
    public File            indexFile;          // place where the index is stored
    public String          stopFilePath;
    public String          queryPath;      // path of the query file
    public File            queryFile;      // the query file
    public int             queryFieldFlag; // 1. title; 2. +desc, 3. +narr
    public String          []queryFields;  // to contain the fields of the query to be used for search
    public Analyzer        analyzer;           // the analyzer

    public String          runName;        // name of the run
    public int             numHits;        // number of document to retrieveWithExpansionTermsFromFile
    public boolean         boolIndexExists;// boolean flag to indicate whether the index exists or not
    public String          resPath;        // path of the res file
    public FileWriter      resFileWriter;  // the res file writer
    public String          fieldToSearch;      // the field in the index to be searched
    public int             simFuncChoice;
    public float           param1, param2, param3;

    private TopDocs topDocs;
    TopScoreDocCollector collector;

    public Searcher(String propPath) throws IOException {

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
        EnglishAnalyzerWithSmartStopword engAnalyzer;
        stopFilePath = prop.getProperty("stopFilePath");
        if (null == stopFilePath)
            engAnalyzer = new common.EnglishAnalyzerWithSmartStopword();
        else
            engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        //+++++ index path setting 
        indexPath = prop.getProperty("indexPath");
        System.out.println("indexPath set to: " + indexPath);
        indexFile = new File(indexPath);
        Directory indexDir = FSDirectory.open(indexFile.toPath());

        if (!DirectoryReader.indexExists(indexDir)) {
            System.err.println("Index doesn't exists in "+indexPath);
            boolIndexExists = false;
            System.exit(1);
        }
        fieldToSearch = prop.getProperty("fieldToSearch");
        //----- index path set

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        System.out.println("queryPath set to: " + queryPath);
        queryFile = new File(queryPath);
//        queryFieldFlag = Integer.parseInt(prop.getProperty("queryFieldFlag"));
//        queryFields = new String[queryFieldFlag-1];
        /* query path set */
        // TODO: queryFields unused

        simFuncChoice = Integer.parseInt(prop.getProperty("similarityFunction"));
        if (null != prop.getProperty("param1"))
            param1 = Float.parseFloat(prop.getProperty("param1"));
        if (null != prop.getProperty("param2"))
            param2 = Float.parseFloat(prop.getProperty("param2"));
        if (null != prop.getProperty("param3"))
            param3 = Float.parseFloat(prop.getProperty("param3"));

        /* setting indexReader and indexSearcher */
        indexReader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()));

        indexSearcher = new IndexSearcher(indexReader);
        setSimilarityFunction(simFuncChoice, param1, param2, param3);
        /* indexReader and searher set */

        /* res path set */
        numHits = Integer.parseInt(prop.getProperty("numHits", "1000"));

    }

    private void setSimilarityFunction(int choice, float param1, float param2, float param3) {

        switch(choice) {
            case 0:
                indexSearcher.setSimilarity(new DefaultSimilarity());
                System.out.println("Similarity function set to DefaultSimilarity");
                break;
            case 1:
                indexSearcher.setSimilarity(new BM25Similarity(param1, param2));
                System.out.println("Similarity function set to BM25Similarity"
                    + " with parameters: " + param1 + " " + param2);
                break;
            case 2:
                indexSearcher.setSimilarity(new LMJelinekMercerSimilarity(param1));
                System.out.println("Similarity function set to LMJelinekMercerSimilarity"
                    + " with parameter: " + param1);
                break;
            case 3:
                indexSearcher.setSimilarity(new LMDirichletSimilarity(param1));
                System.out.println("Similarity function set to LMDirichletSimilarity"
                    + " with parameter: " + param1);
                break;
            case 4:
//                indexSearcher.setSimilarity(new DFRSimilarity(new BasicModelIF(), new AfterEffectB(), new NormalizationH2()));
                BasicModel bm;
                AfterEffect ae;
                Normalization nor;
                switch((int)param1){
                    case 1:
                        bm = new BasicModelBE();
                        break;
                    case 2:
                        bm = new BasicModelD();
                        break;
                    case 3:
                        bm = new BasicModelG();
                        break;
                    case 4:
                        bm = new BasicModelIF();
                        break;
                    case 5:
                        bm = new BasicModelIn();
                        break;
                    case 6:
                        bm = new BasicModelIne();
                        break;
                    case 7:
                        bm = new BasicModelP();
                        break;
                    default:
                        bm = new BasicModelIF();
                        break;
                }
                switch ((int)param2){
                    case 1:
                        ae = new AfterEffectB();
                        break;
                    case 2:
                        ae = new AfterEffectL();
                        break;
                    default:
                        ae = new AfterEffectB();
                        break;
                }
                switch ((int)param3) {
                    case 1:
                        nor = new NormalizationH1();
                        break;
                    case 2:
                        nor = new NormalizationH2();
                        break;
                    case 3:
                        nor = new NormalizationH3();
                        break;
                    case 4:
                        nor = new NormalizationZ();
                        break;
                    case 5:
                        nor = new Normalization.NoNormalization();
                        break;
                    default:
                        nor = new NormalizationH2();
                        break;
                }
//                bm = new BasicModelIF();
                indexSearcher.setSimilarity(new DFRSimilarity(bm, ae, nor));
                System.out.println("Similarity function set to DFRSimilarity with default parameters");
                break;
        }
    }

    public void setResFileName(String runName) throws IOException {

        if(null == prop.getProperty("resPath"))
            resPath = "/home/dwaipayan/";
        else
            resPath = prop.getProperty("resPath");
        if(!resPath.endsWith("/"))
            resPath = resPath+"/";
        resPath = resPath+runName + ".res";

        File fl = new File(resPath);
        //if file exists, delete it
        if(fl.exists())
            System.out.println(fl.delete());

        resFileWriter = new FileWriter(resPath, true);
        System.out.println("Result will be stored in: "+resPath);

    }

    public TopDocs search(Luc4TRECQuery query, String fieldToSearch, int numHits) throws IOException {

        return search(query.luceneQuery, numHits);
    }

    public TopDocs search(Query query, int numHits) throws IOException {

        topDocs = null;
        collector = TopScoreDocCollector.create(numHits);

        indexSearcher.search(query, collector);
        topDocs = collector.topDocs();

        return topDocs;
    }

    public TopDocs search(Luc4TRECQuery query) throws IOException {

        return search(query.luceneQuery, numHits);
    }

    /**
     * Returns a string-buffer in the TREC-res format for the passed queryId
     * @param queryId
     * @param hits
     * @param searcher
     * @param runName
     * @param fieldDocid
     * @return
     * @throws IOException 
     */
    static final public StringBuffer makeTRECResFile(String queryId, ScoreDoc[] hits, 
        IndexSearcher searcher, String runName, String fieldDocid) throws IOException {

        StringBuffer resBuffer = new StringBuffer();
        int hits_length = hits.length;
        for (int i = 0; i < hits_length; ++i) {
            int luceneDocId = hits[i].doc;
            Document d = searcher.doc(luceneDocId);
            resBuffer.append(queryId).append("\tQ0\t").
                append(d.get(fieldDocid)).append("\t").
                append((i)).append("\t").
                append(hits[i].score).append("\t").
                append(runName).append("\n");                
        }

        return resBuffer;
    }

}
