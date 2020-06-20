/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author dwaipayan
 */
/**
 * Per document retrieval information.
 */
public class Hit {

    /**
     * Lucene Docid.
     */
    public int id;
    /**
     * Doc name. (unused)
     */
    public String docno;
    /**
     * Doc score.
     */
    public double score;
    /**
     * per-term score of this document.
     */
    public Map<String, Double> termScores;

    /**
     * Initialize the docid with `id` and the termScore HashMap.
     * @param id 
     */
    public Hit(int id) {
        this.id = id;
        this.termScores = new HashMap<>();
    }

    public void setTermScore(String term, double score) {
        this.termScores.put(term, score);
    }

    public double getTermScore(String term) {
        return termScores.containsKey(term) ? termScores.get(term) : 0.0;
    }
} // class Hit ends 
