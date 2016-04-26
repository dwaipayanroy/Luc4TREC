/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trecdata;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dwaipayan
 */

class TermPosition {
    String  term;
    int     position;

    public TermPosition(String term, int position) {
        this.term = term;
        this.position = position;
    }
    
}
public class DumpIndex {

    Properties  prop;               // prop of the init.properties file
    File        indexFile;          // place where the index will be stored
    boolean     boolIndexExists;    // boolean flag to indicate whether the index exists or not
    boolean     boolDumpIndex;      // true if want ot dump the entire collection
    String      dumpPath;           // path of the file in which the dumping to be done

    static final public String FIELD_ID = "docid";
    static final public String FIELD_BOW = "content";       // ANALYZED bow content
    static final public String FIELD_RAW = "raw-content";   // raw, UNANALYZED content

    private DumpIndex(String propFile) throws Exception {

        prop = new Properties();
        try {
            prop.load(new FileReader(propFile));
        } catch (IOException ex) {
            System.err.println("Error: Properties file missing");
            //ex.printStackTrace();
            System.exit(1);
        }
        //----- Properties file loaded

        /* index path setting */
        indexFile = new File(prop.getProperty("indexPath"));
        Directory indexDir = FSDirectory.open(indexFile);

        if (DirectoryReader.indexExists(indexDir)) {
            System.out.println("Index exists in "+indexFile.getAbsolutePath());
            boolIndexExists = true;
            dumpPath = prop.getProperty("dumpPath");
            if(dumpPath == null) {
                System.err.println("Error: dumpPath missing in prop file\n");
                System.exit(1);
            }
        }
        else {
            System.err.println("Error: Index does not exists.\n");
            boolIndexExists = true;
        }
        /* index path set */

        boolDumpIndex = Boolean.parseBoolean(prop.getProperty("dumpIndex","true"));
    }

    private void dumpIndexRawContent() {

        System.out.println("Dumping raw content of index in: "+ dumpPath);

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile))) {
            PrintWriter pout = new PrintWriter(dumpPath);
            int maxDoc = reader.maxDoc();
            for (int i = 0; i < maxDoc; i++) {
                Document d = reader.document(i);
                //System.out.print(d.get(FIELD_BAG_OF_WORDS) + " ");
                pout.print(d.get(FIELD_BOW) + "\n");
            }
            System.out.println("Index dumped in: " + dumpPath);
            pout.close();
        }
        catch(IOException e) {
            System.err.println("Error: indexFile reading error");
            e.printStackTrace();
        }
    }

    private void dumpIndexWithPosition() {

        System.out.println("Dumping the index in: "+ dumpPath);

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile))) {
            FileWriter dumpFW = new FileWriter(dumpPath);
            int maxDoc = reader.maxDoc();
//            String docContent[] = new String[maxDocLength];
            ArrayList<TermPosition> docContent;

            for (int i = 0; i < maxDoc; i++) {
                docContent = new ArrayList<>();
                Document d = reader.document(i);
                System.out.println(i+": <"+d.get("docid")+">");
                //System.out.print(d.get(FIELD_BOW) + " ");
                Terms vector = reader.getTermVector(i, "content");

                TermsEnum termsEnum = null;
                termsEnum = vector.iterator(termsEnum);
                BytesRef text;
                while ((text = termsEnum.next()) != null) {
                    String term = text.utf8ToString();
                    //System.out.print(term+": ");
                    DocsAndPositionsEnum docsPosEnum = termsEnum.docsAndPositions(null, null, DocsAndPositionsEnum.FLAG_FREQS); 
                    docsPosEnum.nextDoc();

                    int freq=docsPosEnum.freq();
                    for(int k=0; k<freq; k++){
                        int position=docsPosEnum.nextPosition();
                        //System.out.print(position+" ");
                        docContent.add(new TermPosition(term, position));
                    }
                    //System.out.println("");
                }
                //Collections.sort(docContent, (TermPosition t1, TermPosition t2) -> t1.position - t2.position);
                Collections.sort (docContent, new Comparator<TermPosition>(){
                    @Override
                    public int compare(TermPosition r1, TermPosition r2){
                        return r1.position - r2.position;
                    }}
                );
                for (TermPosition termPos : docContent) {
//                    System.out.print(docContent.get(j).term+" ");
                    dumpFW.write(termPos.term + " ");
                }
                dumpFW.write("\n");
                //System.out.println("");
            }
            System.out.println("Index dumped in: " + dumpPath);
            dumpFW.close();
        }
        catch(IOException e) {
            System.err.println("Error: indexFile reading error");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {

        if(args.length == 0) {
            System.out.printf("Usage: java DumpIndex <init.properties>\n");
            args = new String[2];
            args[0] = "/home/dwaipayan/Dropbox/programs/TrecDatahandling/TrecData/trec.dump.properties";
        }

        DumpIndex dump = new DumpIndex(args[0]);

        if(dump.boolIndexExists == true) {
            dump.dumpIndexWithPosition();
        }
    }

}
