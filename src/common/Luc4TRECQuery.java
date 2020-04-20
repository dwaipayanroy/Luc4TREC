/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import static common.CommonMethods.analyzeText;
import static common.CommonMethods.removeSpecialCharacters;
import java.io.StringReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

/**
 * NOTE: to get the final query: <code>luceneQuery.toString(fieldToSearch)</code>
 * @author dwaipayan
 */
public class Luc4TRECQuery {

    public String       qid;            // query id
    public String       q_str;          // string text of the query (stopword removed and stemmed) to be performed search with
    public Query        luceneQuery;    // this is the query with which the search will be performed
    public String       fieldToSearch;  // TODO: to add multi field query search by introducing comma separated multiple values

    public Query getLuceneQuery() { return luceneQuery; }

    /**
     * Returns analyzed queryFieldText from the query
     * @param analyzer
     * @param query_str
     * @return (String) The content of the field
     * @throws Exception 
     */
    public String queryStrAnalyze(String query_str, Analyzer analyzer) throws Exception {

        TokenStream stream = analyzer.tokenStream(null, new StringReader(query_str));
        // public final TokenStream tokenStream(String fieldName, String text)
        //      Returns a TokenStream suitable for fieldName, tokenizing the contents of text.
        // NOTE: we are only analyzing the text, so fieldName is specified as null; anything else would also work 

        StringBuffer localBuff = analyzeText(analyzer, query_str, null);

        return localBuff.toString();
    }

    /**
     * Query analyzed using <code>StandardQueryParser</code>
     * @param query_str
     * @param analyzer
     * @return
     * @throws Exception 
     */
    public Query getAnalyzedQuery(String query_str, Analyzer analyzer) throws Exception {
        StandardQueryParser queryParser = new StandardQueryParser(new EnglishAnalyzer());
        query_str = removeSpecialCharacters(query_str);
        Query luceneQuery = queryParser.parse(query_str, fieldToSearch);

        return luceneQuery;
        
    }

    public BooleanQuery makeBooleanQuery (String queryString, String fieldToSearch, Analyzer analyzer) throws Exception {

        BooleanQuery q = new BooleanQuery();
        Term thisTerm;

        String[] terms = queryStrAnalyze(queryString, analyzer).split("\\s+");
        for (String term : terms) {
            thisTerm = new Term(fieldToSearch, term);
            Query tq = new TermQuery(thisTerm);
            q.add(tq, BooleanClause.Occur.SHOULD);
        }
        luceneQuery = q;        
        BooleanQuery.setMaxClauseCount(8192);
        return q;
    }

    /**
     * @param queryString
     * @param analyzer
     * @throws java.lang.Exception
     * @Deprecated
     * TODO: incomplete.
     * Avoid using this; not complete.
     * Use the other one: <code>public void makeBooleanQuery (String queryString, String fieldToSearch, Analyzer analyzer)</code>
     */
    public void makeBooleanQuery (String queryString, Analyzer analyzer) throws Exception {
        makeBooleanQuery(queryString, this.fieldToSearch, analyzer);
    }


//    public Query getBooleanQuery(Analyzer analyzer, String query_str) throws Exception {
//
//        luceneQuery = makeBooleanQuery(query_str, analyzer);
//
//        return luceneQuery;
//    }


}
