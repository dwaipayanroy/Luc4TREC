/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dwaipayan
 */
public class KnownRelevance {

    String          queryid;
    List<String>    relevant;
    List<String>    nonrelevant;

    public KnownRelevance() {
        relevant = new ArrayList<>();
        nonrelevant = new ArrayList<>();
    }

    /**
     * Returns the list of relevant/non-relevant documents for a query.
     * @param relevance 0: non-relevant, Non-0: Relevant.)
     * @return A string containing list of relevant/non-relevant documents for a query.
     */
    public String toString(int relevance) {
        if(relevance == 0)
            return nonrelevant.toString();
        else
            return relevant.toString();
    }
}
