/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package common.wapo;

import common.Luc4TRECQuery;

/**
 *
 * @author dwaipayan
 */
public class WapoQuery extends Luc4TRECQuery {

//    public String       q_num;
    public String       q_docid;
    public String       q_url;

    public String       q_title;
    public String       q_content;

    @Override
    public String toString() {
        return qid + "\t" + q_docid;
    }

    public String valueOf(String field) {
        String value="";
        
        switch(field) {
            case "title":
                value = q_title;
                break;
            case "contents":
                value = this.q_content;
                break;
        }
        return value;
    }
}
