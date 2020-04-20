/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package common.trec;

import common.CommonMethods;
import static common.CommonMethods.analyzeText;
import common.Luc4TRECQuery;
import static common.trec.DocField.FIELD_BOW;
import java.io.StringReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.Query;

/**
 *
 * @author dwaipayan
 */
public class TRECQuery extends Luc4TRECQuery{
//    public String       qid;
    public String       qtitle;
    public String       qdesc;
    public String       qnarr;
//    public Query        luceneQuery;
//    public String       fieldToSearch;

    @Override
    public String toString() {
        return qid + "\t" + qtitle;
    }

    /**
     * Returns analyzed queryFieldText from the query
     * @param analyzer
     * @param queryFieldText
     * @return (String) The content of the field
     * @throws Exception 
     */
    public String queryFieldAnalyze(Analyzer analyzer, String queryFieldText) throws Exception {
        System.out.println("  Manual analysis: ");
        fieldToSearch = FIELD_BOW;
        StringBuffer localBuff = analyzeText(analyzer, queryFieldText, fieldToSearch);

        return localBuff.toString();
    }
//
//    public Query getBOWQuery(Analyzer analyzer, TRECQuery query) throws Exception {
//        fieldToSearch = FIELD_BOW;
//        BooleanQuery q = new BooleanQuery();
//        Term thisTerm;
//        
//        String[] terms = queryFieldAnalyze(analyzer, query.qtitle).split("\\s+");
//        for (String term : terms) {
//            thisTerm = new Term(fieldToSearch, term);
//            Query tq = new TermQuery(thisTerm);
//            q.add(tq, BooleanClause.Occur.SHOULD);
//        }
//        luceneQuery = q;
//
//        return luceneQuery;
//    }

}
