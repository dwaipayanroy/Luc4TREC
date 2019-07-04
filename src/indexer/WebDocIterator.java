/**
 * Drops content of <DOCNO>, <DOCHDR>; 
 * Index contents, dropping all HTML-like tags and removing URLs; <p>
 * Removes ':', '_'
 * Tested for WT2G and GOV2 Collection.
 */

package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import static common.CommonVariables.FIELD_ID;
import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 *
 * @author dwaipayan
 */
public class WebDocIterator extends DocumentProcessor implements Iterator<Document> {

    boolean     toIndexRefinedContent; // whether to index the refined content; default - true

    public WebDocIterator(File file) throws FileNotFoundException {
        docReader = new BufferedReader(new FileReader(file));
//            System.out.println("Reading " + file.toString());
    }

    public WebDocIterator(File file, CommandLineIndexing obj) throws FileNotFoundException, IOException {
        docReader = new BufferedReader(new FileReader(file));
        this.analyzer = obj.analyzer;
    }

    public WebDocIterator(File file, Analyzer analyzer, Properties prop) throws FileNotFoundException, IOException {

        docReader = new BufferedReader(new FileReader(file));
        this.analyzer = analyzer;
        this.dumpPath = prop.getProperty("dumpPath");
        this.toStore = prop.getProperty("toStore", "NO");

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

    public WebDocIterator(Analyzer analyzer, String toStore, String dumpPath, Properties prop) throws FileNotFoundException{
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
        this.storeTermVector = "YES";   // default
        toIndexRefinedContent = Boolean.parseBoolean(prop.getProperty("toIndexRefinedContent", "true"));
    }

    public WebDocIterator(Analyzer analyzer, String toStore, String storeTermVector, String dumpPath, Properties prop) throws FileNotFoundException{
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
        this.storeTermVector = storeTermVector;
        toIndexRefinedContent = Boolean.parseBoolean(prop.getProperty("toIndexRefinedContent", "true"));
    }

    public WebDocIterator(Analyzer analyzer, String toStore, String dumpPath) throws FileNotFoundException{
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
        this.storeTermVector = "YES";   // default
    }

    public WebDocIterator(Analyzer analyzer, String toStore, String storeTermVector, String dumpPath) throws FileNotFoundException{
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
        this.storeTermVector = storeTermVector;
    }

    /**
     * Sets the BufferedReader to read individual files.
     * @param file 
     */
    public void setFileToRead(File file) throws FileNotFoundException {
        docReader = new BufferedReader(new FileReader(file));
        at_eof = false;
    }

    public void closeFileAfterReading() throws IOException {
        docReader.close();
        at_eof = true;
    }

    @Override
    public boolean hasNext() {
        return !at_eof;
    }

    /**
     * Returns the next document in the collection, setting FIELD_ID, FIELD_BOW, and FIELD_FULL_BOW.
     * @return 
     */
    @Override
    public Document next() {

        Document doc = new Document();
        rawDocSb = new StringBuffer();
        metaContent = new String();

        try {
            String line;

            boolean in_doc = false;
            doc_no = null;

            while (true) {
                line = docReader.readLine();

                if (line == null) {
                // EOF read
                    at_eof = true;
                    break;
                }
                else if (line.isEmpty())
                // Empty line read
                    continue;       // read next line

                // +++ <DOC>
                if (!in_doc) {
                    if (line.startsWith("<DOC>")) {
                        in_doc = true;
                    }
                    continue;
                }
                if (line.contains("</DOC>")) {
                // Document ends
                    if(in_doc)
                        in_doc = false;
                    break;
                }
                // --- </DOC>

                // +++ <DOCNO>
                if(line.startsWith("<DOCNO>")) {
                    doc_no = line;
//                    while(!line.endsWith("</DOCNO>")) {
//                        line = docReader.readLine().trim();
//                        doc_no = doc_no + line;
//                    }
                    doc_no = doc_no.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();

                    // Field: FIELD_ID
                    // the unique document identifier
                    doc.add(new StringField(FIELD_ID, doc_no, Field.Store.YES));
                    continue;   // start reading the next line
                }
                // --- </DOCNO>

                // +++ ignoring DOCOLDNO and DOCHDR
                // <DOCOLDNO>
                if(line.startsWith("<DOCOLDNO>")) {

                    while(!line.endsWith("</DOCOLDNO>"))
                        line = docReader.readLine().trim();
                    continue;   // start reading the next line
                } // </DOCOLDNO>

                // +++ <DOCHDR>
                if(line.startsWith("<DOCHDR>")) {

                    while(!line.endsWith("</DOCHDR>"))
                        line = docReader.readLine();
                    continue;   // start reading the next line
                } // --- </DOCHDR>
                // --- ignored DOCOLDNO and DOCHDR

                rawDocSb.append(line).append(" ");
            } // ends while; a document is read.

            if (doc_no != null && !doc_no.isEmpty()){
            // a document content is read; need to process the Document.
//                doc = processDocument();
                doc = processDocumentUsingJSoup();

                /*
                if(null != dumpPath) {

                    FileWriter fw = new FileWriter(dumpPath, true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write(cleanContent.replaceAll("\\w*\\d\\w*", "")+"\n");
                    bw.close();
                }
                //*/
            }
            else
                doc = null;
        } catch (IOException e) {
            doc = null;
        }
        return doc;
    } // end next()

    @Override
    public void remove() {
    }
}
