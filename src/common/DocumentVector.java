
package common;

import static common.CommonMethods.analyzeText;
import static common.trec.DocField.FIELD_BOW;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */
public class DocumentVector {

    /**
     * Field for which, the statistics will be made.
     */
    public String field;
    /**
     * PerTermStat of the Document.
     */
    public HashMap<String, PerTermStat>     docPerTermStat;
    /**
     * Size of the Document.
     */
    private double                           size;
    /**
     * The retrieval score of the document after a retrieval. *Mostly unused*
     */
    private float                            docScore;   // retrieval score

    public DocumentVector() {
        docPerTermStat = new HashMap<>();
        field = FIELD_BOW;
    }

    public DocumentVector(String field) {
        docPerTermStat = new HashMap<>();
        this.field = field;
    }

    public DocumentVector(HashMap<String, PerTermStat> docVec, int size) {
        this.docPerTermStat = docVec;
        this.size = size;
    }

    public DocumentVector(HashMap<String, PerTermStat> docVec, int size, float docScore) {
        this.docPerTermStat = docVec;
        this.size = size;
        this.docScore = docScore;
    }

    public DocumentVector(Luc4TRECQuery query) {
        docPerTermStat = new HashMap<>();
        for(String q : query.q_str.split(" ")) {
            addTerm(q);
        }
        field = FIELD_BOW;
    }

    public HashMap getDocPerTermStat() {return docPerTermStat;}
    public double getDocSize() {return size;}
    public float getDocScore() {return docScore;}

    public void setDocSize(int size) {this.size = size;}

    /**
     * Returns the document vector for a document with lucene-docid=luceneDocId
       Returns dv containing <br>
      1) docPerTermStat : a HashMap of (t,PerTermStat) type <br>
      2) size : size of the document
     * @param luceneDocId
     * @param cs
     * @return document vector
     * @throws IOException 
     */
    public DocumentVector getDocumentVector(int luceneDocId, CollectionStatistics cs) throws IOException {

        DocumentVector dv = new DocumentVector();
        int docSize = 0;

        if(cs.indexReader==null) {
            System.out.println("Error: null == indexReader in showDocumentVector(int,IndexReader)");
            System.exit(1);
        }

        // t vector for this document and field, or null if t vectors were not indexed
        Terms terms = cs.indexReader.getTermVector(luceneDocId, field);
        if(null == terms) {
            System.err.println("Error getDocumentVector(): Term vectors not indexed: "+luceneDocId);
            return null;
        }

        TermsEnum iterator = terms.iterator();
        BytesRef byteRef = null;

        //* for each word in the document
        while((byteRef = iterator.next()) != null) {
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            //int docFreq = iterator.docFreq();            // df of 't'
            long termFreq = iterator.totalTermFreq();    // tf of 't'
            //System.out.println(t+": tf: "+termFreq);
            docSize += termFreq;

            //* termFreq = cf, in a document; df = 1, in a document
            //dv.docPerTermStat.put(t, new PerTermStat(t, termFreq, 1));
            dv.docPerTermStat.put(term, new PerTermStat(term, termFreq, 1, cs.perTermStat.get(term).getIDF(), (double)termFreq/(double)cs.getVocSize()));
        }
        dv.size = docSize;
        //System.out.println("DocSize: "+docSize);

        return dv;
    }

    public DocumentVector getDocumentVector(int luceneDocId, IndexReader indexReader) throws IOException {

        DocumentVector dv = new DocumentVector();
        int docSize = 0;

        if(indexReader==null) {
            System.out.println("Error: null == indexReader in showDocumentVector(int,IndexReader)");
            System.exit(1);
        }

        // t vector for this document and field, or null if t vectors were not indexed
        String fieldName = field;
        Terms terms = indexReader.getTermVector(luceneDocId, fieldName);
        if(null == terms) {
            System.err.println("Error getDocumentVector(): Term vectors not indexed: "+luceneDocId);
            return null;
        }

        TermsEnum iterator = terms.iterator();
        BytesRef byteRef = null;

        //* for each word in the document
        while((byteRef = iterator.next()) != null) {
            String term = new String(byteRef.bytes, byteRef.offset, byteRef.length);
            //int docFreq = iterator.docFreq();            // df of 't'
            long termFreq = iterator.totalTermFreq();    // tf of 't'
            //System.out.println(t+": tf: "+termFreq);
            docSize += termFreq;

            //* termFreq = cf, in a document; df = 1, in a document
            //dv.docPerTermStat.put(t, new PerTermStat(t, termFreq, 1));
            dv.docPerTermStat.put(term, new PerTermStat(term, termFreq, 1, getIdf(term, indexReader, fieldName), getCollectionProbability(term, indexReader, fieldName)));
        }
        dv.size = docSize;
        //System.out.println("DocSize: "+docSize);

        return dv;
    }

    public double getIdf(String term, IndexReader indexReader, String fieldName) throws IOException {
        int docCount = indexReader.maxDoc();      // total number of documents in the index
        Term termInstance = new Term(fieldName, term);
        long df = indexReader.docFreq(termInstance);       // DF: Returns the number of documents containing the term

        double idf;
        idf = Math.log((float)(docCount)/(float)(df+1));

        return idf;
    }

    public long getVocabularySize(IndexReader indexReader, String field) throws IOException {
        Fields fields = MultiFields.getFields(indexReader);
        Terms terms = fields.terms(field);
        if(null == terms) {
            System.err.println("Field: "+field);
            System.err.println("Error buildCollectionStat(): terms Null found");
        }
        long vocSize = terms.getSumTotalTermFreq();  // total number of terms in the index in that field

        return vocSize;
    }


    public float getCollectionProbability(String term, IndexReader reader, String fieldName) throws IOException {

        Term termInstance = new Term(fieldName, term);
        long termFreq = reader.totalTermFreq(termInstance); // CF: Returns the total number of occurrences of term across all documents (the sum of the freq() for each doc that has this term).

        return (float) termFreq / (float) getVocabularySize(reader, fieldName);
    }

    /**
     * Returns true if the docPerTermStat is not-zero; else, return false.
     * @return 
     */
    public boolean printDocumentVector() {

        if(this == null) {
            System.err.println("Error: printing document vector. Calling docVec null");
            System.exit(1);
        }
        if(0 == this.docPerTermStat.size()) {
            System.out.println("Error: printing document vector. Calling docVec zero");
            return false;
        }
        for (Map.Entry<String, PerTermStat> entrySet : this.docPerTermStat.entrySet()) {
            String key = entrySet.getKey();
            PerTermStat value = entrySet.getValue();
            System.out.println(key + " : " + value.getCF());
        }
        return true;
    }

    /** 
     * Returns the TF of 'term' in 'dv'.
     * @param term The term
     * @param dv Document vector
     * @return Returns the TF of 'term' in 'dv'
     */
    public long getTf(String term, DocumentVector dv) {
        PerTermStat t = dv.docPerTermStat.get(term);
        if(null != t)
            return t.getCF();
        else
            return 0;
    }

    /**
     * Make document vector from RAW content (unanalyzed text)
     * @param text
     * @param analyzer
     * @throws IOException 
     */
    private void makeVector(int luceneDocid, IndexSearcher searcher, Analyzer analyzer, String field, DocumentVector vector) throws IOException {

        String text = searcher.doc(luceneDocid).get(field);

        makeVector(text, analyzer, field, vector);
    }

    /**
     * When the <b>analyzed</b> content is stored in Lucene index with id: <code>luceneDocid</code>.
     * @param luceneDocid
     * @param searcher
     * @param field
     * @param vector
     * @throws IOException 
     */
    private void makeVector(int luceneDocid, IndexSearcher searcher, String field, DocumentVector vector) throws IOException {
        
        String text = searcher.doc(luceneDocid).get(field);

        makeVector(text, new WhitespaceAnalyzer(), field, vector);
    }

    private void makeVector(String text, String field, DocumentVector vector) throws IOException {
        makeVector(text, new WhitespaceAnalyzer(), field, vector);        
    }

    private void makeVector(String text, Analyzer analyzer, String field, DocumentVector vector) throws IOException {

        TokenStream stream = analyzer.tokenStream(field, new StringReader(text));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            System.out.println(term);
            vector.addTerm(term);
        }

        stream.end();
        stream.close();

    }

    private void addTerm(String term) {

        PerTermStat pts = docPerTermStat.get(term);
        if(pts == null) {
            docPerTermStat.put(term, new PerTermStat(term, 1, 1));
        } else {
            long tf = pts.getCF();
            docPerTermStat.put(term, new PerTermStat(term, tf+1, 1));
        }
        size += 1.0;
    }
    
    public void addTerm(String term, double weight) {

        PerTermStat pts = docPerTermStat.get(term);
        if(pts == null) {
            docPerTermStat.put(term, new PerTermStat(term, weight));
        } else {
            double w = pts.getWeight();
            docPerTermStat.put(term, new PerTermStat(term, w + weight));
        }
        size += weight;
    }

    /**
     *
     * @param numTopTerms
     * @return
     */
    public List<PerTermStat> getTopTerms(int numTopTerms) {

        Iterator<Map.Entry<String, PerTermStat>> iterator = docPerTermStat.entrySet().iterator();
        ArrayList<PerTermStat> topTerms = new ArrayList<>();

        while(iterator.hasNext()) {
        // for each term of the document
            Map.Entry<String, PerTermStat> termCompo = iterator.next();
            PerTermStat pts = termCompo.getValue();
            topTerms.add(pts);
        }

        // ++ sorting list in descending order
        Collections.sort(topTerms, new Comparator<PerTermStat>(){
            @Override
            public int compare(PerTermStat t, PerTermStat t1) {
                return t.weight<t1.weight?1:t.weight==t1.weight?0:-1;
            }

        });
        // -- sorted list in descending order

        return topTerms.subList(0, numTopTerms);
    }

    public List<PerTermStat> getTopTerms(DocumentVector dv, int numTopTerms) {
        return dv.getTopTerms(numTopTerms);
    }

    public void unitTesting_makeVector () throws IOException {
        String text = "The quick brown fox jumps over the lazy dog";
        DocumentVector dv = new DocumentVector();
        dv.makeVector(text, new EnglishAnalyzerWithSmartStopword().setAndGetEnglishAnalyzerWithSmartStopword(), null, dv);
    }
}
