
package common.wapo;

/**
 *
 * @author dwaipayan
 */
import common.EnglishAnalyzerWithSmartStopword;
import static common.wapo.DocField.WAPO_CONTENT;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;

public class WapoQueryParser extends DefaultHandler {

    StringBuffer        buff;      // Accumulation buffer for storing the current topic
    String              queryFilePath;
    WapoQuery           query;
    Analyzer            analyzer;
    StandardQueryParser queryParser;
    
    public List<WapoQuery>  queries;

    /**
     * Constructor: 
     *      analyzer is set to EnglishAnalyzer();
     * @param queryFilePath Absolute path of the query file
     * @throws SAXException 
     */
    public WapoQueryParser(String queryFilePath) throws SAXException {
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
    public WapoQueryParser(String queryFilePath, Analyzer analyzer) throws SAXException {
       this.queryFilePath = queryFilePath;
       this.analyzer = analyzer;
       buff = new StringBuffer();
       queries = new LinkedList<>();
       queryParser = new StandardQueryParser(this.analyzer);
    }

    /**
     * Parses the query file from xml format using SAXParser;
     *  'queries' list gets initialized with the queries 
     *  (with title, desc, narr and q_num in different place holders)
     * @throws Exception 
     */
    public void queryFileParse() throws Exception {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = saxParserFactory.newSAXParser();

        saxParser.parse(queryFilePath, this);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("top")) {
            query = new WapoQuery();
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
//        System.out.println(buff);
        if (qName.equalsIgnoreCase("docid")) {
            query.q_docid = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("num")) {
            query.qid = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("url")) {
            query.q_url = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("title")) {
            query.q_title = buff.toString().trim();
            buff.setLength(0);
        }
        else if (qName.equalsIgnoreCase("content")) {
            query.q_content = buff.toString().trim();
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

    public static void main(String[] args) {

        if (args.length < 1) {
            args = new String[1];
            System.err.println("usage: java WapoQueryParser <input xml file>");
//            args[0] = "/home/dwaipayan/Dropbox/ir/corpora-stats/topics_xml//newsir18-title.xml";
            args[0] = "/home/dwaipayan/Dropbox/ir/corpora-stats/topics_xml//newsir18-topics.txt";
        }

        try {

            EnglishAnalyzerWithSmartStopword obj;
            obj = new EnglishAnalyzerWithSmartStopword();
            Analyzer analyzer = obj.setAndGetEnglishAnalyzerWithSmartStopword();

            WapoQueryParser queryParser = new WapoQueryParser(args[0], analyzer);
            queryParser.queryFileParse();

            System.out.println("<topics>");
            for (WapoQuery query : queryParser.queries) {
                System.out.println("<top>");
                System.out.println("  <num>"+query.qid + "</num>");
                System.out.println("  <title>"+query.q_title + "</title>");
                System.out.println("  <title>"+ query.queryStrAnalyze(query.q_title, analyzer) + "</title>");
                // TODO: change the field-name accordingly
                query.makeBooleanQuery(query.q_title, "field-name", analyzer);
                System.out.println(query.luceneQuery);
                System.out.println("</top>");
            }
            System.out.println("</topics>");

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
