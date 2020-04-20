/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.wapo;

/**
 * **** Washington Post data set used in TREC-News track **** *
 *
 * @author dwaipayan
 */
public class DocField {

    /**
     * Unique document id.
     */
    static final public String WAPO_DOCID = "docid";

    /**
     * URL of the article.
     */
    static final public String WAPO_URL = "url";

    /**
     * Title of the news.
     */
    static final public String WAPO_TITLE = "title";

    /**
     * Author of the article.
     */
    static final public String WAPO_AUTHOR = "author";

    /**
     * Publication date of the article.
     */
    static final public String WAPO_DATE = "date";

    /**
     * Content of the article.
     */
    static final public String WAPO_CONTENT = "contents";

    /**
     * Category of the news article.
     */
    static final public String WAPO_CATEGORY = "category";

//    public enum WapoField {
//        AUTHOR("author"),
//        ARTICLE_URL("article_url"),
//        PUBLISHED_DATE("published_date"),
//        TITLE("title"),
//        FULL_CAPTION("fullCaption"),
//        KICKER("kicker");
//
//        public final String name;
//
//        WapoField(String s) {
//            name = s;
//        }
//    }
}
