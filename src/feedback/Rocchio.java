/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package feedback;

import common.DocumentVector;
import common.Luc4TRECQuery;
import common.PerTermStat;
import java.io.IOException;
import static java.lang.Math.log;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author dwaipayan
 */
public class Rocchio {

    /* */
    int docCount;               // total number of documents in the collection
    double avgDl = 0;           // average document length of the collection

    IndexSearcher searcher;

    /* Rocchio parameters */
    double alpha;
    double beta;
    double gamma;

    /* BM25 parameters */
    double k1;
    double b;

    public Rocchio(IndexSearcher searcher, double alpha, double beta, double gamma, double k1, double b) throws IOException {
        this.searcher = searcher;
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;

        this.k1 = k1;
        this.b = b;
        docCount = searcher.getIndexReader().numDocs();
        avgDl = searcher.getIndexReader().getSumTotalTermFreq("content") / (double)docCount;
    }

    /**
     * Default parameter values taken from:
     * <a href=https://nlp.stanford.edu/IR-book/html/htmledition/the-rocchio71-algorithm-1.html>IR-book</a>
     * additionally with ignoring negative feedback.
     * @param searcher
     * @throws java.io.IOException
     */
    public Rocchio(IndexSearcher searcher) throws IOException {
        this(searcher, 1.0, 0.75, 0, 1.2, 0.75);
    }

    public Query expandQuery(TopDocs initialRes, Luc4TRECQuery query, int fbDocs, int fbTerms) throws IOException, Exception {
        
        ScoreDoc[] hits;
        DocumentVector weightedDV = new DocumentVector();
        Query booleanQuery;

        int hits_length;
        hits = initialRes.scoreDocs;
        hits_length = hits.length;               // number of documents retrieved in the first retrieval

        for (int i = 0; i < Math.min(fbDocs, hits_length); i++) {
            // for each feedback document
            int luceneDocId = hits[i].doc;
            Document d = searcher.doc(luceneDocId);
            DocumentVector docV = new DocumentVector("content");
            docV = docV.getDocumentVector(luceneDocId, searcher.getIndexReader());
            if(docV == null)
                continue;

            computeBM25Weights(docV, weightedDV);
        }

        booleanQuery = makeExpandedQuery(weightedDV, query);
        return booleanQuery;
    }

    public BooleanQuery makeExpandedQuery(DocumentVector weightedDV, Luc4TRECQuery query) throws Exception {

        List<PerTermStat> list = weightedDV.getTopTerms(100);

        BooleanQuery booleanQuery = new BooleanQuery();
        
        for (PerTermStat pts : list) {
            Term t = new Term(query.fieldToSearch, pts.t);
            Query tq = new TermQuery(t);
            tq.setBoost((float) pts.weight);
            BooleanQuery.setMaxClauseCount(4096);
            booleanQuery.add(tq, BooleanClause.Occur.SHOULD);

        }
        return booleanQuery;
    }

    // BM25Weight(w,d) = IDF(w) * ((k1 + 1) * tf(w,d)) / (k1 * (1.0 - b + b * (|d|/avgDl)) + tf(w,d))
    // IDF(w) = log ( (docCount - df(w) + 0.5) / (df(w) + 0.5) )
    public void computeBM25Weights(DocumentVector dv, DocumentVector combinedDV) {
        double idf;
        double weight;
        double docLen;
        double df;
        double tf;

        Iterator<Entry<String, PerTermStat>> iterator = dv.docPerTermStat.entrySet().iterator();
        while(iterator.hasNext()) {
        // for each term of the document
            Map.Entry<String, PerTermStat> termCompo = iterator.next();
            String term = termCompo.getKey();
            PerTermStat pts = termCompo.getValue();

            df = pts.getDF();
            idf = log ( (docCount - df + 0.5) / (df + 0.5) );
            docLen = dv.getDocSize();
            tf = pts.getCF();

            weight = idf * ((k1+1) * tf) / (k1 * (1.0 - b + b*(docLen / avgDl)) + tf);
            pts.setWeight((float)weight);

            combinedDV.addTerm(term, weight);
        }
    }
}
