package com.github.rnewson.couchdb.lucene;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cn.ChineseAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;

public final class AnalyzerCache {

    private static Analyzer createAnalyzer(String name) {
        if (name.equals("BRAZILIAN")) {
            return new BrazilianAnalyzer();
        } else if (name.equals("CHINESE")) {
            return new ChineseAnalyzer();
        } else if (name.equals("CJK")) {
            return new CJKAnalyzer();
        } else if (name.equals("CZECH")) {
            return new CzechAnalyzer();
        } else if (name.equals("DUTCH")) {
            return new DutchAnalyzer();
        } else if (name.equals("ENGLISH")) {
            return new StandardAnalyzer();
        } else if (name.equals("FRENCH")) {
            return new FrenchAnalyzer();
        } else if (name.equals("GERMAN")) {
            return new GermanAnalyzer();
        } else if (name.equals("KEYWORD")) {
            return new KeywordAnalyzer();
        } else if (name.equals("PORTER")) {
            return new PorterStemAnalyzer();
        } else if (name.equals("RUSSIAN")) {
            return new RussianAnalyzer();
        } else if (name.equals("SIMPLE")) {
            return new SimpleAnalyzer();
        } else if (name.equals("STANDARD")) {
            return new StandardAnalyzer();
        } else if (name.equals("THAI")) {
            return new ThaiAnalyzer();
        } else {
            throw new RuntimeException("unexpected analyzer name: \"" + name + "\"");
        }
    }

    private static final class PorterStemAnalyzer extends Analyzer {
        public TokenStream tokenStream(final String fieldName, final Reader reader) {
            return new PorterStemFilter(new LowerCaseTokenizer(reader));
        }
    }

    private static Map MAP = new HashMap();

    public static Analyzer getAnalyzer(String name) {
        name = name.toUpperCase();
        Analyzer result = (Analyzer) MAP.get(name);
        if (result != null) {
            return result;
        }

        result = createAnalyzer(name);

        MAP.put(name, result);
        return result;
    }
}
