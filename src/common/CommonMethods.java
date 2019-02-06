
package common;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

/**
 *
 * @author dwaipayan
 */
public class CommonMethods {

    /**
     * Returns a string-buffer in the TREC-res format for the passed queryId
     * @param queryId
     * @param hits
     * @param searcher
     * @param runName
     * @return
     * @throws IOException 
     */
    static final public StringBuffer writeTrecResFileFormat(String queryId, ScoreDoc[] hits, 
        IndexSearcher searcher, String runName) throws IOException {

        StringBuffer resBuffer = new StringBuffer();
        int hits_length = hits.length;
        for (int i = 0; i < hits_length; ++i) {
            int luceneDocId = hits[i].doc;
            Document d = searcher.doc(luceneDocId);
            resBuffer.append(queryId).append("\tQ0\t").
                append(d.get("docid")).append("\t").
                append((i)).append("\t").
                append(hits[i].score).append("\t").
                append(runName).append("\n");                
        }

        return resBuffer;
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

        // +++ For replacing characters- ':','_'
        Map<String, String> replacements = new HashMap<String, String>() {{
            put(":", " ");
            put("_", " ");
        }};
        // create the pattern joining the keys with '|'
        String regExp = ":|_";
        Pattern p = Pattern.compile(regExp);
        // --- For replacing characters- ':','_'

        StringBuffer temp;
        Matcher m;
        StringBuffer tokenizedContentBuff;
        TokenStream stream;
        CharTermAttribute termAtt;

        // +++ For replacing characters- ':','_'
        temp = new StringBuffer();
        m = p.matcher(text);
        while (m.find()) {
            String value = replacements.get(m.group(0));
            if(value != null)
                m.appendReplacement(temp, value);
        }
        m.appendTail(temp);
        text = temp.toString();
        // --- For replacing characters- ':','_'

        tokenizedContentBuff = new StringBuffer();

        stream = analyzer.tokenStream(fieldName, new StringReader(text));
        termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            if(!term.equals("nbsp") && !term.equals("amp"))
                tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff;
    }

    /**
     * Analyzes 'text', using 'analyzer', to be stored in a  dummy field.
     * @param analyzer
     * @param text
     * @return
     * @throws IOException 
     */
    public static StringBuffer analyzeText(Analyzer analyzer, String text) throws IOException {

        StringBuffer temp;
        Matcher m;
        StringBuffer tokenizedContentBuff;
        TokenStream stream;
        CharTermAttribute termAtt;

        tokenizedContentBuff = new StringBuffer();

        stream = analyzer.tokenStream("dummy_field", new StringReader(text));
        termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff;
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
     * Analyzes 'text', using 'analyzer', to be stored in a  dummy field.
     * @param analyzer
     * @param text
     * @return
     * @throws IOException 
     */
    public static StringBuffer analyzeTextRemoveNum(Analyzer analyzer, String text) throws IOException {

        StringBuffer temp;
        Matcher m;
        StringBuffer tokenizedContentBuff;
        TokenStream stream;
        CharTermAttribute termAtt;

        tokenizedContentBuff = new StringBuffer();

        stream = analyzer.tokenStream("dummy_field", new StringReader(text));
        termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            if(!isNumeric(term))
                tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();

        return tokenizedContentBuff;
    }

    // for unit testing
    public static void main(String[] args) throws IOException {

    }
}

