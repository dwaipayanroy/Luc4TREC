
package searcher;

import common.wapo.WapoQuery;
import common.wapo.WapoQueryParser;
import java.io.IOException;
import java.util.List;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import static common.wapo.DocField.WAPO_CONTENT;
import static common.wapo.DocField.WAPO_DOCID;

/**
 *
 * @author dwaipayan
 */
public class WapoSearcher extends Searcher {

    List<WapoQuery> queries;
    WapoQueryParser wapoQueryparser;

    /**
     *
     * @param propPath
     * @throws IOException
     * @throws Exception
     */
    public WapoSearcher(String propPath) throws IOException, Exception {

        super(propPath);

        /* constructing the query */
        fieldToSearch = prop.getProperty("fieldToSearch", WAPO_CONTENT);
        System.out.println("Searching field for retrieval: " + fieldToSearch);

        wapoQueryparser = new WapoQueryParser(queryPath, analyzer);        
        wapoQueryparser.queryFileParse();
        queries = wapoQueryparser.queries;
        /* constructed the query */

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

    public TopDocs retrieve(WapoQuery query) throws Exception {

        TopDocs topDocs;

        query.fieldToSearch = fieldToSearch;
        query.luceneQuery = query.makeBooleanQuery(query.q_title, fieldToSearch, analyzer);
        query.q_str = query.luceneQuery.toString(fieldToSearch);

        System.out.println(query.qid+": \t" + query.luceneQuery.toString(fieldToSearch));

        topDocs = search(query);

        return topDocs;
    }

    public void retrieveAll() throws Exception {

        TopDocs topDocs;
        ScoreDoc[] hits;

        for (WapoQuery query : queries) {

            topDocs = retrieve(query);
            if(topDocs.totalHits == 0)
                System.out.println(query.qid + ": documents retrieve: " + 0);

            else {
                hits = topDocs.scoreDocs;
                System.out.println(query.qid + ": documents retrieve: " +hits.length);
                StringBuffer resBuffer = makeTRECResFile(query.qid, hits, indexSearcher, runName, WAPO_DOCID);
                resFileWriter.write(resBuffer.toString());
            }
        }
        resFileWriter.close();
        System.out.println("The result is saved in: "+resPath);

    }

    public static void main(String[] args) throws IOException, Exception {

        WapoSearcher searcher;

        /* // uncomment this if wants to run from inside Netbeans IDE
        args = new String[1];
        args[0] = "searcher.properties";
        //*/

        if(0 == args.length) {
            args = new String[1];
            args[0] = "/home/dwaipayan/Dropbox/programs/Luc4TREC/build/classes/newsir18-topics.xml-searcher.properties";
//            System.out.println(usage);
//            System.exit(1);
        }

        System.out.println("Using properties file: "+args[0]);
        searcher = new WapoSearcher(args[0]);

        searcher.retrieveAll();
    }

}
