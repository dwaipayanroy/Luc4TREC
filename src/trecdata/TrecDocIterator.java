/**
 * Index TREC document collections dropping all the HTML tags and URLs
 * Removes ':', '_'
 * Tested for TREC Disk 1-5.
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
import java.util.HashMap;
import java.util.Map;
/*import org.apache.lucene.document.TextField;*/

/**
 *
 * @author dwaipayan
 */
public class TrecDocIterator implements Iterator<Document> {

	protected BufferedReader rdr;
	protected boolean at_eof = false;
        Analyzer analyzer;
        String      toStore;
        String      dumpPath;
        Properties prop;

	public TrecDocIterator(File file) throws FileNotFoundException {
            rdr = new BufferedReader(new FileReader(file));
	}

	public TrecDocIterator(File file, CommandLineIndexing obj) throws FileNotFoundException, IOException {
            rdr = new BufferedReader(new FileReader(file));
            this.analyzer = obj.analyzer;
            this.prop = prop;
	}

	public TrecDocIterator(File file, Analyzer analyzer, Properties prop) throws FileNotFoundException, IOException {
            rdr = new BufferedReader(new FileReader(file));
            this.analyzer = analyzer;
            this.prop = prop;
	}

        public TrecDocIterator(File file, Analyzer analyzer, String toStore, String dumpPath) throws FileNotFoundException{
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

            // +++ For replacing characters- ':','_'
            Map<String, String> replacements = new HashMap<String, String>() {{
                put(":", " ");
                put("_", " ");
            }};
            // create the pattern joining the keys with '|'
            String regExp = ":|_";
            Pattern p = Pattern.compile(regExp);
            // --- For replacing characters- ':','_'

            try {
                String line;
                boolean in_doc = false;
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

                    sb.append(line);
                    sb.append(" ");
                }

                if (sb.length() > 0) {
                    //doc.add(new TextField(CommandLineIndexing.FIELD_BOW, sb.toString(), Field.Store.YES));
                    String txt = removeHTMLTags(sb.toString()); // remove all html-like tags (e.g. <xyz>)
                    txt = removeURL(txt);

                    // +++ For replacing characters- ':','_'
                    StringBuffer temp = new StringBuffer();
                    Matcher m = p.matcher(txt);
                    while (m.find()) {
                        String value = replacements.get(m.group(0));
                        if(value != null)
                            m.appendReplacement(temp, value);
                    }
                    m.appendTail(temp);
                    txt = temp.toString();
                    // --- For replacing characters- ':','_'

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

            // +++ For replacing characters- ':','_'
            Map<String, String> replacements = new HashMap<String, String>() {{
                put(":", " ");
                put("_", " ");
            }};
            // create the pattern joining the keys with '|'
            String regExp = ":|_";
            Pattern p = Pattern.compile(regExp);
            // --- For replacing characters- ':','_'

            try {
                String line;
                boolean in_doc = false;
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
                    if (line.startsWith("</DOC>")) {
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

                    sb.append(" ");
                    sb.append(line);
                }
                if (sb.length() > 0) {
                    String txt = removeHTMLTags(sb.toString()); // remove all html tags
                    txt = removeURL(txt);

                    // +++ For replacing characters- ':','_'
                    StringBuffer temp = new StringBuffer();
                    Matcher m = p.matcher(txt);
                    while (m.find()) {
                        String value = replacements.get(m.group(0));
                        if(value != null)
                            m.appendReplacement(temp, value);
                    }
                    m.appendTail(temp);
                    txt = temp.toString();
                    // --- For replacing characters- ':','_'

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
