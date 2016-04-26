/**
 * Index TREC document collections with tags as specified in:
 *      <a href="resources/trec-toRead-tags">tag-list</a> <p>
 * Tested for TREC1-8.
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
public class TrecDocIterator implements Iterator<Document> {

	protected BufferedReader rdr;
	protected boolean at_eof = false;
        Analyzer analyzer;
        String      toStore;
        String      dumpPath;
        Properties prop;

	public TrecDocIterator(File file) throws FileNotFoundException {
            rdr = new BufferedReader(new FileReader(file));
//            System.out.println("Reading " + file.toString());
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

                do {
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
                    if (line.endsWith("</DOC>")) {
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

                    // <content-tag>
//                    Pattern content_tag = Pattern.compile("<[a-zA-Z]+>");
                    Pattern content_tag = Pattern.compile("(?m)\\s*<[a-zA-Z]+>\\s*(.*)</", Pattern.DOTALL);
                    Matcher m = content_tag.matcher(line);
                    if (m.find()) {
                        if (line.contains("<TTL>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<TITLE>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<TEXT>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<SUMMARY>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<SPECS>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<HEADLINE>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<LP>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<LEADPARA>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<HL>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<HEADLINE>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<HEAD>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<H3>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<CATEGORY>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<CAPTION>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<CORRECTION>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<TXT5>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<PHRASE>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.contains("<TI>")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        if (line.startsWith("<TEXT")){
                            in_content = true;
                            in_content_tag++;
                            sb.append(line);
                            sb.append(" ");
                        }
                        else{
                            //useless tag; continue reading next line
                            continue;
                        }
                    }

                    else {      // line is not starting with a <tag>
                    // Appending the content in the buffer
                        if(in_content) {
                            sb.append(line);
                            sb.append(" ");
                        }
                    }

                    
                    if (line.contains("</TTL>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</TITLE>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</TEXT>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</SUMMARY>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</SPECS>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</HEADLINE>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</LP>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</LEADPARA>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</HL>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</HEADLINE>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</HEAD>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</H3>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</CATEGORY>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</CAPTION>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</CORRECTION>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</TXT5>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</PHRASE>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    if (line.contains("</TI>")){
                        in_content_tag--;
                        if (in_content_tag <= 0)
                            in_content = false;
                    }
                    // </content-tag>

                } while(in_doc);

                if (sb.length() > 0) {
                    //doc.add(new TextField(CommandLineIndexing.FIELD_BOW, sb.toString(), Field.Store.YES));
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

                    String toIndex = tokenizedContentBuff.toString().
                        replaceAll(":", " ");
                    doc.add(new Field(FIELD_BOW, tokenizedContentBuff.toString(), 
                        Field.Store.valueOf(toStore), Field.Index.ANALYZED, Field.TermVector.YES));

                    if(null != dumpPath) {
//                    if(prop.containsKey("dumpPath")) {
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
                Pattern docno_tag = Pattern.compile("<DOCNO>\\s*(\\S+)\\s*<");
                boolean in_doc = false;
                while (true) {
                    line = rdr.readLine();
                    if (line == null) {
                        at_eof = true;
                        break;
                    }
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

                    Matcher m = docno_tag.matcher(line);
                    if (m.find()) {
                        String docno = m.group(1);
                    }
                    else {
                        sb.append(" ");
                        sb.append(line);
                    }
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
