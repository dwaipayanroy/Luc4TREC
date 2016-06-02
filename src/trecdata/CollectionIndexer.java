/**
 * 
 */

package trecdata;

import java.io.BufferedReader;
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
import org.apache.lucene.util.Version;
import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.FIELD_ID;

/**
 *
 * @author dwaipayan
 */
public class CollectionIndexer {
    
    String      propPath;
    Properties  prop;               // prop of the init.properties file
    String      collPath;           // path of the collection
    String      collSpecPath;       // path of the collection spec file
    File        collDir;            // collection Directory
    int         collectionType;     // 1 - TREC collection, 2 - Web collection
    File        indexFile;          // place where the index will be stored
    String      toStore;            // YES / NO; to be read from prop file; default - 'NO'
    String      storeTermVector;    // NO / YES / WITH_POSITIONS / WITH_OFFSETS / WITH_POSITIONS_OFFSETS; to be read from prop file; default - YES
    String      stopFilePath;
    Analyzer    analyzer;           // the paper analyzer
    IndexWriter indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done
    boolean     boolToDump;

    /**
     * 
     * @param propPath
     * @throws Exception 
     */
    private CollectionIndexer(String propPath) throws Exception {

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
        stopFilePath = prop.getProperty("stopFilePath");
        common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        /* collection path setting */
        if(prop.containsKey("collSpec")) {
            boolIndexFromSpec = true;
            collSpecPath = prop.getProperty("collSpec");
        }
        else if(prop.containsKey("collPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("collPath");
            collDir = new File(collPath);
            if (!collDir.exists() || !collDir.canRead()) {
                System.err.println("Collection directory '" +collDir.getAbsolutePath()+ "' does not exist or is not readable");
                System.exit(1);
            }
        }
        else {
            System.err.println("Neither collPath not collSpec is present");
            System.exit(1);
        }
        /* collection path set */

        // +++ collectionType
        if(prop.containsKey("collectionType"))
            collectionType = Integer.parseInt(prop.getProperty("collectionType"));
        else {
            System.err.println("collectionType missing in prop file");
            System.out.println("collectionType=1(TREC) / 2(Web)");
            System.exit(1);
        }
        // --- collectionType

        /* index path setting */
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile);
        /* index path set */

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
        }
        else {
            System.out.println("Creating the index in: " + indexFile.getAbsolutePath());
            boolIndexExists = false;
            // +++++ setting the IndexWriterConfig
            // NOTE: WhitespaceAnalyzer is used as the content will be analyzed 
            //  using previously defined analyzer.
            IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LATEST, new WhitespaceAnalyzer());
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            // ----- iwcfg set
            indexWriter = new IndexWriter(indexDir, iwcfg);
        }

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

        dumpPath = null;
        if(prop.containsKey("dumpPath")) {
            boolDumpIndex = true;
            dumpPath = prop.getProperty("dumpPath");
        }

    }

    public static String refineSpecialChars(String txt) {
        if(txt!=null)
            txt = txt.replaceAll("\\p{Punct}+", " ");
        return txt;
    }

    public static String refineTexts(String txt) {
    /* removes all special characters from txt. */

        // removes the urls
//        txt = removeUrl(txt);

        // removes any special characters
        txt = refineSpecialChars(txt);

        return txt;
    }

    /**
     * When the file is from Trec collection: TREC disk 1-5
     * @param file 
     */
    private void processTrecFile(File file) {
        try {
//            TrecDocIterator docs = new TrecDocIterator(file, analyzer, prop);

            TrecDocIterator docs = new TrecDocIterator(file, analyzer, this.toStore, this.dumpPath);

            Document doc;
            while (docs.hasNext()) {
                doc = docs.next();
                if (doc != null && doc.getField(FIELD_BOW) != null) {
                    if(doc.getField(FIELD_ID) == null) {
                        System.err.println("NULL");
                        char ch = (char) System.in.read();
                    }
                    System.out.println((++docIndexedCounter)+": Indexing doc: " + doc.getField(FIELD_ID).stringValue());
                    indexWriter.addDocument(doc);
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println("Error: '"+file.getAbsolutePath()+"' not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.err.println("Error: IOException on reading '"+file.getAbsolutePath()+"'");
            ex.printStackTrace();
        }

    }

    /**
     * When the file is from web collection type: WT10G, GOV2
     * @param file 
     */
    private void processWebFile(File file) {
        try {
//            TrecDocIterator docs = new TrecDocIterator(file, analyzer, prop);

            WebDocIterator docs = new WebDocIterator(file, analyzer, this.toStore, this.dumpPath);

            Document doc;
            while (docs.hasNext()) {
                doc = docs.next();
                if (doc != null && doc.getField(FIELD_BOW) != null) {
                    if(doc.getField(FIELD_ID) == null) {
                        System.err.println("Read NULL for doc.FIELD_ID");
                        char ch = (char) System.in.read();
                    }
                    else {
                        System.out.println((++docIndexedCounter)+": Indexing doc: " + doc.getField(FIELD_ID).stringValue());
                        indexWriter.addDocument(doc);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            System.err.println("Error: '"+file.getAbsolutePath()+"' not found");
            ex.printStackTrace();
        } catch (IOException ex) {
            System.err.println("Error: IOException on reading '"+file.getAbsolutePath()+"'");
            ex.printStackTrace();
        }

    }

    private void processDirectory(File collDir) throws Exception {

        File[] files = collDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Indexing directory: " + file.getName());
                processDirectory(file);  // recurse
            }
            else {
                System.out.println("Indexing file: " + file.getAbsolutePath());
                processTrecFile(file);
            }
        }
    }

    public void createIndex() throws Exception {

        if (indexWriter == null ) {
            System.err.println("Index already exists at " + indexFile.getName() + ". Skipping...");
            return;
        }

        System.out.println("Indexing started");

        if (boolIndexFromSpec) {
            /* if collectiomSpec is present, then index from the spec file*/
            System.out.println("Reading from spec file at: "+collSpecPath);
            try (BufferedReader br = new BufferedReader(new FileReader(collSpecPath))) {
                String line;
                if(collectionType == 1) {
                    while ((line = br.readLine()) != null)
                       processTrecFile(new File(line));
                }
                else if(collectionType == 2) {
                    while ((line = br.readLine()) != null)
                       processWebFile(new File(line));
                }

            }
        }
        else {
            if (collDir.isDirectory())
                processDirectory(collDir);
            else
                processTrecFile(collDir);
        }

        indexWriter.close();

        System.out.println("Indexing ends\n"+docIndexedCounter + " files indexed");
    }

    public static void main(String[] args) throws Exception {

        CollectionIndexer collIndexer = null;

        String usage = "Usage: java CollectionIndexer <init.properties>\n"
        + "Properties file must contain:\n"
        + "1. collSpec = path of the spec file containing the collection spec\n"
        + "2. indexPath = dir. path in which the index will be stored\n"
        + "3. collectionType = 1 - Trec; 2 - Web"
        + "4. stopFile = path of the stopword list file\n"
        + "5. [OPTIONAL] dumpPath = path of the file to dump the content\n"
        + "6. [OPTIONAL] toStore = YES / NO (default)\n"
        + "7. [OPTIONAL] storeTermVector==NO/YES(default)/"
            + "WITH_POSITIONS/WITH_OFFSETS/WITH_POSITIONS_OFFSETS";

        /*
        args = new String[1];
        args[0] = "/home/dwaipayan/Dropbox/programs/TrecDatahandling/TrecData/build/classes/trec123.index.properties";
        //*/
        if(args.length == 0) {
            System.out.println(usage);
            args = new String[1];
            args[0] = "/home/dwaipayan/Dropbox/programs/TrecDatahandling/TrecData/build/classes/trec-sample.index.properties";
//            System.exit(1);
        }

        collIndexer = new CollectionIndexer(args[0]);

        if(collIndexer.boolIndexExists==false) {
            collIndexer.createIndex();
            collIndexer.boolIndexExists = true;
        }
    }

}
