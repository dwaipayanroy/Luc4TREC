/**
 * TrecDocument Indexer using JSOUP. <p>
 * Indexes total content of each documents.<p>
 */

package trecdata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.FIELD_ID;


/**
 *
 * @author dwaipayan
 */
public class IndexWithJsoup {

    String      propPath;
    Properties  prop;               // prop of the init.properties file
    String      collPath;           // path of the collection
    String      collSpecPath;       // path of the collection spec file
    File        collDir;            // collection Directory
    File        indexFile;          // place where the index will be stored
    String      stopFilePath;
    Analyzer    analyzer;           // the paper analyzer
    IndexWriter indexWriter;
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean     boolIndexFromSpec;  // true; false if indexing from collPath
    int         docIndexedCounter;  // document indexed counter
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done
    boolean     boolToDump;

    public final void setAnalyzer() {
        String stopFile = prop.getProperty("stopFile");
        List<String> stopwords = new ArrayList<>();

        String line;
        try {
            FileReader fr = new FileReader(stopFile);
            BufferedReader br = new BufferedReader(fr);
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close(); fr.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        analyzer = new EnglishAnalyzer(StopFilter.makeStopSet(stopwords));
    }
    
    private IndexWithJsoup(String propPath) throws IOException {

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
        stopFilePath = prop.getProperty("stopFile");
        common.EnglishAnalyzerWithSmartStopword engAnalyzer = new common.EnglishAnalyzerWithSmartStopword(stopFilePath);
        analyzer = engAnalyzer.setAndGetEnglishAnalyzerWithSmartStopword();
        // ----- analyzer set: analyzer

        /* collection path setting */
        if(prop.containsKey("collSpec")) {
            boolIndexFromSpec = true;
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

        /* index path setting */
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile);
        /* index path set */

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
        }
        else {
            System.out.println("Creating the index in: " + indexFile.getName());
            boolIndexExists = false;
            // +++++ setting the IndexWriterConfig
            // NOTE: WhitespaceAnalyzer is used as the content will be analyzed with
            //  a loop for making the content available for dumping
            IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LATEST, new WhitespaceAnalyzer());
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            // ----- iwcfg set
            indexWriter = new IndexWriter(indexDir, iwcfg);
        }

        if(prop.containsKey("dumpPath")) {
            boolDumpIndex = true;
            dumpPath = prop.getProperty("dumpPath");
        }

    }

    private void indexDirectory(File collDir) throws Exception {
        File[] files = collDir.listFiles();
        //System.out.println("Indexing directory: "+files.length);
        for (File f : files) {
            if (f.isDirectory()) {
                System.out.println("Indexing directory: " + f.getName());
                indexDirectory(f);  // recurse
            }
            else {
                System.out.println((docIndexedCounter+1)+": Indexing file: " + f.getName());
                indexFile(f);
                docIndexedCounter++;
            }
        }
    }

    /**
     * Analyze "txt" using "analyzer" suitable for storing in "fieldName"
     * @param analyzer
     * @param txt
     * @param fieldName
     * @return
     * @throws IOException 
     */
    public String analyzeText(Analyzer analyzer, String txt, String fieldName) throws IOException {

        StringBuffer tokenizedContentBuff = new StringBuffer();

        TokenStream stream = analyzer.tokenStream(fieldName, new StringReader(txt));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }

        stream.end();
        stream.close();
        //System.out.println(tokenizedContentBuff.toString());

        return tokenizedContentBuff.toString();
    }

    public String refineSpecialChars(String txt) {
        if(txt!=null)
            txt = txt.replaceAll("\\p{Punct}+", " ");
        return txt;
    }

    public String removeURLs(String txt) {
        String urlPatternStr = "\\b((https?|ftp|file)://|www)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern urlPattern = Pattern.compile(urlPatternStr);

        Matcher m = urlPattern.matcher(txt);

        return m.replaceAll(" ");
    }

    Document constructDoc(String id, String content) throws IOException {
    /*
        id: Unique document identifier
        content: Total content of the document
    */

        Document doc = new Document();

        doc.add(new Field(FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));
        /* unique doc-id is added */

        // analyzing the text with EnglishAnalyzerWithSmartStopword
        String txt = analyzeText(analyzer, content, FIELD_BOW);

        txt = removeURLs(txt);
        // refining the special characters (punctuations)
        txt = refineSpecialChars(txt);

        // dumping the content
        if (boolDumpIndex) {
            FileWriter fileWritter = new FileWriter(dumpPath, true);
            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
            bufferWritter.write(txt+"\n");
            bufferWritter.close();
        }

        // Storing the content in the index with field-name FIELD_BOW;
        // with termvector; analyzed with WhiteSpaceTokenizer
        doc.add(new Field(FIELD_BOW, txt,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
        /* FIELD_BOW: analyzed, bow content is added */

        return doc;
    }

    void indexFile(File collDir) throws Exception {

        Document doc;
        String line;
        FileReader fr = new FileReader(collDir);
        BufferedReader br = new BufferedReader(fr);

        StringBuffer txtbuff = new StringBuffer();
        while ((line = br.readLine()) != null)
            txtbuff.append(line).append("\n");
        String content = txtbuff.toString();

        org.jsoup.nodes.Document jdoc = Jsoup.parse(content);
        Elements docElts = jdoc.select("DOC");

        for (Element docElt : docElts) {
            Element docIdElt = docElt.select("DOCNO").first();
            System.out.println((docIndexedCounter+1)+": Indexing: "+docIdElt.text());
            doc = constructDoc(docIdElt.text(), docElt.text());
            //System.out.println(docElt.text());
            indexWriter.addDocument(doc);
            docIndexedCounter++;
        }
    }

    public void createIndex() throws Exception{

        if (indexWriter == null ) {
            System.err.println("Index already exists at " + indexFile.getName() + ". Skipping...");
            return;
        }

        System.out.println("Indexing started");

        if (boolIndexFromSpec) {
            /* if collectiomSpec is present, then index from the spec file*/
            String specPath = prop.getProperty("collSpec");
            System.out.println("Reading from spec file at: "+specPath);
            try (BufferedReader br = new BufferedReader(new FileReader(specPath))) {
                String line;
                while ((line = br.readLine()) != null) {
                   indexFile(new File(line));
                }
            }
        }
        else {
            if (collDir.isDirectory())
                indexDirectory(collDir);
            else
                indexFile(collDir);
        }

        indexWriter.close();

        System.out.println("Indexing ends successfully");
        System.out.println(docIndexedCounter + " files indexed");
    }

    private void dumpIndex() {
        System.out.println("Dumping the index in: "+ dumpPath);
        File f = new File(dumpPath);
        if (f.exists()) {
            System.out.println("Dump existed.");
            System.out.println("Last modified: "+f.lastModified());
            System.out.println("Overwrite(Y/N)?");
            Scanner reader = new Scanner(System.in);
            char c = reader.next().charAt(0);
            if(c == 'N' || c == 'n')
                return;
            else
                System.out.println("Dumping...");
        }
        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile))) {
            FileWriter dumpFW = new FileWriter(dumpPath);
            int maxDoc = reader.maxDoc();
            for (int i = 0; i < maxDoc; i++) {
                Document d = reader.document(i);
                //System.out.print(d.get(FIELD_BOW) + " ");
                dumpFW.write(d.get(FIELD_BOW) + " ");
            }
            System.out.println("Index dumped in: " + dumpPath);
            dumpFW.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String args[]) throws IOException, Exception {

        String usage = "Usage: java Indexer <init.properties>\n"
        + "Properties file must contain:\n"
        + "1. collSpec = path of the spec file containing the collection spec\n"
        + "2. indexPath = dir. path in which the index will be stored\n"
        + "3. stopFile = path of the stopword list file\n"
        + "4. [OPTIONAL] dumpPath = path of the file to dump the content";

        /*
        args = new String[1];
        args[0] = "trec678.index.properties";
        */

        if(args.length == 0) {
            System.out.println(usage);
            System.exit(1);
        }

        IndexWithJsoup indexer = new IndexWithJsoup(args[0]);

        if(indexer.boolIndexExists==false) {
            indexer.createIndex();
            indexer.boolIndexExists = true;
        }
    }
}
