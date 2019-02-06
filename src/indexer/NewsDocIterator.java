/**
 * Index TREC News document collections dropping all the HTML tags and URLs
 * Removes ':', '_'
 * Tested for TREC Disk 1-5.
 */

package indexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.FIELD_ID;
/*import org.apache.lucene.document.TextField;*/

/**
 *
 * @author dwaipayan
 */
public class NewsDocIterator extends DocumentProcessor implements Iterator<Document> {

    boolean     toRefine;           // true, if to remove html-tags and urls; default - false

    public NewsDocIterator(File file) throws FileNotFoundException {
        docReader = new BufferedReader(new FileReader(file));
    }

    public NewsDocIterator(File file, CommandLineIndexing obj) throws FileNotFoundException, IOException {
        docReader = new BufferedReader(new FileReader(file));
        this.analyzer = obj.analyzer;
    }

    public NewsDocIterator(Analyzer analyzer, String toStore, String dumpPath, Properties prop) throws FileNotFoundException{
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
        this.storeTermVector = "YES";   // default
        toRefine = Boolean.parseBoolean(prop.getProperty("toIndexRefinedContent", "true"));
    }

    public NewsDocIterator(Analyzer analyzer, String toStore, String storeTermVector, String dumpPath, Properties prop) throws FileNotFoundException{
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
        this.storeTermVector = storeTermVector;
        toRefine = Boolean.parseBoolean(prop.getProperty("toIndexRefinedContent", "true"));
    }

    public NewsDocIterator(File file, Analyzer analyzer, Properties prop) throws FileNotFoundException, IOException {
        docReader = new BufferedReader(new FileReader(file));
        this.analyzer = analyzer;
        this.dumpPath = prop.getProperty("dumpPath");
        this.toStore = prop.getProperty("toStore", "NO");
        this.toRefine = Boolean.parseBoolean(prop.getProperty("toRefine", "false"));
        // +++ toStore or not
        if(prop.containsKey("toStore")) {
            toStore = prop.getProperty("toStore");
            if(!toStore.equals("YES")&&!toStore.equals("NO")){
                System.err.println("prop file: toStore=YES/NO (case-sensitive); if not specified, considers NO");
                System.exit(1);
            }
        }
        else
            toStore = "NO";
        // --- 

        // +++ storeTermVector or not
        if(prop.containsKey("storeTermVector")) {
            storeTermVector = prop.getProperty("storeTermVector");

            if(!storeTermVector.equals("YES")&&!storeTermVector.equals("NO")&&
                !storeTermVector.equals("WITH_POSITIONS")&&!storeTermVector.equals("WITH_OFFSETS")&&
                !storeTermVector.equals("WITH_POSITIONS_OFFSETS")) {
                System.err.println("prop file: storeTermVector=NO / YES(default)/ "
                    + "WITH_POSITIONS / WITH_OFFSETS / WITH_POSITIONS_OFFSETS "
                    + "(case-sensitive)");
                System.exit(1);
            }
        }
        else
            storeTermVector = "YES";
        // --- 
    }

    public NewsDocIterator(File file, Analyzer analyzer, String toStore, String dumpPath) throws FileNotFoundException{
        docReader = new BufferedReader(new FileReader(file));
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
    }

    /**
     * Sets the BufferedReader to read individual files.
     * @param file 
     */
    public void setFileToRead(File file) throws FileNotFoundException {
        docReader = new BufferedReader(new FileReader(file));
        at_eof = false;
    }

    @Override
    public boolean hasNext() {
        return !at_eof;
    }

    /**
     * Returns the next document in the collection, setting FIELD_ID and FIELD_BOW.
     * @return 
     */
    @Override
    public Document next() {

        Document doc = new Document();
//        cleanTxtSb = new StringBuffer();
        rawDocSb = new StringBuffer();

        try {
            String line;
            boolean in_doc = false;
            doc_no = null;

            while (true) {
                line = docReader.readLine();

                if (line == null) {
                    at_eof = true;
                    break;
                }
                else
                    line = line.trim();

                // <DOC>
                if (!in_doc) {
                    if (line.startsWith("<DOC>"))
                        in_doc = true;
                    else
                        continue;
                }
                if (line.contains("</DOC>")) {
                    in_doc = false;
                    break;
                }
                // </DOC>

                // <DOCNO>
                if(line.startsWith("<DOCNO>")) {
                    doc_no = line;
                    while(!line.endsWith("</DOCNO>")) {
                        line = docReader.readLine().trim();
                        doc_no = doc_no + line;
                    }
                    doc_no = doc_no.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();
                    doc.add(new StringField(FIELD_ID, doc_no, Field.Store.YES));
                    continue;   // start reading the next line
                }
                // </DOCNO>

//                cleanTxtSb.append(line).append(" ");
                rawDocSb.append(line).append(" ");
            } // ends while; a document is read.

            if (doc_no == null || doc_no.isEmpty()){
                doc = null;
            }
            else {
            // a document content is read; need to processNewsDocument.
                doc = processNewsDocument();

                if(null != dumpPath) {

                    FileWriter fw = new FileWriter(dumpPath, true);
                    try (BufferedWriter bw = new BufferedWriter(fw)) {
                        bw.write(cleanContent.replaceAll("\\w*\\d\\w*", "")+"\n");
                    }
                }
            }
        } catch (IOException e) {
            doc = null;
        }
        return doc;
    }

    /**
     * This is only used for dumping the content of the files; NO INDEXING
     * @return 
     */
    public Document dumpNextDoc() {
        Document doc = new Document();
        StringBuffer sb = new StringBuffer();

        try {
            String line;
            boolean in_doc = false;
            doc_no = null;

            while (true) {
                line = docReader.readLine();

                if (line == null) {
                    at_eof = true;
                    break;
                }
                else
                    line = line.trim();

                // <DOC>
                if (!in_doc) {
                    if (line.startsWith("<DOC>"))
                        in_doc = true;
                    else
                        continue;
                }
                if (line.startsWith("</DOC>")) {
                    in_doc = false;
                    sb.append(line);
                    break;
                }
                // </DOC>

                // <DOCNO>
                if(line.startsWith("<DOCNO>")) {
                    doc_no = line;
                    while(!line.endsWith("</DOCNO>")) {
                        line = docReader.readLine().trim();
                        doc_no = doc_no + line;
                    }
                    doc_no = doc_no.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();
                    doc.add(new StringField(FIELD_ID, doc_no, Field.Store.YES));
                    continue;   // start reading the next line
                }
                // </DOCNO>

                sb.append(" ");
                sb.append(line);
            }
            if (sb.length() > 0) {

                String txt = sb.toString();
                if(toRefine) {
                    txt = removeHTMLTags(txt); // remove all html tags
                    txt = removeURL(txt);
                }

                // +++ For replacing characters- ':','_'
                StringBuffer temp = new StringBuffer();
                Matcher m = p.matcher(txt);
                while (m.find()) {
                    String value = replacements.get(m.group(0));
                    if(value != null)
                        m.appendReplacement(temp, value);
                }
                m.appendTail(temp);
                txt = temp.toString();
                // --- For replacing characters- ':','_'

                StringBuffer tokenizedContentBuff = new StringBuffer();

                TokenStream stream = analyzer.tokenStream(FIELD_BOW, 
                    new StringReader(txt));
                CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
                stream.reset();

                while (stream.incrementToken()) {
                    String term = termAtt.toString();
                    tokenizedContentBuff.append(term).append(" ");
                }

                stream.end();
                stream.close();

                FileWriter fileWritter = new FileWriter(dumpPath, true);
                BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                bufferWritter.write(tokenizedContentBuff.toString()+"\n");
                bufferWritter.close();
            }
        } catch (IOException e) {
            doc = null;
        }
        return doc;
    }

    @Override
    public void remove() {
    }
}
