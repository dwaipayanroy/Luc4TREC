
package feedback;

import static common.trec.DocField.FIELD_BOW;
import static common.trec.DocField.FIELD_ID;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import common.trec.TRECQuery;
import common.trec.TRECQueryParser;
import java.util.ArrayList;
import org.apache.lucene.search.Query;
import searcher.Searcher;

/**
 *
 * @author dwaipayan
 */

public class CallRocchio extends Searcher {

    String          fieldForFeedback;   // field, to be used for feedback
    long            vocSize;            // vocabulary size
    Rocchio         rocchio;
    Boolean         feedbackFromFile;

    List<TRECQuery> queries;
    TRECQueryParser trecQueryparser;

    HashMap<String, TopDocs> allTopDocsFromFileHashMap;     // For feedback from file, to contain all topdocs from file

    // +++ TRF
    String qrelPath;
    String rf;    // P or T depending on Pseudo or True Relevance Feedback
    HashMap<String, List<Integer>> allRelDocsFromQrelHashMap;     // For TRF, to contain all true rel. docs.
    HashMap<String, List<Integer>> allNonRelDocsFromQrelHashMap;     // For TRF, to contain all true rel. docs.
    // --- TRF

    double alpha, beta, gamma, k1, b;

    float           mixingLambda;    // mixing weight, used for doc-col weight distribution
    int             numFeedbackTerms;// number of feedback terms
    int             numFeedbackDocs; // number of feedback documents
    float           QMIX;

    /**
     *
     * @param propPath
     * @throws IOException
     * @throws Exception
     */
    public CallRocchio(String propPath) throws IOException, Exception {

        super(propPath);

        /* constructing the query */
        fieldToSearch = prop.getProperty("fieldToSearch", FIELD_BOW);
        System.out.println("Searching field for retrieval: " + fieldToSearch);

        trecQueryparser = new TRECQueryParser(queryPath, analyzer);
        trecQueryparser.queryFileParse();
        queries = trecQueryparser.queries;

        // analyze the query
        for (TRECQuery query : queries) {
            query.fieldToSearch = fieldToSearch;
            query.luceneQuery = query.makeBooleanQuery(query.qtitle, fieldToSearch, analyzer);
            query.q_str = query.luceneQuery.toString(fieldToSearch);
        }
        /* constructed the query */

        fieldForFeedback = prop.getProperty("fieldForFeedback", FIELD_BOW);
        System.out.println("Field for Feedback: " + fieldForFeedback);

        rf = prop.getProperty("RF","P");        // default is PRF
        System.out.println("Doing " + rf + "RF.");
        // +++ TRF
        if(rf.toUpperCase().charAt(0) == 'T') {
            qrelPath = prop.getProperty("qrelPath");
            ArrayList<String> queryIds = new ArrayList<>();
            for (TRECQuery query : queries)
                queryIds.add(query.qid);
    
            allRelDocsFromQrelHashMap = new HashMap<>(); 
            allNonRelDocsFromQrelHashMap = new HashMap<>();

            common.CommonMethods.readJudgedDocsFromQrel(qrelPath, queryIds, indexReader, FIELD_ID, allRelDocsFromQrelHashMap, allNonRelDocsFromQrelHashMap);
        }
        // --- TRF

        // numFeedbackTerms = number of top terms to select for query expansion
        numFeedbackTerms = Integer.parseInt(prop.getProperty("numFeedbackTerms"));
        // numFeedbackDocs = number of top documents to select for feedback
        numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));

        // + taking Rocchio parameter as inputs
        alpha = Double.parseDouble(prop.getProperty("alpha", "1.0"));
        beta = Double.parseDouble(prop.getProperty("beta", "0.75"));
        gamma = Double.parseDouble(prop.getProperty("gamma", "0.10"));
        k1 = Double.parseDouble(prop.getProperty("k1", "1.2"));
        b = Double.parseDouble(prop.getProperty("b", "0.75"));

//        rocchio = new Rocchio(indexSearcher);
        rocchio = new Rocchio(indexSearcher, alpha, beta, gamma, k1, b);
        // + taking Rocchio parameter as inputs

        setRunName();
    }

    /**
     * Sets runName and resPath variables depending on similarity functions.
     */
    private void setRunName() throws IOException {

        Similarity s = indexSearcher.getSimilarity(true);
        runName = queryFile.getName()+"-"+s.toString()+"-D"+numFeedbackDocs+"-T"+numFeedbackTerms;
        runName += "-rocchio-"+alpha+"-"+beta+"-"+gamma+"-"+rf;
        runName += "-" + fieldToSearch + "-" + fieldForFeedback;
        runName = runName.replace(" ", "").replace("(", "").replace(")", "").replace("00000", "");

        setResFileName(runName);

    } // ends setResFileName()

    public static TopDocs search(IndexSearcher searcher, Query query, int numHits) throws IOException {

        return searcher.search(query, numHits);
    }

    public TopDocs retrieve(TRECQuery query, int numFeedbackDocs) throws Exception {

        System.out.println(query.qid+": \t" + query.luceneQuery.toString(fieldToSearch));

        return retrieve(query.luceneQuery, numFeedbackDocs);
    }
    
    public TopDocs retrieve(Query luceneQuery, int numHits) throws Exception {
        
        TopDocs topDocs;

        System.out.println(luceneQuery.toString());
        topDocs = search(luceneQuery, numHits);

        return topDocs;
    }

    public List<Integer> selectTopDocs (TopDocs topRetDocs, List<Integer> l2) {

        ScoreDoc[] sd = topRetDocs.scoreDocs;
        List<Integer> l1 = new ArrayList<>();

        for (int i = 0; i < sd.length; i++) {
            l1.add(i, sd[i].doc);
        }
        l1.retainAll(l2);

        return l1;
    }

    public void retrieveAll() throws Exception {

        TopDocs topRetDocs;             // top retrieved documents
        List<Integer> trueRelDocs;             // top relevant documents
        List<Integer> trueNonRelDocs = null;   // top non-relevant documents
        ScoreDoc[] hits;

        for (TRECQuery query : queries) {

//            if(Integer.parseInt(query.qid)==143)
            {
            // + Initial retrieval
            topRetDocs = retrieve(query, numFeedbackDocs);
            // - Initial retrieval
            switch(rf) {
                case "T":
                    // +++ TRF
                    System.out.println("TRF from qrel");
                    if(null == (trueRelDocs = allRelDocsFromQrelHashMap.get(query.qid))) {
                        System.err.println("No judged relevant documents found for Query id: "+query.qid);
                        continue;
                    }
                    trueNonRelDocs = allNonRelDocsFromQrelHashMap.get(query.qid);
                    // write a function select(topRetDocs, allNonRelDocs) that would return the intersection of the two
                    // allNonRelDocs = select(topRetDocs, allNonRelDocs) 
                    trueNonRelDocs = selectTopDocs(topRetDocs, trueNonRelDocs);
                    numFeedbackDocs = topRetDocs.scoreDocs.length;
                    // --- TRF
                    break;
                case "P":
                default:
                    // +++ PRF
                    //topRelDocs = topRetDocs;
                    trueRelDocs = new ArrayList<>();
                    ScoreDoc[] sd = topRetDocs.scoreDocs;
                    for(ScoreDoc s : sd) {
                        trueRelDocs.add(s.doc);
                    }
                    // --- PRF
                    break;
            }

            System.out.println(query.qid + ": retrieved documents available for feedback: " + topRetDocs.totalHits);
            if(topRetDocs.totalHits == 0)
                System.out.println("");

            else {
                // + expanded retrieval
                Query expanded_query = rocchio.expandQuery(trueRelDocs, trueNonRelDocs, query, numFeedbackDocs, numFeedbackTerms);
                topRetDocs = retrieve(expanded_query, numHits);
                // - expanded retrieval

                hits = topRetDocs.scoreDocs;
                if(hits == null)
                    System.out.println(query.qid + ": expanded retrieval: documents retrieve: " + 0);

                else {
                    System.out.println(query.qid + ": expanded retrieval: documents retrieve: " +hits.length);
                    StringBuffer resBuffer = makeTRECResFile(query.qid, hits, indexSearcher, runName, FIELD_ID);
                    resFileWriter.write(resBuffer.toString());
                }
            }
            }
        } // ends for each query
        resFileWriter.close();
    } // ends retrieveAll

    public static void main(String[] args) throws IOException, Exception {

        String usage = "java RelevanceBasedLanguageModel <properties-file>\n"
                + " 1. Path of the index."
                + " 2. Path of the query.xml file."
                + " 3. Path of the res file."
                + " 4. Number of expansion documents"
                + " 5. Number of expansion terms"
                + " 6. Field to be used for Search"
                + " 7. Field to be used for Feedback"
                + " 8. ALPHA:"
                + " 9. BETA:"
                + "10. GAMMA:"
                + "11. T / P: TRF or PRF";

        if(1 != args.length) {
            System.out.println("Usage: " + usage);
            // for debugging
            args = new String[1];
            args[0] = "trec3.xml-rocchio.D-10.T-40.S-content.F-1.2.properties";
            System.exit(1);
        }
        CallRocchio rblm = new CallRocchio(args[0]);

        rblm.retrieveAll();
    } // ends main()

}
