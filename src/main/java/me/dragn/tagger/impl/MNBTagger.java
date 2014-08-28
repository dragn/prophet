package me.dragn.tagger.impl;

import me.dragn.tagger.Tagger;
import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import me.dragn.tagger.prov.DataProvider;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Multinomial Naive Bayes.
 * <p>
 * Based on the article by Rennie et al. [2003]: http://people.csail.mit.edu/jrennie/papers/icml03-nb.pdf
 * User: dsabelnikov
 * Date: 8/25/14
 * Time: 5:34 PM
 */
public class MNBTagger extends Tagger {

    public MNBTagger(DataProvider provider) {
        super(provider);
    }

    @Override
    public void learn(Catalogue catalogue) throws IOException {
        // MNB classifier
        // weight(c,i) = log( theta(c,i) );

        // theta - smoothed maximum likelihood
        // theta(c,i) = (N(c,i) + 1) / (N(c) + count(i))
        // N(c,i) - number of time word i occurs in document of class c

        Keywords keywords = new Keywords();

        // N(C,i) number of occurrences of word i in class C
        Map<String, Map<String, MutableInt>> wordCount = new ConcurrentHashMap<>();

        // N(C) total word occurrences in C
        Map<String, MutableInt> totalCount = new ConcurrentHashMap<>();

        Set<String> words = new HashSet<>();

        catalogue.parallelForEach((tag, docs) -> {

            Map<String, MutableInt> wc = new ConcurrentHashMap<>();

            final MutableInt total = new MutableInt(0);

            docs.forEach(doc -> {
                System.out.println(doc);
                String text = getDocument(doc);
                if (text != null) {
                    bagOfWords(text).forEach((word, count) -> {
                        addToMapValue(wc, word, count.intValue());
                        total.add(count);
                        words.add(word);
                    });
                }
            });

            totalCount.put(tag, total);
            wordCount.put(tag, wc);
        });

        catalogue.tags().forEach(tag -> {
            words.forEach(word -> {
                MutableInt countMutable = wordCount.get(tag).get(word);
                int count = countMutable == null ? 0 : countMutable.getValue();
                keywords.add(tag, new Keyword(word,
                        Math.log10((double) (count + 1) / (totalCount.get(tag).doubleValue() + words.size()))
                ));
            });
        });

        setKeywords(normalize(keywords));
    }

    @Override
    public String tagText(String text) {

        // Probabilities for document to have a tag P(doc|tag)
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
