/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.wapo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;

/**
 *
 * @author dwaipayan
 */
public class GetAllCategories {
    
    IndexReader     indexReader;
    IndexSearcher   indexSearcher;
    String          indexPath;

    public GetAllCategories(String indexPath) throws IOException, SAXException, Exception {

        indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));

        indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(new DefaultSimilarity());
    }

    private void getAllCategories(String filePath) throws IOException {
        
        int docCount = indexReader.maxDoc();      // total number of documents in the index
        Query query = new MatchAllDocsQuery();
        TopScoreDocCollector collector = TopScoreDocCollector.create(docCount);
        TopDocs allDocs;
        indexSearcher.search(query, collector);
        allDocs = collector.topDocs();
        ScoreDoc[] hits = allDocs.scoreDocs;
        int hits_length = hits.length;

	PrintWriter writer = new PrintWriter(filePath, "UTF-8");

        for (int i = 0; i < hits_length; i++) {
            if(i % 10000 == 0)
                System.out.println("Writen: " +i + " documents");
            int luceneDocId = hits[i].doc;
            Document d = indexSearcher.doc(luceneDocId);
//            System.out.println(d.get(DocField.WAPO_CATEGORY));
            writer.println(d.get(DocField.WAPO_CATEGORY));
        }
	writer.close();
        System.out.println("Completed");
    }

    public static void main(String[] args) throws SAXException, Exception {
        GetAllCategories getCategories = new GetAllCategories("/store/collections/indexed/WaPo");
        getCategories.getAllCategories("/home/dwaipayan/wapo-article-categories.txt");
    }
}
