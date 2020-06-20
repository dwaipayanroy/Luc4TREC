
package common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

/**
 *
 * @author dwaipayan
 */
public class CommonMethods {

    /**
     * 
     * @param str
     * @return 
     */
    public static String removeSpecialCharacters(String str) {
        return str.replaceAll("-", " ").replaceAll("/", " ")
                .replaceAll("\\?", " ").replaceAll("\"", " ")
                .replaceAll("\\&", " ").replaceAll(":", " ")
                .replaceAll("_"," ");
    }

    /**
     * Analyzes 'text', using 'analyzer', to be stored in 'fieldName'.
     * @param analyzer The analyzer to be used for analyzing the text
     * @param text The text to be analyzed
     * @param fieldName The name of the field in which the text is going to be stored
     * @return The analyzed text as StringBuffer
     * @throws IOException 
     */
    public static StringBuffer analyzeText(Analyzer analyzer, String text, String fieldName) throws IOException {

        StringBuffer tokenizedContentBuff = new StringBuffer();

        TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(text));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff;
    } // ends analyzeText()

    /**
     * Analyzes 'text', using 'analyzer', to be stored in a dummy field.
     * @param analyzer
     * @param text
     * @return
     * @throws IOException 
     */
    public static StringBuffer analyzeText(Analyzer analyzer, String text) throws IOException {

        return analyzeText(analyzer, text, null);
    }

    static boolean isNumeric(String term) {
        int len = term.length();
        for (int i = 0; i < len; i++) {
            char ch = term.charAt(i);
            if (Character.isDigit(ch))
                return true;
        }
        return false;
    }

    /**
     * Analyzes 'text', using 'analyzer', to be stored in a dummy null field.
     * @param analyzer
     * @param text
     * @return
     * @throws IOException 
     */
    public static StringBuffer analyzeTextRemoveNum(Analyzer analyzer, String text) throws IOException {

        StringBuffer tokenizedContentBuff = new StringBuffer();

        TokenStream stream = analyzer.tokenStream(null, new StringReader(text));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            if(!isNumeric(term))
                tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff;
    } // ends analyzeTextRemoveNum()

    /**
     * Read the qrel file into a HashMap and return. 
     * @param qrelFile Path of the qrel file.
     * @return A HashMap with <code>qid</code> as Key and <code>KnownRelevance</code> as Value.
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static HashMap<String, KnownRelevance> readQrelFile(String qrelFile) throws FileNotFoundException, IOException {

        HashMap<String, KnownRelevance> allKnownJudgement = new HashMap();

        FileInputStream fis = new FileInputStream(new File(qrelFile));

	BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        String lastQid = "";
        KnownRelevance singleQueryInfo = new KnownRelevance();
	String line = null;

        System.out.println("Reading all judged documents from qrel...");
        while ((line = br.readLine()) != null) {
//            System.out.println(line);
            String qid, docid;
            int rel;
            String tokens[] = line.split("[\\s]+");
            qid = tokens[0];
            docid = tokens[2];
            rel = Integer.parseInt(tokens[3]);

            if(lastQid.equals(qid)) {
                if(rel <= 0)
                    singleQueryInfo.nonrelevant.add(docid);
                else
                    singleQueryInfo.relevant.add(docid);
            }
            else {  // a new query is read
                if(!lastQid.isEmpty()) // information about a query is there
                    allKnownJudgement.put(lastQid, singleQueryInfo);

                singleQueryInfo = new KnownRelevance();
                if(rel <= 0)
                    singleQueryInfo.nonrelevant.add(docid);
                else
                    singleQueryInfo.relevant.add(docid);
                lastQid = qid;
            }
	}

        // for the last query
        allKnownJudgement.put(lastQid, singleQueryInfo);

	br.close();
        System.out.println("Completed.");

        return allKnownJudgement;
    } // ends readQrelFile

    /**
     * 
     * @param qrelPath
     * @param queryIds
     * @param indexReader
     * @param fieldName
     * @param relType 0-NonRel, 1-Rel;
     * @return
     * @throws Exception 
     */
    public static HashMap<String, TopDocs> readJudgedDocsFromQrel(String qrelPath, ArrayList<String> queryIds,
        IndexReader indexReader, String fieldName, int relType) throws Exception {

        IndexSearcher docidSearcher;
        docidSearcher = new IndexSearcher(indexReader);

        ScoreDoc[] docidHits = null;
        TopDocs docidTopDocs = null;
        Query docidQuery;
        TopScoreDocCollector collector;

        QueryParser docidSearchParser = new QueryParser(fieldName, new WhitespaceAnalyzer());

        HashMap<String, KnownRelevance> allKnownJudgement;       // For TRF, to contain info about all known relevance

        allKnownJudgement = readQrelFile(qrelPath);         // all known judgements are read

        HashMap<String, TopDocs> allJudgedDocHashMap = new HashMap<>();

        for (String qid : queryIds) { // For each query:
//            System.out.println("Query ID: " + qid);
            KnownRelevance qKnownRel = allKnownJudgement.get(qid);

            int judgedDocNotFoundCount = 0;
            int judgedDocQrelCount = 0;
            ScoreDoc scoreDoc[] = null;
            String docid;
            int luceneDocid;

            if(relType == 1) {
                judgedDocQrelCount = qKnownRel.relevant.size();
    //            System.out.println("Num Rel = " + judgedDocQrelCount);
                scoreDoc= new ScoreDoc[judgedDocQrelCount];

                for (int i=0; i<judgedDocQrelCount; i++) {        // For each judged rel doc for that query:
                    docid = qKnownRel.relevant.get(i);
                    docidQuery = docidSearchParser.parse(docid);
                    collector = TopScoreDocCollector.create(1);
                    docidSearcher.search(docidQuery, collector);
                    docidTopDocs = collector.topDocs();
                    docidHits = docidTopDocs.scoreDocs;
                    if(docidHits.length == 0) {
                        System.err.println("Judged rel doc. not found in index: "+docid);
                        judgedDocNotFoundCount++;
                        continue;
                    }
                    luceneDocid = docidHits[0].doc;
                    scoreDoc[i] = new ScoreDoc(luceneDocid, 0);
                }
            }
            else {
                judgedDocQrelCount = qKnownRel.nonrelevant.size();
    //            System.out.println("Num NonRel = " + judgedDocQrelCount);
                scoreDoc= new ScoreDoc[judgedDocQrelCount];

                for (int i=0; i<judgedDocQrelCount; i++) {        // For each judged non-rel doc for that query:
                    docid = qKnownRel.nonrelevant.get(i);
                    docidQuery = docidSearchParser.parse(docid);
                    collector = TopScoreDocCollector.create(1);
                    docidSearcher.search(docidQuery, collector);
                    docidTopDocs = collector.topDocs();
                    docidHits = docidTopDocs.scoreDocs;
                    if(docidHits.length == 0) {
                        System.err.println("Judged non-rel doc. not found in index: "+docid);
                        judgedDocNotFoundCount++;
                        continue;
                    }
                    luceneDocid = docidHits[0].doc;
                    scoreDoc[i] = new ScoreDoc(luceneDocid, 0);
                }
            }

            if(judgedDocQrelCount-judgedDocNotFoundCount <=0){
                System.out.println("?? None of the judged documents found in the collection!");
                char ch = (char) System.in.read();
            }
            TopDocs topDocs = new TopDocs(judgedDocQrelCount-judgedDocNotFoundCount, scoreDoc, 0);
            allJudgedDocHashMap.put(qid, topDocs);
        }

        return allJudgedDocHashMap;
    }

    /**
     * Extracts judged relevant and non-relevant documents per-query into two hashmaps:
     * 1. <code>allRelDocsFromQrelHashMap</code> and 2. <code>allNonRelDocsFromQrelHashMap</code>
     * @param qrelPath
     * @param queryIds
     * @param indexReader
     * @param fieldName
     * @param allRelDocsFromQrelHashMap
     * @param allNonRelDocsFromQrelHashMap
     * @throws Exception 
     */
    public static void readJudgedDocsFromQrel(String qrelPath, ArrayList<String> queryIds,
            IndexReader indexReader, String fieldName, 
            HashMap<String, List<Integer>> allRelDocsFromQrelHashMap, 
            HashMap<String, List<Integer>> allNonRelDocsFromQrelHashMap) throws Exception {

        IndexSearcher docidSearcher;
        docidSearcher = new IndexSearcher(indexReader);

        HashMap<String, KnownRelevance> allKnownJudgement;       // For TRF, to contain info about all known relevance

        allKnownJudgement = readQrelFile(qrelPath);         // all known judgements are read

        System.out.println("Reading per-query relevant and non-relevant judged documents... ");
        for (String qid : queryIds) { // For each query:
//            System.out.println("Query ID: " + qid);
            KnownRelevance qKnownRel = allKnownJudgement.get(qid);

            int judgedRelDocNotFoundCount = 0;
            int judgedNonRelDocNotFoundCount = 0;
            String docid;
            int luceneDocid;

            int judgedRelDocQrelCount = qKnownRel.relevant.size();
            int judgedNonrelDocQrelCount = qKnownRel.nonrelevant.size();

            List<Integer> relScoreDoc = new ArrayList<>();
            List<Integer> nonrelScoreDoc = new ArrayList<>();
//            ScoreDoc relScoreDoc[] = new ScoreDoc[judgedRelDocQrelCount];
//            ScoreDoc nonrelScoreDoc[] = new ScoreDoc[judgedNonrelDocQrelCount];

            int i;
            int min = Math.min(judgedRelDocQrelCount, judgedNonrelDocQrelCount);
            int input_rel, input_nonrel;

            for (i=0, input_rel = 0, input_nonrel = 0; i<min; i++) {        // For each judged rel and nonrel doc for that query:
                // rel
                docid = qKnownRel.relevant.get(i);
                luceneDocid = getLuceneDocid(docid, docidSearcher, fieldName);
                if(luceneDocid < 0) {
                    System.err.println("1. Judged rel doc. not found in index: "+docid);
                    judgedRelDocNotFoundCount++;
                    continue;
                }
                relScoreDoc.add(input_rel++,luceneDocid);
//                relScoreDoc[input_rel++] = new ScoreDoc(luceneDocid, 0);

                // nonrel
                docid = qKnownRel.nonrelevant.get(i);
                luceneDocid = getLuceneDocid(docid, docidSearcher, fieldName);
                if(luceneDocid < 0) {
                    System.err.println("1. Judged non-rel doc. not found in index: "+docid);
                    judgedNonRelDocNotFoundCount++;
                    continue;
                }
                nonrelScoreDoc.add(input_nonrel++, luceneDocid);
//                nonrelScoreDoc[input_nonrel++] = new ScoreDoc(luceneDocid, 0);
            }

            if (judgedRelDocQrelCount > judgedNonrelDocQrelCount) {
                // rel
                for (; i<judgedRelDocQrelCount; i++) {        // For each judged non-rel doc for that query:
                    docid = qKnownRel.relevant.get(i);
                    luceneDocid = getLuceneDocid(docid, docidSearcher, fieldName);
                    if(luceneDocid < 0) {
                        System.err.println("2. Judged rel doc. not found in index: "+docid);
                        judgedRelDocNotFoundCount++;
                        continue;
                    }
                    relScoreDoc.add(input_rel++, luceneDocid);
//                    relScoreDoc[input_rel++] = new ScoreDoc(luceneDocid, 0);
                }
            }
            else {
                for (; i<judgedNonrelDocQrelCount; i++) {        // For each judged non-rel doc for that query:
                    docid = qKnownRel.nonrelevant.get(i);
                    luceneDocid = getLuceneDocid(docid, docidSearcher, fieldName);
                    if(luceneDocid < 0) {
                        System.err.println("2. Judged non-rel doc. not found in index: "+docid);
                        judgedNonRelDocNotFoundCount++;
                        continue;
                    }
                    nonrelScoreDoc.add(input_nonrel++, luceneDocid);
//                    nonrelScoreDoc[input_nonrel++] = new ScoreDoc(luceneDocid, 0);
                }
            }

            // rel
            if(judgedRelDocQrelCount-judgedRelDocNotFoundCount <=0){
                System.out.println("?? None of the judged rel documents found in the collection!");
                char ch = (char) System.in.read();
            }
//            TopDocs topDocs = new TopDocs(judgedRelDocQrelCount-judgedRelDocNotFoundCount, Arrays.copyOf(relScoreDoc, judgedRelDocQrelCount-judgedRelDocNotFoundCount), 0);
//            allRelDocsFromQrelHashMap.put(qid, topDocs);
            allRelDocsFromQrelHashMap.put(qid, relScoreDoc);

            // nonrel
            if(judgedNonrelDocQrelCount-judgedNonRelDocNotFoundCount <=0){
                System.out.println("?? None of the judged non-rel documents found in the collection!");
                char ch = (char) System.in.read();
            }
//            topDocs = new TopDocs(judgedNonrelDocQrelCount-judgedNonRelDocNotFoundCount, Arrays.copyOf(nonrelScoreDoc, judgedNonrelDocQrelCount-judgedNonRelDocNotFoundCount), 0);
//            allNonRelDocsFromQrelHashMap.put(qid, topDocs);
            allNonRelDocsFromQrelHashMap.put(qid, nonrelScoreDoc);
        }
        System.out.println("Completed.");
    }
    // -

    /**
     * Returns the Lucene docid associated with <code>docno</code>
     * @param docno
     * @param docidSearcher
     * @param fieldName name of the field where <code>docno</code> is stored
     * @return
     * @throws IOException
     * @throws ParseException 
     */
    public static int getLuceneDocid(String docno, IndexSearcher docidSearcher, String fieldName) throws IOException, ParseException {
        
        int luceneDocid = -1;
        QueryParser docidSearchParser = new QueryParser(fieldName, new WhitespaceAnalyzer());
        Query docidQuery = docidSearchParser.parse(docno);
        ScoreDoc[] docidHits = null;
        TopDocs docidTopDocs = null;
        TopScoreDocCollector collector;
        collector = TopScoreDocCollector.create(1);

        docidSearcher.search(docidQuery, collector);
        docidTopDocs = collector.topDocs();
        docidHits = docidTopDocs.scoreDocs;
        if(docidHits.length == 0) {
//            System.err.println("Judged doc. not found in index: "+docid);
        }
        else
            luceneDocid = docidHits[0].doc;
        return luceneDocid;
    } // ends getLuceneDocid()

    // for unit testing
    public static void main(String[] args) throws IOException {

    }
}

