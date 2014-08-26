package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Multinomial Naive Bayes.
 * <p>
 * User: dsabelnikov
 * Date: 8/25/14
 * Time: 5:34 PM
 */
public class MNBTagger extends Tagger {

    @Override
    public void learn(Catalogue catalogue) throws IOException {
        // MNB classifier
        // weight(c,i) = log( theta(c,i) );

        // theta - smoothed maximum likelihood
        // theta(c,i) = (N(c,i) + 1) / (N(c) + count(i))
        // N(c,i) - number of time word i occurs in document of class c

        Keywords keywords = new Keywords();

        catalogue.parallelForEach((tag, sites) -> {
            // N(C,i) number of occurrences of word i in class C
            Map<String, MutableInt> wordCount = new ConcurrentHashMap<>();

            // N(C) total word occurrences in C
            final MutableInt total = new MutableInt(0);

            // alpha - number of words
            final MutableInt alpha = new MutableInt(0);

            sites.forEach(site -> {
                System.out.println(site);
                bagOfWords(getSiteText(site)).forEach((word, count) -> {
                    addToMapValue(wordCount, word, count.intValue());
                    total.add(count);
                    alpha.increment();
                });
            });

            wordCount.forEach((word, count) -> {
                keywords.add(tag, new Keyword(word,
                        Math.log((count.doubleValue() + 1) / (total.doubleValue() + alpha.doubleValue()))
                ));
            });
        });

        setKeywords(normalize(keywords));
    }

    @Override
    public String tagText(String text) {

        // Probabilities for site to have a tag P(site|tag)
        Map<String, MutableDouble> probByTag = new HashMap<>();

        getKeywords().tags().forEach(tag -> {

            // assume a prior to be equal to zero
            MutableDouble prob = new MutableDouble(0);

            Map<String, Keyword> kws = getKeywords().byTag(tag);

            bagOfWords(text).forEach((word, count) -> {
                Keyword kw = kws.get(word);
                if (kw != null) {
                    prob.add(kw.weight() * count.doubleValue());
                }
            });
            probByTag.put(tag, prob);

            //System.out.println(tag + ": " + probByTag.get(tag));
        });

        // return a tag with max probability
        return probByTag.entrySet().stream().max((e1, e2) -> e1.getValue().compareTo(e2.getValue())).get().getKey();
    }
}
