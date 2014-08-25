package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.data.Keywords;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/**
 * Naive Bayes Classifier
 * <p>
 * User: dsabelnikov
 * Date: 8/20/14
 * Time: 2:49 PM
 */
public abstract class Tagger {

    private Keywords keywords;

    public Keywords getKeywords() {
        return keywords;
    }

    public void setKeywords(Keywords keywords) {
        this.keywords = keywords;
    }

    /**
     * Read already learned keywords from file.
     */
    public void fromFile(String file) {
        keywords = Keywords.fromFile(file);
    }

    /**
     * Learn keywords by parsing sites from catalogue.
     */
    public abstract void learn(Catalogue catalogue) throws IOException;

    protected String getSiteText(String site) {
        Document doc = Crawler.getWithRetry(site, 5000, 3);
        return doc != null ? doc.text() : "";
    }

    /**
     * Store learned keywords to file.
     */
    public void toFile(String file) throws IOException {
        if (keywords != null) keywords.toFile(file);
    }

    /**
     * Test this tagger against provided catalogue.
     * Tag each site in input, build own Catalogue, measure catalogue relevancy (e.g. F1 score).
     */
    public void test(Catalogue input) {
        Catalogue myCatalogue = new Catalogue();
        input.forEach((tag, sites) -> sites.parallelStream().forEach(site -> {
            String calc = tagText(getSiteText(site));
            System.out.printf("%s: %s\n", site, calc);
            synchronized (myCatalogue) {
                myCatalogue.add(calc, site);
            }
        }));
        TaggerTest.score(input, myCatalogue);
    }

    protected Map<String, MutableInt> bagOfWords(String text) {
        Map<String, MutableInt> words = new HashMap<>();
        forEachWord(text, word -> incrementMapValue(words, word));
        return words;
    }

    protected void forEachWord(String text, Consumer<String> cons) {
        Matcher matcher = KeywordsFetcher.wordPattern.matcher(text);
        while (matcher.find()) {
            cons.accept(matcher.group().toLowerCase());
        }
    }

    protected void addToMapValue(Map<String, MutableInt> map, String key, int add) {
        MutableInt value = map.get(key);
        if (value == null) {
            map.put(key, new MutableInt(add));
        } else {
            value.add(add);
        }
    }

    protected void incrementMapValue(Map<String, MutableInt> map, String key) {
        addToMapValue(map, key, 1);
    }

    public abstract String tagText(String text);

}
