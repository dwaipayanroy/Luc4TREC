/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package trecdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.FIELD_ID;

/**
 *
 * @author dwaipayan
 */
public class DumpAnalyzedText {

    boolean         boolIndexFromSpec;  // true; false if indexing from collPath
    String          specPath;       // path of the collection spec file
    Properties      prop;
    Analyzer        analyzer;           // the analyzer
    String          stopFilePath;
    String          collPath;           // path of the collection
    String          dumpPath;
    File            collDir;            // collection Directory
    int             docIndexedCounter;  // document indexed counter

    public DumpAnalyzedText(String propPath) {

        //+++++ Properties file loading
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

        File fl = new File(prop.getProperty("dumpPath"));
        //if file exists, delete it
        if(fl.exists())
            System.out.println(fl.delete());
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

    private void processFile(File file) {
        try {
            TrecDocIterator docs = new TrecDocIterator(file, analyzer, prop);
            Document doc;
            while (docs.hasNext()) {
                doc = docs.dumpNextDoc();
                if (doc != null && doc.getField(FIELD_BOW) != null) {
                    System.out.println((++docIndexedCounter)+": Indexing doc: " + doc.getField(FIELD_ID).stringValue());
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

    public void createDump() throws Exception {

        System.out.println("Text dumping started");

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

        System.out.println("Indexing ends\n"+docIndexedCounter + " files indexed");
    }

    public static void main(String[] args) throws Exception {

        DumpAnalyzedText analyzedText = new DumpAnalyzedText(args[0]);
        
        analyzedText.createDump();
    }
}
