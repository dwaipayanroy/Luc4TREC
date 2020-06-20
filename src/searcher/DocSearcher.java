
package searcher;

import static common.trec.DocField.FIELD_BOW;
import static common.trec.DocField.FIELD_ID;
import common.trec.TRECQuery;
import common.trec.TRECQueryParser;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 *
 * @author dwaipayan
 */
public class DocSearcher extends Searcher {

    List<TRECQuery> queries;
    TRECQueryParser trecQueryparser;

    public DocSearcher(String propPath) throws IOException, Exception {

        super(propPath);

        /* constructing the query */
        fieldToSearch = prop.getProperty("fieldToSearch", FIELD_BOW);
        System.out.println("Searching field for retrieval: " + fieldToSearch);

        trecQueryparser = new TRECQueryParser(queryPath, analyzer);
        trecQueryparser.queryFileParse();
        queries = trecQueryparser.queries;
        /* constructed the query */

        /* setting res path */
        runName = queryFile.getName()+"-"+fieldToSearch+"-"+indexSearcher.getSimilarity(true).
            toString().replace(" ", "-").replace("(", "").replace(")", "").replace("00000", "");

        setRunName();
    }

    /**
     * Sets runName and resPath variables depending on similarity functions.
     */
    private void setRunName() throws IOException {
        
        /* setting res path */
        runName = queryFile.getName()+"-"+fieldToSearch+"-"+indexSearcher.getSimilarity(true).
            toString().replace(" ", "-").replace("(", "").replace(")", "").replace("00000", "");

        setResFileName(runName);

    }

    public TopDocs retrieve(TRECQuery query) throws Exception {

        TopDocs topDocs;

        query.fieldToSearch = fieldToSearch;
        query.luceneQuery = query.makeBooleanQuery(query.qtitle, fieldToSearch, analyzer);
        query.q_str = query.luceneQuery.toString(fieldToSearch);

        System.out.println(query.qid+": \t" + query.luceneQuery.toString(fieldToSearch));

        topDocs = search(query);

        return topDocs;
    }

    public void retrieveAll() throws Exception {

        TopDocs topDocs;
        ScoreDoc[] hits;

        StringBuffer resBuffer;

        for (TRECQuery query : queries) {

            topDocs = retrieve(query);
            if(topDocs.totalHits == 0)
                System.out.println(query.qid + ": documents retrieve: " + 0);

            else {
                hits = topDocs.scoreDocs;
                System.out.println(query.qid + ": documents retrieve: " +hits.length);
                resBuffer = makeTRECResFile(query.qid, hits, indexSearcher, runName, FIELD_ID);
                resFileWriter.write(resBuffer.toString());
            }
        }
        resFileWriter.close();
        System.out.println("The result is saved in: "+resPath);

    }

    public static void main(String[] args) throws IOException, Exception {

        DocSearcher collSearcher;

        String usage = "java NewsDocSearcher <properties-file>\n"
            + "Properties file must contain:\n"
            + "1. indexPath: Path of the index\n"
            + "2. fieldToSearch: Name of the field to use for searching\n"
            + "3. queryPath: Path of the query file (in proper xml format)\n"
            + "4. queryFieldFlag: 1-title, 2-title+desc, 3-title+desc+narr\n"
            + "5. similarityFunction: 0.DefaultSimilarity, 1.BM25Similarity, 2.LMJelinekMercerSimilarity, 3.LMDirichletSimilarity\n"
            + "6. param1: \n"
            + "7. [param2]: optional if using BM25";

        /* // uncomment this if wants to run from inside Netbeans IDE
        args = new String[1];
        args[0] = "searcher.properties";
        //*/

        if(0 == args.length) {
            System.out.println(usage);
            System.exit(1);
        }

        System.out.println("Using properties file: "+args[0]);
        collSearcher = new DocSearcher(args[0]);

        collSearcher.retrieveAll();
    }

}
