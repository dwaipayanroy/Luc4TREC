/*
 * Heavily inspired from: https://github.com/gtsherman/lucene/blob/master/src/main/java/org/retrievable/lucene/searching/support/Hits.java
 */
package common;

import common.Hit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author dwaipayan
 */
public class Hits implements Iterable<Hit> {

    /**
     * For a query, Hit information of set of all retrieved documents.
     * (lucene-docid, Hit)
     */
    private Map<Integer, Hit> hits;
	
    public Hits() {
        this.hits = new HashMap<>();
    }

    /**
     * Combine per-query term retrieval lists. 
     * @param hits 
     */
    public void combineHits(Hits hits) {

        for (Integer docid : hits.hits.keySet()) {
            Hit hit = hits.getHit(docid);
            for (String term : hit.termScores.keySet()) {
                setTermScore(term, hit.getTermScore(term), docid);
            }
        }
    }

    /**
     * Set query-term score for term in doc.
     * @param term
     * @param score
     * @param doc 
     */
    public void setTermScore(String term, double score, int doc) {
        if (!hits.containsKey(doc)) {
            hits.put(doc, new Hit(doc));
        }
        hits.get(doc).setTermScore(term, score);
    }

    public Hit getHit(int id) {
        return hits.containsKey(id) ? hits.get(id) : new Hit(id);
    }

    @Override
    public Iterator<Hit> iterator() {
        return hits.values().iterator();
    }

    /**
     * Returns the ranked documents based on the doc (hit) scores.
     * @return 
     */
    public List<Hit> getRankedHits() {
        List<Hit> rankedHits = new ArrayList<>(hits.values());
        Collections.sort(rankedHits, new Comparator<Hit>() {
            @Override
            public int compare(Hit h1, Hit h2) {
                if (h1.score > h2.score) {
                    return -1;
                }
                if (h1.score < h2.score) {
                    return 1;
                }
                return 0;
            }
        });
        return rankedHits;
    }
} // class Hits ends