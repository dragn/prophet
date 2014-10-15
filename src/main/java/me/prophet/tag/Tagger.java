package me.prophet.tag;

import me.prophet.data.Catalogue;
import me.prophet.data.Keyword;
import me.prophet.data.Keywords;
import me.prophet.prov.DataProvider;
import me.prophet.util.KeywordsFetcher;
import me.prophet.util.TaggerTest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.util.*;
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

    protected boolean verbose = false;

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

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
            String text = provider.getDocument(doc);
            if (text != null) {
                String calc = tagText(text);
                System.out.printf("%s: %s\n", doc, StringUtils.isEmpty(calc) ? "Не уверен..." : calc);
                if (StringUtils.isNotEmpty(calc)) {
                    synchronized (myCatalogue) {
                        myCatalogue.add(calc, doc);
                    }
                }
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
            double sum = words.entrySet().stream().mapToDouble(entry -> Math.abs(entry.getValue().weight())).sum();
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

    protected void addToMapValueDouble(Map<String, MutableDouble> map, String key, double add) {
        MutableDouble value = map.get(key);
        if (value == null) {
            map.put(key, new MutableDouble(add));
        } else {
            value.add(add);
        }
    }

    protected String getSigmaBest(Map<String, MutableDouble> probByTag, double sigma) {
        double mean = probByTag.values().stream().mapToDouble(MutableDouble::getValue).average().getAsDouble();

        // Standard deviation
        double stDev = Math.sqrt(probByTag.values().stream().mapToDouble(
                prob -> Math.pow(prob.doubleValue() - mean, 2)).sum() / probByTag.size());

        //System.out.printf("Mean: %f, st.dev.: %f\n", mean, stDev);

        // Check for a 4-sigma certainty
        Optional<Map.Entry<String, MutableDouble>> maxEntry = probByTag.entrySet().stream()
                .filter(entry -> entry.getValue().doubleValue() > (mean + sigma * stDev))
                .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()));

        return maxEntry.isPresent() ? maxEntry.get().getKey() : null;
    }

    protected void incrementMapValue(Map<String, MutableInt> map, String key) {
        addToMapValue(map, key, 1);
    }

    public abstract String tagText(String text);

    public List<String> multitagText(String text) {
        return Arrays.asList(tagText(text));
    }
}
