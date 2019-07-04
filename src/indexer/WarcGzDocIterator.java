
package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;

import java.util.zip.GZIPInputStream;

/**
 * Process documents stored in .warc.gz files.
 * @author dwaipayan
 */
public class WarcGzDocIterator extends DocumentProcessor implements Iterator<Document> {

    public static final String WARC_DOC_START = "WARC-TREC-ID:";  // every document starts from the WARC-TREC-ID
    public static final String WARC_DOC_END = "WARC/0.18";       // every document ends with "WARC/0.18" OR, EOF

    ClueWebSpamFiltering    cluewebSpamFilter;
    boolean                 toSpamFilter;

    public WarcGzDocIterator(Analyzer analyzer, String toStore) {
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.storeTermVector = "YES";   // default
    }

    public WarcGzDocIterator(Analyzer analyzer, String toStore, String spamScoreIndexPath, float spamScoreThreshold) throws IOException {
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.storeTermVector = "YES";   // default
        cluewebSpamFilter = new ClueWebSpamFiltering(spamScoreIndexPath, spamScoreThreshold);
        toSpamFilter = true;
    }

    /**
     * Sets the BufferedReader to read individual files.
     * @param file 
     */
    public void setFileToRead(File file) throws FileNotFoundException, IOException {
        docReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), "UTF-8"));
        at_eof = false;
    }

    @Override
    public boolean hasNext() {
        return !at_eof;
    }

    /**
     * Returns the next document in the collection, setting FIELD_ID, FIELD_BOW, and FIELD_FULL_BOW.
     * @return LuceneDocument, set with the values
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
                if (line.isEmpty())
                // Empty line read
                    continue;       // read next line

                // +++ WARC-TREC-ID reading
                if (!in_doc) {
                    if (line.startsWith(WARC_DOC_START)) {
                    // Start of a Document
                    // line is of the form: "WARC-TREC-ID: clueweb09-en0039-05-00000"
                        in_doc = true;
                        doc_no = line.split(" ")[1].trim();

                        // +++ spam filtering
                        if(toSpamFilter) {
                            if(cluewebSpamFilter.isSpam(doc_no)) {
                            // a spam document read; set 'line' with document end marker: WARC_DOC_END
                                doc_no = "";    // make doc_no empty, so that it can be skipped.
                                //line = WARC_DOC_END;    // .. so that 
                                while ( line != null && !line.startsWith(WARC_DOC_END)) {
                                // while the spam document is read
                                    line = docReader.readLine();
                                }
                                if(line == null)    // if at the end-of-file
                                    at_eof = true;
                                break;
                            }
                        }
                        // --- spam filtering

                        // +++ ignoring, from "WARC-TREC-ID" to the second "Content-Length"
                        int content_lenght_count = 0;
                        while(content_lenght_count != 2) {
                            line = docReader.readLine().trim();
                            if(line.startsWith("Content-Length:"))
                                content_lenght_count ++;
                        }
                        // --- ignored, from "WARC-TREC-ID" to the second "Content-Length"
                    }
                    continue;
                }
                // --- WARC-TREC-ID reading

                if (line.startsWith(WARC_DOC_END)) {
                // Document ends
                    if(in_doc)
                        in_doc = false;
                    break;
                }

                rawDocSb.append(line).append(" ");
            } // ends while; a document is read.

            if (!doc_no.isEmpty()){
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
