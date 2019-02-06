
package searcher;

import common.CommonVariables;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dwaipayan
 */
public class GetAllDocIds {

    File indexFile;
    String dumpPath;

    public GetAllDocIds() {
    }

    public GetAllDocIds(File indexFile, String dumpPath) {
        this.indexFile = indexFile;
        this.dumpPath = dumpPath;
    }

    
    public static void main(String[] args) throws IOException {

        GetAllDocIds obj = new GetAllDocIds(new File("/store/collections/indexed/trec678"), 
            "/home/dwaipayan/trec678.docid");
        System.out.println("Writing docId from index: "+obj.indexFile.getAbsolutePath()+
            " in: "+obj.dumpPath);
        obj.getAllDocIds();
    }

    /**
     * 
     * @throws IOException 
     */
    public void getAllDocIds() throws IOException {

        FileWriter fileWritter = new FileWriter(dumpPath);
        BufferedWriter bufferWritter = new BufferedWriter(fileWritter);

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexFile.toPath()))) {
            int maxDoc = reader.maxDoc();
            for (int i = 0; i < maxDoc; i++) {
                Document d = reader.document(i);
//                System.out.println(d.get(CommonVariables.FIELD_ID));
                bufferWritter.write(d.get(CommonVariables.FIELD_ID)+"\n");
            }
        }
        catch(Exception e) {
            System.err.println("NoSuchDirectory at: "+indexFile.getAbsolutePath());
//            e.printStackTrace();
        }
        bufferWritter.close();

    }
}
