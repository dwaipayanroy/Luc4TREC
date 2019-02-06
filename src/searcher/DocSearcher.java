/**
 * TODO: queryField[] is not used. 
 * It works on only the title of the query.
 */
package searcher;

import static common.CommonVariables.FIELD_FULL_BOW;
import static common.CommonVariables.FIELD_ID;
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
import org.apache.lucene.search.similarities.Normalization.NoNormalization;
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
public class DocSearcher {

    String          propPath;
    Properties      prop;
    IndexReader     indexReader;
    IndexSearcher   indexSearcher;
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
    String          fieldToSearch;
    int             simFuncChoice;
    float           param1, param2, param3;

    public DocSearcher(String propPath) throws IOException, Exception {

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
        System.out.println("stopFilePath set to: " + stopFilePath);
        common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
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
        //----- index path set

        /* setting query path */
        queryPath = prop.getProperty("queryPath");
        System.out.println("queryPath set to: " + queryPath);
        queryFile = new File(queryPath);
        queryFieldFlag = Integer.parseInt(prop.getProperty("queryFieldFlag"));
        queryFields = new String[queryFieldFlag-1];
        /* query path set */
        // TODO: queryFields unused

        /* constructing the query */
        fieldToSearch = prop.getProperty("fieldToSearch", FIELD_FULL_BOW);
        System.out.println("Searching field for retrieval: " + fieldToSearch);
        trecQueryparser = new TRECQueryParser(queryPath, analyzer, fieldToSearch);
        queries = constructQueries();
        /* constructed the query */

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

        setRunName_ResFileName();

        File fl = new File(resPath);
        //if file exists, delete it
        if(fl.exists())
            System.out.println(fl.delete());

        resFileWriter = new FileWriter(resPath, true);

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
                        nor = new NoNormalization();
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

    private void setRunName_ResFileName() {

        runName = queryFile.getName()+"-"+fieldToSearch+"-"+indexSearcher.getSimilarity(true).
            toString().replace(" ", "-").replace("(", "").replace(")", "").replace("00000", "");
        if(null == prop.getProperty("resPath"))
            resPath = "/home/dwaipayan/";
        else
            resPath = prop.getProperty("resPath");
        if(!resPath.endsWith("/"))
            resPath = resPath+"/";
        resPath = resPath+runName + ".res";
        System.out.println("Result will be stored in: "+resPath);
    }

    private List<TRECQuery> constructQueries() throws Exception {

        trecQueryparser.queryFileParse();
        return trecQueryparser.queries;
    }

    public ScoreDoc[] retrieve(TRECQuery query) throws Exception {

        ScoreDoc[] hits;
        TopDocs topDocs;

        TopScoreDocCollector collector = TopScoreDocCollector.create(numHits);
        Query luceneQuery = trecQueryparser.getAnalyzedQuery(query, 1);

        System.out.println(query.qid+ ": " +luceneQuery.toString(fieldToSearch));

        indexSearcher.search(luceneQuery, collector);
        topDocs = collector.topDocs();
        hits = topDocs.scoreDocs;
        if(hits == null)
            System.out.println("Nothing found");

        return hits;
    }

    public void retrieveAll() throws Exception {

        ScoreDoc[] hits;


        for (TRECQuery query : queries) {

            hits = retrieve(query);
            int hits_length = hits.length;
            System.out.println(query.qid + ": documents retrieve: " +hits_length);
            StringBuffer resBuffer = new StringBuffer();

            for (int i = 0; i < hits_length; ++i) {
                int luceneDocId = hits[i].doc;
                Document d = indexSearcher.doc(luceneDocId);
                resBuffer.append(query.qid).append("\tQ0\t").
                    append(d.get(FIELD_ID)).append("\t").
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

        DocSearcher collSearcher;

        String usage = "java NewsDocSearcher <properties-file>\n"
            + "Properties file must contain:\n"
            + "1. indexPath: Path of the index\n"
            + "2. fieldToSearch: Name of the field to use for searching\n"
            + "3. queryPath: Path of the query file (in proper xml format)\n"
            + "4. queryFieldFlag: 1-title, 2-title+desc, 3-title+desc+narr\n"
            + "5. similarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity\n"
            + "6. param1: \n"
            + "7. [param2]: optional if using BM25";

        /* // uncomment this if wants to run from inside Netbeans IDE
        args = new String[1];
        args[0] = "searcher.properties";
        //*/

        if(0 == args.length) {
            System.out.println(usage);
            System.exit(1);
        }

        System.out.println("Using properties file: "+args[0]);
        collSearcher = new DocSearcher(args[0]);

        collSearcher.retrieveAll();
    }

}
