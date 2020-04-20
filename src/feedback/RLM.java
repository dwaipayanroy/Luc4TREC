/*
 */
package feedback;

import common.DocumentVector;
import common.Luc4TRECQuery;
import common.PerTermStat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

/**
 *
 * @author dwaipayan
 */
public class RLM {

    IndexReader     indexReader;
    IndexSearcher   indexSearcher;

    String          fieldToSearch;      // the field of the index which will be used for both retrievals
    String          fieldForFeedback;   // the field of the index which will be used for feedback
    Analyzer        analyzer;
    
    int             numFeedbackTerms;// number of feedback terms
    int             numFeedbackDocs; // number of feedback documents
    float           mixingLambda;    // mixing weight, used for doc-col weight adjustment
    float           QMIX;           // query mixing parameter; to be used for RM3, RM4 (not done)
    int             numHits;        //

    /**
     * Hashmap of Vectors of all feedback documents, keyed by luceneDocId (Integer).
     */
    HashMap<Integer, DocumentVector>    feedbackDocumentVectors;

    /**
     * HashMap of PerTermStat of all feedback terms, keyed by the term (String).
     */
    HashMap<String, PerTermStat>        feedbackTermStats;

    /**
     * HashMap of P(Q|D) (Float) for all feedback documents, keyed by luceneDocId (Integer).
     */
    HashMap<Integer, Float> hash_P_Q_Given_D;

    long            vocSize;        // vocabulary size
    long            docCount;       // number of documents in the collection

    /**
     * List, for sorting the words in non-increasing order of probability.
     */
    List<WordProbability> list_PwGivenR;

    /**
     * HashMap of P(w|R) for 'numFeedbackTerms' terms with top P(w|R) among each w in R,
     * keyed by the term (String) with P(w|R) as the value (WordProbability).
     */
    HashMap<String, WordProbability> hashmap_PwGivenR;

    /**
     * HashMap<DocId, DocumentVector> to contain all topdocs for reranking.
     * Only used if reranking, reading top docs from file.
     */
    HashMap<String, DocumentVector> topDocsDV = new HashMap<>();

    public RLM(IndexReader indexReader, IndexSearcher indexSearcher,
            Analyzer analyzer, 
            String fieldToSearch, String fieldForFeedback, 
            int numFeedbackDocs, int numFeedbackTerms,
            float mixingLambda, float QMIX) throws IOException {

        this.indexReader = indexReader;
        this.indexSearcher = indexSearcher;
        this.analyzer = analyzer;
        this.fieldToSearch = fieldToSearch;
        this.fieldForFeedback = fieldForFeedback;
        this.numFeedbackDocs = numFeedbackDocs;
        this.numFeedbackTerms = numFeedbackTerms;
        this.mixingLambda = mixingLambda;
        this.QMIX = QMIX;
        vocSize = getVocabularySize();          // total number of terms in the vocabulary
        docCount = indexReader.maxDoc();        // total number of documents in the index
        numHits = 1000;
    }

    /**
     * Sets the following variables with feedback statistics: to be used consequently.<p>
     * {@link #feedbackDocumentVectors},<p> 
     * {@link #feedbackTermStats}, <p>
     * {@link #hash_P_Q_Given_D}
     * @param topDocs
     * @param analyzedQuery
     * @throws IOException 
     */
    public void setFeedbackStats(TopDocs topDocs, String[] analyzedQuery) throws IOException {

        feedbackDocumentVectors = new HashMap<>();
        feedbackTermStats = new HashMap<>();
        hash_P_Q_Given_D = new HashMap<>();

        ScoreDoc[] hits;
        int hits_length;
        hits = topDocs.scoreDocs;
        hits_length = hits.length;               // number of documents retrieved in the first retrieval

        for (int i = 0; i < Math.min(numFeedbackDocs, hits_length); i++) {
            // for each feedback document
            int luceneDocId = hits[i].doc;
            Document d = indexSearcher.doc(luceneDocId);
            DocumentVector docV = new DocumentVector(fieldForFeedback);
            docV = docV.getDocumentVector(luceneDocId, indexReader);
            if(docV == null)
                continue;
            feedbackDocumentVectors.put(luceneDocId, docV);                // the document vector is added in the list

            for (Map.Entry<String, PerTermStat> entrySet : docV.docPerTermStat.entrySet()) {
            // for each term of that feedback document
                String key = entrySet.getKey();
                PerTermStat value = entrySet.getValue();

                if(null == feedbackTermStats.get(key)) {
                // this feedback term is not already put in the hashmap, hence needed to be put;
                    Term termInstance = new Term(fieldForFeedback, key);
                    long cf = indexReader.totalTermFreq(termInstance); // CF: Returns the total number of occurrences of term across all documents (the sum of the freq() for each doc that has this term).
                    long df = indexReader.docFreq(termInstance);       // DF: Returns the number of documents containing the term

                    feedbackTermStats.put(key, new PerTermStat(key, cf, df));
                }
            } // ends for each term of that feedback document
        } // ends for each feedback document

        // Calculating P(Q|d) for each feedback documents
        for (Map.Entry<Integer, DocumentVector> entrySet : feedbackDocumentVectors.entrySet()) {
            // for each feedback document
            int luceneDocId = entrySet.getKey();
            DocumentVector docV = entrySet.getValue();

            float p_Q_GivenD = 1;
            for (String qTerm : analyzedQuery)
                p_Q_GivenD *= return_Smoothed_MLE(qTerm, docV);
            if(null == hash_P_Q_Given_D.get(luceneDocId))
                hash_P_Q_Given_D.put(luceneDocId, p_Q_GivenD);
            else {
                System.err.println("Error while pre-calculating P(Q|d). "
                + "For luceneDocId: " + luceneDocId + ", P(Q|d) already existed.");
            }
        }

    }

    /**
     * mixingLambda*tf(t,d)/d-size + (1-mixingLambda)*cf(t)/col-size
     * @param t The term under consideration
     * @param dv The document vector under consideration
     * @return MLE of t in a document dv, smoothed with collection statistics
     * @throws java.io.IOException
     */
    public float return_Smoothed_MLE(String t, DocumentVector dv) throws IOException {

        float smoothedMLEofTerm = 1;
        PerTermStat docPTS;

        docPTS = dv.docPerTermStat.get(t);
        PerTermStat colPTS = feedbackTermStats.get(t);

        if (colPTS != null) {
            smoothedMLEofTerm = 
                ((docPTS!=null)?(mixingLambda * (float)docPTS.getCF() / (float)dv.getDocSize()):(0)) +
                ((feedbackTermStats.get(t)!=null)?((1.0f-mixingLambda)*(float)feedbackTermStats.get(t).getCF()/(float)vocSize):0);
//            (1.0f-mixingLambda)*(getCollectionProbability(t, indexReader, fieldForFeedback));
        }
        return smoothedMLEofTerm;
    } // ends return_Smoothed_MLE()

    /**
     * Returns the vocabulary size of the collection for 'fieldForFeedback'.
     * @return vocSize Total number of terms in the vocabulary
     * @throws IOException IOException
     */
    private long getVocabularySize() throws IOException {

        Fields fields = MultiFields.getFields(indexReader);
        Terms terms = fields.terms(fieldForFeedback);
        if(null == terms) {
            System.err.println("Field: "+fieldForFeedback);
            System.err.println("Error buildCollectionStat(): terms Null found");
        }
        vocSize = terms.getSumTotalTermFreq();  // total number of terms in the index in that field

        return vocSize;  // total number of terms in the index in that field
    }

    public float getCollectionProbability(String term, IndexReader reader, String fieldName) throws IOException {

        Term termInstance = new Term(fieldName, term);
        long termFreq = reader.totalTermFreq(termInstance); // CF: Returns the total number of occurrences of term across all documents (the sum of the freq() for each doc that has this term).

        return (float) termFreq / (float) vocSize;
    }

    /**
     * Returns MLE of a query term q in Q;<p>
     * P(w|Q) = tf(w,Q)/|Q|
     * @param qTerms all query terms
     * @param qTerm query term under consideration
     * @return MLE of qTerm in the query qTerms
     */
    public float returnMLE_of_q_in_Q(String[] qTerms, String qTerm) {

        int count=0;
        for (String queryTerm : qTerms)
            if (qTerm.equals(queryTerm))
                count++;
        return ( (float)count / (float)qTerms.length );
    } // ends returnMLE_of_w_in_Q()

    /**
     * RM1: IID Sampling <p>
     * Returns 'hashmap_PwGivenR' containing all terms of PR docs (PRD) with 
     * weights calculated using IID Sampling <p>
     * P(w|R) = \sum{d\in PRD} {smoothedMLE(w,d)*smoothedMLE(Q,d)}
     * Reference: Relevance Based Language Model - Victor Lavrenko (SIGIR-2001)
     * @param topDocs Initial retrieved document list
     * @return 'hashmap_PwGivenR' containing all terms of PR docs with weights
     * @throws Exception 
     */
    ///*
    public HashMap RM1(TopDocs topDocs) throws Exception {

        float p_W_GivenR_one_doc;

        list_PwGivenR = new ArrayList<>();

        hashmap_PwGivenR = new LinkedHashMap<>();

        // Calculating for each wi in R: P(wi|R)~P(wi, q1 ... qk)
        // P(wi, q1 ... qk) = \sum{d\in PRD} {P(w|D)*\prod_{i=1... k} {P(qi|D}}

        for (Map.Entry<String, PerTermStat> entrySet : feedbackTermStats.entrySet()) {
            // for each t in R:
            String t = entrySet.getKey();
            p_W_GivenR_one_doc = 0;

            for (Map.Entry<Integer, DocumentVector> docEntrySet : feedbackDocumentVectors.entrySet()) {
            // for each doc in RF-set
                int luceneDocId = docEntrySet.getKey();
                p_W_GivenR_one_doc += 
                    return_Smoothed_MLE(t, feedbackDocumentVectors.get(luceneDocId)) *
                    hash_P_Q_Given_D.get(luceneDocId);
            }
            list_PwGivenR.add(new WordProbability(t, p_W_GivenR_one_doc));
        }

        // ++ sorting list in descending order
        Collections.sort(list_PwGivenR, new Comparator<WordProbability>(){
            @Override
            public int compare(WordProbability t, WordProbability t1) {
                return t.p_w_given_R<t1.p_w_given_R?1:t.p_w_given_R==t1.p_w_given_R?0:-1;
            }});
        // -- sorted list in descending order

        for (WordProbability singleTerm : list_PwGivenR) {
            if (null == hashmap_PwGivenR.get(singleTerm.w)) {
                hashmap_PwGivenR.put(singleTerm.w, new WordProbability(singleTerm.w, singleTerm.p_w_given_R));
            }
            //* else: The t is already entered in the hash-map 
        }

        return hashmap_PwGivenR;
    }   // ends RM1()


    /**
     * RM3 <p>
     * P(w|R) = QueryMix*RM1 + (1-QueryMix)*P(w|Q) <p>
     * Reference: Nasreen Abdul Jaleel - TREC 2004 UMass Report <p>
     * @param query The query 
     * @param topDocs Initially retrieved document list
     * @return hashmap_PwGivenR: containing numFeedbackTerms expansion terms with normalized weights
     * @throws Exception 
     */
    public HashMap RM3(Luc4TRECQuery query, TopDocs topDocs) throws Exception {

        hashmap_PwGivenR = new LinkedHashMap<>();

        hashmap_PwGivenR = RM1(topDocs);
        // hashmap_PwGivenR has all terms of PRDs along with their probabilities 

        /*
        // +++ Inserting the idf factor
        for (Map.Entry<String, WordProbability> entrySet : hashmap_PwGivenR.entrySet()) {
            String key = entrySet.getKey();
            WordProbability value = entrySet.getValue();
            value.p_w_given_R *= Math.log(docCount/(feedbackTermStats.get(key).getDF()+1));
            hashmap_PwGivenR.put(key, value);
        }
        hashmap_PwGivenR = sortByValues(hashmap_PwGivenR);
        // ---
        //*/

        // +++ selecting top numFeedbackTerms terms and normalize
        int expansionTermCount = 0;
        float normFactor = 0;

        list_PwGivenR = new ArrayList<>(hashmap_PwGivenR.values());
        hashmap_PwGivenR = new LinkedHashMap<>();
        for (WordProbability singleTerm : list_PwGivenR) {
            if (null == hashmap_PwGivenR.get(singleTerm.w)) {
                hashmap_PwGivenR.put(singleTerm.w, new WordProbability(singleTerm.w, singleTerm.p_w_given_R));
                expansionTermCount++;
                normFactor += singleTerm.p_w_given_R;
                if(expansionTermCount>=numFeedbackTerms)
                    break;
            }
            //* else: The t is already there in the hash-map 
        }
        // ++ Normalizing 
        for (Map.Entry<String, WordProbability> entrySet : hashmap_PwGivenR.entrySet()) {
            WordProbability wp = entrySet.getValue();
            wp.p_w_given_R /= normFactor;
        }
        // -- Normalizing done

        String[] analyzedQuery = query.q_str.split("\\s+");

        normFactor = 0;
        //* Each w of R: P(w|R) to be (1-QMIX)*P(w|R) 
        for (Map.Entry<String, WordProbability> entrySet : hashmap_PwGivenR.entrySet()) {
            String key = entrySet.getKey();
            WordProbability value = entrySet.getValue();
            value.p_w_given_R = value.p_w_given_R * (1.0f-QMIX);
            normFactor += value.p_w_given_R;
        }

        // Now P(w|R) = (1-QMIX)*P(w|R)
        //* Each w which are also query terms: P(w|R) += QMIX*P(w|Q)
        //      P(w|Q) = tf(w,Q)/|Q|
        for (String qTerm : analyzedQuery) {
            WordProbability oldProba = hashmap_PwGivenR.get(qTerm);
            float newProb = QMIX * returnMLE_of_q_in_Q(analyzedQuery, qTerm);
            normFactor += newProb;
            if (null != oldProba) { // qTerm is in R
                oldProba.p_w_given_R += newProb;
                hashmap_PwGivenR.put(qTerm, oldProba);
            }
            else  // the qTerm is not in R
                hashmap_PwGivenR.put(qTerm, new WordProbability(qTerm, newProb));
        }

        // ++ Normalizing
        for (Map.Entry<String, WordProbability> entrySet : hashmap_PwGivenR.entrySet()) {
            WordProbability wp = entrySet.getValue();
            wp.p_w_given_R /= normFactor;
        }
        // -- Normalizing done

        return hashmap_PwGivenR;
    } // end RM3()

    /**
     * Returns the expanded query in BooleanQuery form with P(w|R) as 
     * corresponding weights for the expanded terms
     * @param expandedQuery The expanded query
     * @param query The query
     * @return BooleanQuery to be used for consequent re-retrieval
     * @throws Exception 
     */
    public BooleanQuery makeExpandedQuery(HashMap<String, WordProbability> expandedQuery, Luc4TRECQuery query) throws Exception {

        BooleanQuery booleanQuery = new BooleanQuery();
        
        for (Map.Entry<String, WordProbability> entrySet : expandedQuery.entrySet()) {
            String key = entrySet.getKey();
            if(key.contains(":"))
                continue;
            WordProbability wProba = entrySet.getValue();
            float value = wProba.p_w_given_R;

            Term t = new Term(fieldToSearch, key);
            Query tq = new TermQuery(t);
            tq.setBoost(value);
            BooleanQuery.setMaxClauseCount(4096);
            booleanQuery.add(tq, BooleanClause.Occur.SHOULD);
        }

        return booleanQuery;
    } // ends makeExpandedQuery()

    private static HashMap sortByValues(HashMap map) {
        List<Map.Entry<String, WordProbability>> list = new ArrayList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator<Map.Entry<String, WordProbability>>() {
            @Override
            public int compare(Map.Entry<String, WordProbability> t1, Map.Entry<String, WordProbability> t2) {
                return t1.getValue().p_w_given_R<t2.getValue().p_w_given_R?1:t1.getValue().p_w_given_R==t2.getValue().p_w_given_R?0:-1;
            }
        });

        // Copying the sorted list in HashMap
        // using LinkedHashMap to preserve the insertion order
        HashMap sortedHashMap = new LinkedHashMap();
        for (Map.Entry entry : list) {
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
        return sortedHashMap;
    }

    /**
     * 
     * @param topDocs
     * @param query
     * @return
     * @throws Exception 
     */
    public Query expandQuery(TopDocs topDocs, Luc4TRECQuery query) throws Exception {

        TopScoreDocCollector collector;

        setFeedbackStats(topDocs, query.q_str.split(" "));

        //hashmap_PwGivenR = rlm.RM1(query, finalTopDocs);
        hashmap_PwGivenR = RM3(query, topDocs);
        BooleanQuery booleanQuery;

        booleanQuery = makeExpandedQuery(hashmap_PwGivenR, query);

        // + Expanded retrieval
        collector = TopScoreDocCollector.create(numHits);
        System.out.println(booleanQuery.toString(fieldToSearch));
        indexSearcher.search(booleanQuery, collector);

        return booleanQuery;
//        finalTopDocs = collector.topDocs();
//        hits = finalTopDocs.scoreDocs;
//        if(hits == null)
//            System.out.println("Nothing found");
//        // - Expanded retrieval
//
//        return finalTopDocs;
    }
}
