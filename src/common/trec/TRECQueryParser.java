
package common.trec;

/**
 *
 * @author dwaipayan
 */
import common.EnglishAnalyzerWithSmartStopword;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;

public class TRECQueryParser extends DefaultHandler {

    StringBuffer        buff;      // Accumulation buffer for storing the current topic
    String              queryFilePath;
    TRECQuery           query;
    Analyzer            analyzer;
    StandardQueryParser queryParser;
    
    public List<TRECQuery>  queries;
    final static String[] TAGS = {"num", "title", "desc", "narr"};

    /**
     * Constructor: 
     *      analyzer is set to EnglishAnalyzer();
     * @param queryFilePath Absolute path of the query file
     * @throws SAXException 
     */
    public TRECQueryParser(String queryFilePath) throws SAXException {
       this.queryFilePath = queryFilePath;
       buff = new StringBuffer();
       queries = new LinkedList<>();
       analyzer = new EnglishAnalyzer();
    }

    /**
     * Constructor: 
     * @param queryFilePath Absolute path of the query file
     * @param analyzer Analyzer to be used for analyzing the query fields
     * @throws SAXException 
     */
    public TRECQueryParser(String queryFilePath, Analyzer analyzer) throws SAXException {
       this.queryFilePath = queryFilePath;
       this.analyzer = analyzer;
       buff = new StringBuffer();
       queries = new LinkedList<>();
       queryParser = new StandardQueryParser(this.analyzer);
    }

    /**
     * Parses the query file from xml format using SAXParser;
     * 'queries' list gets initialized with the queries 
     * (with title, desc, narr and qid in different place holders)
     * @throws Exception 
     */
    public void queryFileParse() throws Exception {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();

        saxParser.parse(queryFilePath, this);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("top"))
            query = new TRECQuery();
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //System.out.println(buff);
        if (qName.equalsIgnoreCase("title")) {
            query.qtitle = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("desc")) {
            query.qdesc = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("num")) {
            query.qid = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("narr")) {
            query.qnarr = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("top")) {
            queries.add(query);
        }        
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        buff.append(new String(ch, start, length));
    }

//    public Query getAnalyzedQuery(TRECQuery trecQuery) throws Exception {
//
//        System.out.println("  Using StandardQueryParser:");
//        trecQuery.qtitle = removeSpecialCharacters(trecQuery.qtitle);
//        Query luceneQuery = queryParser.parse(trecQuery.qtitle, fieldToSearch);
//        trecQuery.luceneQuery = luceneQuery;
//
//        return luceneQuery;
//    }
//
//    public Query getAnalyzedQuery(String queryString) throws Exception {
//
//        System.out.println("  Only make term query without any analysis:");
//        queryString = removeSpecialCharacters(queryString);
//
//        Query luceneQuery = new TermQuery(new Term(fieldToSearch, queryString));
//        return luceneQuery;
//    }
//
//    public Query getAnalyzedQuery(TRECQuery trecQuery, String field) throws Exception {
//
//        trecQuery.qtitle = trecQuery.qtitle.replaceAll("-", " ");
//        Query luceneQuery = queryParser.parse(trecQuery.qtitle.replaceAll("/", " ")
//            .replaceAll("\\?", " ").replaceAll("\"", " ").replaceAll("\\&", " "), field);
//        trecQuery.luceneQuery = luceneQuery;
//
//        return luceneQuery;
//    }

    public static void main(String[] args) {

        if (args.length < 1) {
            args = new String[1];
            System.err.println("usage: java TRECQueryParser <input xml file>");
            args[0] = "/home/dwaipayan/Dropbox/ir/corpora-stats/topics_xml/trec6.xml";
        }

        try {

            EnglishAnalyzerWithSmartStopword obj;
            obj = new EnglishAnalyzerWithSmartStopword();
            Analyzer analyzer = obj.setAndGetEnglishAnalyzerWithSmartStopword();

            TRECQueryParser queryParser = new TRECQueryParser(args[0], analyzer);
            queryParser.queryFileParse();

            System.out.println("<topics>");
            for (TRECQuery query : queryParser.queries) {
                System.out.println("<top>");
                System.out.println("  <num>"+query.qid + "</num>");
                System.out.println("  <title>"+query.qtitle + "</title>");
                System.out.println("  <title>"+ query.queryStrAnalyze(query.qtitle, analyzer) + "</title>");
                System.out.println("\t" + query.queryStrAnalyze(query.qtitle, analyzer));
                // TODO: change the field-name accordingly
                query.makeBooleanQuery(query.qtitle, "field-name", analyzer);
                System.out.println("\t" + query.luceneQuery);
                System.out.println("</top>");
            }
            System.out.println("</topics>");

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
