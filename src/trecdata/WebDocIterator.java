/**
 * Drops content of <DOCNO>, <DOCHDR>; 
 * Indexes other content, dropping all HTML-like tags and removing URLs; <p>
 * Tested for WT2G and GOV2 Collection.
 */

package trecdata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import static common.CommonVariables.FIELD_BOW;
import static common.CommonVariables.FIELD_ID;
/*import org.apache.lucene.document.TextField;*/

/**
 *
 * @author dwaipayan
 */
public class WebDocIterator implements Iterator<Document> {

	protected BufferedReader rdr;
	protected boolean at_eof = false;
        Analyzer    analyzer;
        String      toStore;            // YES / NO; to be read from prop file; default is 'NO'
        String      storetermVector;         // NO / YES / WITH_POSITIONS / WITH_OFFSETS / WITH_POSITIONS_OFFSETS; to be read from prop file; default - YES
        String      dumpPath;
        Properties  prop;

	public WebDocIterator(File file) throws FileNotFoundException {
            rdr = new BufferedReader(new FileReader(file));
//            System.out.println("Reading " + file.toString());
	}

	public WebDocIterator(File file, CommandLineIndexing obj) throws FileNotFoundException, IOException {
            rdr = new BufferedReader(new FileReader(file));
            this.analyzer = obj.analyzer;
            this.prop = prop;
	}

	public WebDocIterator(File file, Analyzer analyzer, Properties prop) throws FileNotFoundException, IOException {
            rdr = new BufferedReader(new FileReader(file));
            this.analyzer = analyzer;
            this.prop = prop;
	}

        public WebDocIterator(File file, Analyzer analyzer, String toStore, String dumpPath) throws FileNotFoundException{
            rdr = new BufferedReader(new FileReader(file));
            this.analyzer = analyzer;
            this.toStore = toStore;
            this.dumpPath = dumpPath;
        }

        @Override
	public boolean hasNext() {
            return !at_eof;
	}

        /**
         * Removes the HTML tags from 'str' and returns the resultant string
         * @param str
         * @return 
         */
        public String removeHTMLTags(String str) {
//            String tagPatternStr = "<\\p{Punct}*\\s*[a-zA-Z0-9 ]*\\s*\\p{Punct}*[^>]>";
            String tagPatternStr = "<[^>\\n]*[>\\n]";
            Pattern tagPattern = Pattern.compile(tagPatternStr);

            Matcher m = tagPattern.matcher(str);
            return m.replaceAll(" ");
        }

        /**
         * Removes URLs from 'str' and returns the resultant string
         * @param str
         * @return 
         */
        public String removeURL(String str) {
            String urlPatternStr = "\\b((https?|ftp|file)://|www)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
            Pattern urlPattern = Pattern.compile(urlPatternStr);

            Matcher m = urlPattern.matcher(str);
            return m.replaceAll(" ");
        }

        /**
         * Returns the next document in the collection, setting FIELD_ID and FIELD_BOW.
         * @return 
         */
	@Override
	public Document next() {
            Document doc = new Document();
            StringBuffer sb = new StringBuffer();

            try {
                String line;
                boolean in_doc = false;
                boolean in_content = false;
                int in_content_tag = 0;
                String doc_no = null;

                while (true) {
                    line = rdr.readLine();

                    if (line == null) {
                        at_eof = true;
                        break;
                    }
                    else
                        line = line.trim();

                    // <DOC>
                    if (!in_doc) {
                        if (line.startsWith("<DOC>"))
                            in_doc = true;
                        else
                            continue;
                    }
                    if (line.contains("</DOC>")) {
                        in_doc = false;
                        sb.append(line);
                        break;
                    }
                    // </DOC>

                    // <DOCNO>
                    if(line.startsWith("<DOCNO>")) {
                        doc_no = line;
                        while(!line.endsWith("</DOCNO>")) {
                            line = rdr.readLine().trim();
                            doc_no = doc_no + line;
                        }
                        doc_no = doc_no.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();
                        doc.add(new StringField(FIELD_ID, doc_no, Field.Store.YES));
                        continue;   // start reading the next line
                    }
                    // </DOCNO>

                    // +++ ignoring DOCOLDNO and DOCHDR
                    // <DOCOLDNO>
                    if(line.startsWith("<DOCOLDNO>")) {
                        while(!line.endsWith("</DOCOLDNO>"))
                            line = rdr.readLine().trim();
                        continue;   // start reading the next line
                    } // </DOCOLDNO>
                    // <DOCHDR>
                    if(line.startsWith("<DOCHDR>")) {
                        while(!line.endsWith("</DOCHDR>"))
                            line = rdr.readLine().trim();
                        continue;   // start reading the next line
                    } // </DOCHDR>
                    // --- ignored DOCOLDNO and DOCHDR

                    sb.append(line);
                    sb.append(" ");
                }

                if (sb.length() > 0) {
                    //doc.add(new TextField(CommandLineIndexing.FIELD_BOW, sb.toString(), Field.Store.YES));
                    String txt = removeHTMLTags(sb.toString()); // remove all html-like tags (e.g. <xyz>)
                    txt = removeURL(txt);

                    StringBuffer tokenizedContentBuff = new StringBuffer();

                    TokenStream stream = analyzer.tokenStream(FIELD_BOW, 
                        new StringReader(txt));
                    CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
                    stream.reset();

                    while (stream.incrementToken()) {
                        String term = termAtt.toString();
                        term = term.replaceAll(":", " ");
                        if(!term.equals("nbsp"))
                            tokenizedContentBuff.append(term).append(" ");
                    }

                    stream.end();
                    stream.close();

                    String toIndex = tokenizedContentBuff.toString();
                    doc.add(new Field(FIELD_BOW, toIndex, 
                        Field.Store.valueOf(toStore), Field.Index.ANALYZED, Field.TermVector.YES));

                    if(null != dumpPath) {
                        FileWriter fw = new FileWriter(dumpPath, true);
                        BufferedWriter bw = new BufferedWriter(fw);
                        bw.write(toIndex+"\n");
                        bw.close();
                    }
                }
            } catch (IOException e) {
                doc = null;
            }
            return doc;
	}

        /**
         * This is only used for dumping the content of the files; NO INDEXING
         * @return 
         */
	public Document dumpNextDoc() {
            Document doc = new Document();
            StringBuffer sb = new StringBuffer();

            try {
                String line;
                boolean in_doc = false;
                boolean in_content = false;
                int in_content_tag = 0;
                String doc_no = null;

                while (true) {
                    line = rdr.readLine();

                    if (line == null) {
                        at_eof = true;
                        break;
                    }
                    else
                        line = line.trim();

                    // <DOC>
                    if (!in_doc) {
                        if (line.startsWith("<DOC>"))
                            in_doc = true;
                        else
                            continue;
                    }
                    if (line.contains("</DOC>")) {
                        in_doc = false;
                        sb.append(line);
                        break;
                    }
                    // </DOC>

                    // <DOCNO>
                    if(line.startsWith("<DOCNO>")) {
                        doc_no = line;
                        while(!line.endsWith("</DOCNO>")) {
                            line = rdr.readLine().trim();
                            doc_no = doc_no + line;
                        }
                        doc_no = doc_no.replace("<DOCNO>", "").replace("</DOCNO>", "").trim();
                        doc.add(new StringField(FIELD_ID, doc_no, Field.Store.YES));
                        continue;   // start reading the next line
                    }
                    // </DOCNO>

                    // +++ ignoring DOCOLDNO and DOCHDR
                    // <DOCOLDNO>
                    if(line.startsWith("<DOCOLDNO>")) {
                        while(!line.endsWith("</DOCOLDNO>"))
                            line = rdr.readLine().trim();
                        continue;   // start reading the next line
                    } // </DOCOLDNO>
                    // <DOCHDR>
                    if(line.startsWith("<DOCHDR>")) {
                        while(!line.endsWith("</DOCHDR>"))
                            line = rdr.readLine().trim();
                        continue;   // start reading the next line
                    } // </DOCHDR>
                    // --- ignored DOCOLDNO and DOCHDR

                    sb.append(line);
                    sb.append(" ");
                }

                if (sb.length() > 0) {
                    String txt = removeHTMLTags(sb.toString()); // remove all html tags
                    txt = removeURL(txt);

                    StringBuffer tokenizedContentBuff = new StringBuffer();

                    TokenStream stream = analyzer.tokenStream(FIELD_BOW, 
                        new StringReader(txt));
                    CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
                    stream.reset();

                    while (stream.incrementToken()) {
                        String term = termAtt.toString();
                        tokenizedContentBuff.append(term).append(" ");
                    }

                    stream.end();
                    stream.close();

                    FileWriter fileWritter = new FileWriter(prop.getProperty("dumpPath"), true);
                    BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                    bufferWritter.write(tokenizedContentBuff.toString()+"\n");
                    bufferWritter.close();
                }
            } catch (IOException e) {
                doc = null;
            }
            return doc;
	}

        @Override
	public void remove() {
	}
}
