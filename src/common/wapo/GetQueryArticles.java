/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.wapo;

import static common.wapo.DocField.WAPO_CONTENT;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.SAXException;
import static common.wapo.DocField.WAPO_DOCID;

/**
 *
 * @author dwaipayan
 */
public class GetQueryArticles {
    
    IndexReader     indexReader;
    IndexSearcher   indexSearcher;
    String          indexPath;

    List<WapoQuery> queries;
    WapoQueryParser wapoQueryparser;

    public GetQueryArticles(String indexPath, String queryPath) throws IOException, SAXException, Exception {

        indexReader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));

        indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(new DefaultSimilarity());
        wapoQueryparser = new WapoQueryParser(queryPath);
        queries = constructQueries();
    }

    private List<WapoQuery> constructQueries() throws Exception {

        wapoQueryparser.queryFileParse();
        return wapoQueryparser.queries;
    }

    private String makeQuery(WapoQuery query) throws IOException {

        ScoreDoc[] hits;
        TopDocs topDocs;

        TopScoreDocCollector collector = TopScoreDocCollector.create(1);
        Query luceneDocidQuery = new TermQuery(new Term(WAPO_DOCID, query.q_docid));

        indexSearcher.search(luceneDocidQuery, collector);
        topDocs = collector.topDocs();
        hits = topDocs.scoreDocs;
        if(hits.length <= 0) {
            System.out.println(query.q_docid+": document not found");
            return null;
        }
        else {
            System.out.println("<top>");
            System.out.println("  <num>"+query.qid + "</num>");
            System.out.println("  <title>"+indexSearcher.doc(hits[0].doc).get(DocField.WAPO_TITLE) + "</title>");
            System.out.println("  <content>"+indexSearcher.doc(hits[0].doc).get(DocField.WAPO_CATEGORY) + "</content>");
            System.out.println("</top>");
//            System.out.println(query.q_docid + " : " + indexSearcher.doc(hits[0].doc).get(DocField.WAPO_TITLE));
            return indexSearcher.doc(hits[0].doc).get(WAPO_CONTENT);
        }

    }

    public void makeQueries() throws IOException {

        System.out.println("<topics>");
        for (WapoQuery query : queries) {
            makeQuery(query);
        }
        System.out.println("</topics>");
    }

    public static void main(String[] args) throws SAXException, Exception {
        GetQueryArticles getArticles = new GetQueryArticles("/store/collections/indexed/WaPo", "/home/dwaipayan/Dropbox/ir/corpora-stats/topics_xml//newsir18-topics.xml");
        getArticles.makeQueries();
    }
}
