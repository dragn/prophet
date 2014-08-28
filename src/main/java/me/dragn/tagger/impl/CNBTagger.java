package me.dragn.tagger.impl;

import me.dragn.tagger.Tagger;
import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import me.dragn.tagger.prov.DataProvider;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Complement Naive Bayes implementation.
 * Keyword weights represent the probability of a given word to be not in document of given class.
 * Thus the subtraction in the classification rule instead of addition.
 * <p>
 * User: dsabelnikov
 * Date: 8/25/14
 * Time: 5:48 PM
 */
public class CNBTagger extends Tagger {

    private final int SITE_LIMIT = 50;

    public CNBTagger(DataProvider provider) {
        super(provider);
    }

    @Override
    public void learn(Catalogue catalogue) throws IOException {
        Keywords keywords = new Keywords();

        // Set of all encountered words
        Set<String> allWords = new HashSet<>();

        // Instead of counting how many word occurrences in the document of class C,
        // we count word occurrences in the documents of other classes.

        // 1. Count the occurrences of word i in documents with tag C
        // N(C,i) number of occurrences of word i in class C
        Map<String, Map<String, MutableInt>> wordCount = new ConcurrentHashMap<>();

        catalogue.parallelForEach((tag, docs) -> {
            Map<String, MutableInt> wc = new ConcurrentHashMap<>();
            Collections.shuffle((List) docs);
            docs.stream().limit(SITE_LIMIT).forEach(doc -> {
                System.out.println(doc);
                String text = getDocument(doc);
                if (text != null) {
                    bagOfWords(text).forEach((word, count) -> {
                        addToMapValue(wc, word, count.intValue());
                        allWords.add(word);
                    });
                }
            });
            wordCount.put(tag, wc);
        });

        // 2. Count the occurrences of word i not in documents with tag C
        // ~N(C,i)
        Map<String, Map<String, MutableInt>> notWordCount = new ConcurrentHashMap<>(catalogue.tags().size());
        catalogue.tags().forEach(tag -> notWordCount.put(tag, new HashMap<>()));

        // ~N(C) count of words occurrences not in class C
        Map<String, MutableInt> totalCount = new HashMap<>(catalogue.tags().size());

        wordCount.forEach((tag, words) -> {
            words.forEach((word, count) -> {
                catalogue.tags().stream().filter(t -> !tag.equals(t)).forEach(t -> {
                    addToMapValue(notWordCount.get(t), word, count.intValue());
                    addToMapValue(totalCount, t, count.intValue());
                });
            });
        });

        // release wordCount
        wordCount.clear();

        // 3. Calculate weights as log10((~N(C,i) + 1) / (~N(c) + allWords.size())
        catalogue.tags().forEach(tag -> {
            allWords.forEach(word -> {
                MutableInt countMutable = notWordCount.get(tag).get(word);
                int count = countMutable == null ? 0 : countMutable.getValue();
                keywords.add(tag, new Keyword(word,
                        Math.log10((double) (count + 1) / (totalCount.get(tag).doubleValue() + allWords.size()))
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
                    prob.subtract(kw.weight() * count.doubleValue());
                }
            });
            probByTag.put(tag, prob);

            //System.out.println(tag + ": " + probByTag.get(tag));
        });

        return getSigmaBest(probByTag, 2);
    }
}
