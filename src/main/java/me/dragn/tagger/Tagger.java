package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.prov.DataProvider;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import me.dragn.tagger.util.KeywordsFetcher;
import org.apache.commons.lang3.mutable.MutableInt;

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

    private DataProvider provider;

    public Tagger(DataProvider provider) {
        this.provider = provider;
    }

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

    /**
     * Retrieves a document from DataProvider
     */
    public String getDocument(String key) {
        return provider.getDocument(key);
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
        input.forEach((tag, docs) -> docs.parallelStream().forEach(doc -> {
            String calc = tagText(provider.getDocument(doc));
            System.out.printf("%s: %s\n", doc, calc);
            synchronized (myCatalogue) {
                myCatalogue.add(calc, doc);
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

    protected Keywords normalize(Keywords keywords) {
        Keywords kws = new Keywords();
        keywords.forEach((tag, words) -> {
            double sum = words.entrySet().stream().mapToDouble(entry -> entry.getValue().weight()).sum();
            words.forEach((word, kw) -> {
                kws.add(tag, new Keyword(word, kw.weight() / sum));
            });
        });
        return kws;
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
