/**
 * Runs on the basis of command-line argument<p>
 * Uses the TrecDocIterator<p>
 */
package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import static common.trec.DocField.FIELD_BOW;
import static common.trec.DocField.FIELD_ID;

/**
 *
 * @author dwaipayan
 */
public class CommandLineIndexing {

    Properties  prop;               // prop of the init.properties file
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    String      specPath;       // path of the collection spec file
    String      collPath;           // path of the collection
    String      dumpPath;
    String      stopFilePath;
    File        collDir;            // collection Directory
    File        indexFile;          // place where the index will be stored
    Analyzer    analyzer;           // the analyzer
    IndexWriter indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    int         docIndexedCounter;  // document indexed counter

    /**
     * Sets 
     *  1.prop
     *  2.document-analyzer
     *  3.index-path
     * 
     * @param propPath
     * @throws IOException 
     */
    private void setCommonVariables(String propPath) throws IOException {

        prop = new Properties();
        try {
            prop.load(new FileReader(propPath));
        } catch (IOException ex) {
            System.err.println("Error: Properties file missing");
            //ex.printStackTrace();
            System.exit(1);
        }
        //----- Properties file loaded

        // +++++ setting the analyzer with English Analyzer with Smart stopword list
        stopFilePath = prop.getProperty("stopFilePath");
        common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        //+++++ index path setting
        Directory indexDir;

        indexDir = FSDirectory.open(indexFile.toPath());

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
        }
        else {
            System.out.println("Will create the index in: " + indexFile.getAbsolutePath());
            boolIndexExists = false;
            //* Create a new index in the directory 
            IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            indexWriter = new IndexWriter(indexDir, iwcfg);
        }
        //----- index path set

        if(prop.containsKey("dumpPath")) {
            dumpPath = prop.getProperty("dumpPath");
            System.out.println("The index will be dumped in: "+dumpPath);

            File fl = new File(prop.getProperty("dumpPath"));
            //if file exists, delete it
            if(fl.exists())
                System.out.println(fl.delete());
        }

    }

    /**
     * 
     * @param propPath
     * @param indexPath
     * @param collOrSpecPath: if ends with .docs: collPath; if ends with .spec: specPath
     * @throws Exception 
     * collOrSpecPath is either carrying the coll-path or the spec-path depending
     *  on the whether it ends with .docs or .spec.
     * So the actual path will be get after stripping the .docs/.spec from the string.
     */
    private CommandLineIndexing(String propPath, String indexPath, String collOrSpecPath) throws Exception {

        //+++++ collection path setting
        if(collOrSpecPath.endsWith(".spec")){
            boolIndexFromSpec = true;
            specPath = collOrSpecPath.substring(0, collOrSpecPath.lastIndexOf(".spec"));
        }
        else if(collOrSpecPath.endsWith(".docs")){
            boolIndexFromSpec = false;
            collPath = collOrSpecPath.substring(0, collOrSpecPath.lastIndexOf(".docs"));
            collDir = new File(this.collPath);
            if (!collDir.exists() || !collDir.canRead()) {
                System.err.println("Error: Collection directory '" +collDir.getAbsolutePath()+ "' does not exist or is not readable");
                System.exit(1);
            }
        }

        //----- collection path set 

        //+++++ index path setting
        indexFile = new File(indexPath);
        //----- index path set

        setCommonVariables(propPath);
    }

    /**
     * 
     * @param propPath
     * @throws Exception 
     */
    private CommandLineIndexing(String propPath) throws Exception {

        //+++++ collection path setting
        if(prop.containsKey("collSpec")) {
            boolIndexFromSpec = true;
            specPath = prop.getProperty("collSpec");
        }
        else if(prop.containsKey("collPath")) {
            boolIndexFromSpec = false;
            collPath = prop.getProperty("collPath");
            collDir = new File(collPath);
            if (!collDir.exists() || !collDir.canRead()) {
                System.err.println("Error: Collection directory '" +collDir.getAbsolutePath()+ "' does not exist or is not readable");
                System.exit(1);
            }
        }
        else {
            System.err.println("Neither collPath not collSpec is present");
            System.exit(1);
        }
        //----- collection path set 

        //+++++ index path setting
        if(!prop.containsKey("indexPath")) {
            System.err.println("Error: prop file missing indexpath");
            System.exit(1);
        }
        indexFile = new File(prop.getProperty("indexPath"));
        //----- index path set

        setCommonVariables(propPath);
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

    private void processFile(File file) {
        try {
            NewsDocIterator docs = new NewsDocIterator(file, analyzer, prop);
            Document doc;
            while (docs.hasNext()) {
                doc = docs.next();
                if (doc != null && doc.getField(FIELD_BOW) != null) {
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

    private void processDirectory(File collDir) throws Exception {

        File[] files = collDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                System.out.println("Indexing directory: " + file.getName());
                processDirectory(file);  // recurse
            }
            else {
                System.out.println("Indexing file: " + file.getAbsolutePath());
                processFile(file);
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
            System.out.println("Reading from spec file at: "+specPath);
            try (BufferedReader br = new BufferedReader(new FileReader(specPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                   processFile(new File(line));
                }
            }
        }
        else {
            if (collDir.isDirectory())
                processDirectory(collDir);
            else
                processFile(collDir);
        }

        indexWriter.close();

        System.out.println("Indexing ends\n"+docIndexedCounter + " files indexed");
    }

    public static void main(String[] args) throws Exception {

        CommandLineIndexing trecIndexer = null;

        String usage1 = "java TrecFullTextIndexer"
            + " -prop PROPERTIES_PATH [-index INDEX_PATH] "
            + "[-spec SPEC_PATH / -docs DOCS_PATH]\n"
            + "Possible runnable configurations:\n"
            + "\t1. P\n"
            + "\t2. PIS/PISD: 'D' will be ignored\n"
            + "\t3. PID\n"
            + "P - PROPERTIES_PATH\nI - INDEX_PATH\nS - SPEC_PATH\nD - DOCS_PATH";

        String usage2 = "java TrecIndexer"
            + " -prop PROPERTIES_PATH [-index INDEX_PATH] \n"
            + "[-spec SPEC_PATH / -docs DOCS_PATH]\n"
            + "This indexes the documents in SPEC_PATH/DOCS_PATH, "
            + "creating a Lucene index in INDEX_PATH.\n"
            + "If only properties file is given OR "
            + "any one of INDEX_PATH or DOCS_PATH is given, "
            + "INDEX_PATH and DOCS_PATH will "
            + "be read from the properties file.\n"
            + "Other information like, stopword.list path will be read from prop.\n"
            + "Possible runnable configurations:\n"
            + "\t1. P\n"
            + "\t2. PIS/PISD: 'D' will be ignored\n"
            + "\t3. PID\n"
            + "P - PROPERTIES_PATH\nI - INDEX_PATH\nS - SPEC_PATH\nD - DOCS_PATH";

        /*  // uncomment this if wants to run from inside Netbeans IDE
        args = new String[2];
        args[0] = "-prop";
        //args[1] = "trec123.index.properties";
        //args[1] = "trec4.index.properties";
        //args[1] = "trec5.index.properties";
        //args[1] = "trec678.index.properties";
        //args[1] = "wt10g.index.properties";
        */

        System.out.println("Usage: \n" +usage1);
        if(args.length == 0) {
            System.exit(1);
        }

        else {
            String propPath = null;
            String indexPath = null;
            String collPath = null;
            String specPath = null;

            for(int i=0;i<args.length;i++) {
                if (null != args[i]) switch (args[i]) {
                    case "-index":
                        indexPath = args[i+1];
                        System.out.println("Using -index path: "+indexPath);
                        i++;
                        break;
                    case "-docs":
                        collPath = args[i+1];
                        System.out.println("Using -docs path: "+collPath);
                        i++;
                        break;
                    case "-prop":
                        propPath = args[i+1];
                        System.out.println("Using -prop file information from: "+propPath);
                        i++;
                        break;
                    case "-spec":
                        specPath = args[i+1];
                        i++;
                        break;
                }
            }

            if(propPath == null) {
                System.err.println("Usage: " + usage2);
                System.exit(1);
            }
            if (indexPath == null || ( collPath == null && specPath == null) ) {
                if (propPath == null ) {
                    System.err.println("Usage: " + usage2);
                    System.exit(1);
                }
                else { // if propPath != null:
                    System.out.println("Using only -prop file information from: " + propPath);
                    System.exit(1);
                    trecIndexer = new CommandLineIndexing(propPath);
                }
            }
            else {
                if(specPath == null) {
                    collPath = collPath + ".docs";
                    trecIndexer = new CommandLineIndexing(propPath, indexPath, collPath);
                }
                else { // if specPath is not null
                    specPath = specPath + ".spec";
                    trecIndexer = new CommandLineIndexing(propPath, indexPath, specPath);
                }
            }
        }

        if(null!=trecIndexer && false==trecIndexer.boolIndexExists) {
            trecIndexer.createIndex();
            trecIndexer.boolIndexExists = true;
        }
    }

}
