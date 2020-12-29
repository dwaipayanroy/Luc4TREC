/**
 * To index the MSMarco collection.
 */

package indexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;

import org.apache.lucene.document.Document;
import java.util.logging.Level;
import java.util.logging.Logger;
/*import org.apache.lucene.document.TextField;*/

/**
 *
 * @author dwaipayan
 */
public class MSMarcoDocIterator extends DocumentProcessor implements Iterator<Document> {

    boolean     toRefine;           // true, if to remove html-tags and urls; default - false

    public MSMarcoDocIterator(File file) throws FileNotFoundException {
        docReader = new BufferedReader(new FileReader(file));
    }

    public MSMarcoDocIterator(File file, CommandLineIndexing obj) throws FileNotFoundException, IOException {
        docReader = new BufferedReader(new FileReader(file));
        this.analyzer = obj.analyzer;
    }

    public MSMarcoDocIterator(Analyzer analyzer, String toStore, String dumpPath, Properties prop) throws FileNotFoundException{
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
        this.storeTermVector = "YES";   // default
        toRefine = Boolean.parseBoolean(prop.getProperty("toIndexRefinedContent", "true"));
    }

    public MSMarcoDocIterator(Analyzer analyzer, String toStore, String storeTermVector, String dumpPath, Properties prop) throws FileNotFoundException{
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
        this.storeTermVector = storeTermVector;
        toRefine = Boolean.parseBoolean(prop.getProperty("toIndexRefinedContent", "true"));
    }

    public MSMarcoDocIterator(File file, Analyzer analyzer, Properties prop) throws FileNotFoundException, IOException {
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

    public MSMarcoDocIterator(File file, Analyzer analyzer, String toStore, String dumpPath) throws FileNotFoundException{
        docReader = new BufferedReader(new FileReader(file));
        this.analyzer = analyzer;
        this.toStore = toStore;
        this.dumpPath = dumpPath;
    }

    /**
     * Sets the BufferedReader to read individual files.
     * @param file 
     * @throws java.io.FileNotFoundException 
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

        String line;

        try {
            line = docReader.readLine();
            if (line == null) {
                at_eof = true;
                return null;
            }
            else
                line = line.trim();

            doc = processMSMarcoDocument(line);

            if(null != dumpPath) {

                FileWriter fw = new FileWriter(dumpPath, true);
                try (BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(cleanContent.replaceAll("\\w*\\d\\w*", "")+"\n");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MSMarcoDocIterator.class.getName()).log(Level.SEVERE, null, ex);
        } 

        return doc;
    }

    @Override
    public void remove() {
    }
}
