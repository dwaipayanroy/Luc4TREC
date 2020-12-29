/*
 * Washington Post Collection Indexer.
 * The dataset used in TREC-News track.
 */
package indexer;

import static common.trec.DocField.FIELD_ID;
import common.EnglishAnalyzerWithSmartStopword;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class MSMarcoIndexer {

    String      propPath;
    Properties  prop;               // prop of the init.properties file

    String      collPath;           // path of the collection
    File        collDir;            // collection Directory
    File        indexFile;          // place where the index will be stored


    //### toStore
    //# - YES: store the raw content
    //# - analyzed: store the analyzed content
    //# - NO: do not store the content. doc.get() will return null
    String      toStore;            // YES / NO; to be read from prop file; default - 'NO'
    String      storeTermVector;    // NO / YES / WITH_POSITIONS / WITH_OFFSETS / WITH_POSITIONS_OFFSETS; to be read from prop file; default - YES
    String      stopFilePath;
    Analyzer    analyzer;           // analyzer

    IndexWriter indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done
    boolean     boolToDump;

    MSMarcoDocIterator docs;

    public MSMarcoIndexer(String propPath) throws IOException {

        this.propPath = propPath;
        prop = new Properties();
        try {
            prop.load(new FileReader(propPath));
        } catch (IOException ex) {
            System.err.println("Error: prop file missing at: "+propPath);
            System.exit(1);
        }
        // ----- properties file set

        // +++++ setting the analyzer with English Analyzer with Smart stopword list
        EnglishAnalyzerWithSmartStopword engAnalyzer;
        stopFilePath = prop.getProperty("stopFilePath");
        if (null != stopFilePath && new File(stopFilePath).exists())
            engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        else
            engAnalyzer = new common.EnglishAnalyzerWithSmartStopword();
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        collPath = prop.getProperty("collPath");
        collDir = new File(collPath);
        if (!collDir.exists() || !collDir.canRead()) {
            System.err.println("Collection directory '" +collDir.getAbsolutePath()+ "' does not exist or is not readable");
            System.exit(1);
        }

        /* index path setting */
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile.toPath());
        /* index path set */

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
        }
        else {
            System.out.println("Creating the index in: " + indexFile.getAbsolutePath());
            boolIndexExists = false;
        }

        IndexWriterConfig iwcfg;
        // +++ toStore or not
        if(prop.containsKey("toStore")) {
            toStore = prop.getProperty("toStore").toUpperCase();
            // ----- iwcfg set
            if(toStore.equals("ANALYZED")) {
            // i.e. analyzed content will be stored.
                // NOTE: WhitespaceAnalyzer is used as the content will be analyzed manually
                //  using previously defined analyzer.
                iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
                iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                indexWriter = new IndexWriter(indexDir, iwcfg);
                toStore = "YES";
                // TODO: uncomment the tagged region in DocumentProcession.java:181-185
            }
            else {
            // i.e. either RAW content or nothing will be stored
                iwcfg = new IndexWriterConfig(analyzer);
                iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                indexWriter = new IndexWriter(indexDir, iwcfg);
            }
        }
        else {   // default value
            toStore = "NO";
            iwcfg = new IndexWriterConfig(analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            indexWriter = new IndexWriter(indexDir, iwcfg);
        }
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
        else    // default value
            storeTermVector = "YES";
        // --- toStore or not

        dumpPath = null;
        if(prop.containsKey("dumpPath")) {
            boolDumpIndex = true;
            dumpPath = prop.getProperty("dumpPath");
        }

        docs = new MSMarcoDocIterator(analyzer, this.toStore, this.storeTermVector, this.dumpPath, prop);    
    }

    private void processTSVFile(File collDir) {

        try {

            docs.setFileToRead(collDir);

            Document doc;
            while (docs.hasNext()) {
                doc = docs.next();
                if (doc != null) {
                    System.out.println((++docIndexedCounter)+": Indexing doc: " + doc.getField(FIELD_ID).stringValue());
                    indexWriter.addDocument(doc);
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println("Error: '"+collDir.getAbsolutePath()+"' not found");
        } catch (IOException ex) {
            System.err.println("Error: IOException on reading '"+ collDir.getAbsolutePath()+"'");
        }

    }

    private void createIndex() throws IOException {

        System.out.println("Indexing started");

        processTSVFile(collDir);

        indexWriter.close();

        System.out.println("Indexing ends\n"+docIndexedCounter + " documents indexed");

    }

    public static void main(String[] args) throws IOException {

        MSMarcoIndexer indexer;
        if(args.length == 0) {
            args = new String[1];
            args[0] = "/home/dwaipayan/init.properties";
//            System.out.println(usage);
            System.exit(1);
        }

        indexer = new MSMarcoIndexer(args[0]);
        indexer.createIndex();
    }

}
