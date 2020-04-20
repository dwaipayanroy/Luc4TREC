
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
    }

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
    }

    // for unit testing
    public static void main(String[] args) throws IOException {

    }
}

