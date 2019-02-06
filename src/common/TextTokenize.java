/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.util.AttributeFactory;

/**
 *
 * @author dwaipayan
 */
public class TextTokenize {

    public static void main(String[] args) throws IOException {
        // Define your attribute factory (or use the default) - same between 4.x and 5.x
        AttributeFactory factory = AttributeFactory.DEFAULT_ATTRIBUTE_FACTORY;

        // Create the tokenizer and prepare it for reading
        //  Lucene 5.x
//        StandardTokenizer tokenizer = new StandardTokenizer(factory);
        String text = "sgra.jpl.nasa.gov, U.S.A, www.isical.ac.in:8080/~mandar/dwaipayan";
        Analyzer analyzer = new EnglishAnalyzer();
        System.out.println(CommonMethods.analyzeText(analyzer, text, ""));
    }
}
