package me.dragn.tagger.impl;

import me.dragn.tagger.Tagger;
import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.prov.DataProvider;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Transformed Weight-Normalized Complement Naive Bayes.
 * <p>
 * User: dsabelnikov
 * Date: 8/25/14
 * Time: 5:48 PM
 */
public class TWCNBTagger extends Tagger {

    public TWCNBTagger(DataProvider provider) {
        super(provider);
    }

    @Override
    public void learn(Catalogue catalogue) throws IOException {
        Keywords keywords = new Keywords();

        int docLimit = catalogue.map().entrySet().stream().mapToInt(entry -> entry.getValue().size()).min().getAsInt();
        System.out.println("Doc limit: " + docLimit);

        // Instead of counting how many word occurrences in the document of class C,
        // we count word occurrences in the documents of other classes.

        // 1. Count the words and apply TF transform
        // d(i,j) count of word i in document j
        Map<String, Map<String, MutableDouble>> wordCount = new ConcurrentHashMap<>();
        AtomicInteger docCount = new AtomicInteger(0);

        // docCount(i) - number of documents that have word i
        Map<String, MutableInt> wordDocCount = new ConcurrentHashMap<>();

        catalogue.parallelForEach((tag, docs) -> {
            docs.stream().limit(docLimit).forEach(doc -> {
                System.out.println(doc);
                Map<String, MutableDouble> map = new HashMap<>();
                bagOfWords(getDocument(doc)).forEach((word, count) -> {
                    map.put(word, new MutableDouble(Math.log(count.intValue() + 1)));
                    synchronized (wordDocCount) {
                        addToMapValue(wordDocCount, word, 1);
                    }
                });
                wordCount.put(doc, map);
                docCount.incrementAndGet();
            });
        });

        // 2. Apply IDF transform
        wordCount.forEach((doc, words) -> {
            words.forEach((word, count) -> {
                count.setValue(count.getValue() *
                                Math.log(docCount.doubleValue() / wordDocCount.get(word).doubleValue())
                );
            });
        });

        // 2. Count the occurrences of word i not in documents with tag C
        // ~N(C,i)
        Map<String, Map<String, MutableInt>> notWordCount = new ConcurrentHashMap<>(catalogue.tags().size());
        catalogue.tags().forEach(tag -> notWordCount.put(tag, new HashMap<>()));

        // ~N(C) count of words occurrences not in class C
        Map<String, MutableInt> totalCount = new HashMap<>(catalogue.tags().size());

        catalogue.forEach((tag, docs) -> {
            docs.stream().limit(docLimit).forEach((doc) -> {
                wordCount.get(doc).forEach((word, count) -> {
                    catalogue.tags().stream().filter(t -> !tag.equals(t)).forEach(t -> {
                        addToMapValue(notWordCount.get(t), word, count.intValue());
                        addToMapValue(totalCount, t, count.intValue());
                    });
                });
            });
        });

        // release wordCount
        wordCount.clear();

        // 3. Calculate weights
        notWordCount.forEach((tag, words) -> {
            words.forEach((word, count) -> {
                keywords.add(tag, new Keyword(word,
                        Math.log((count.doubleValue() + 1) / (totalCount.get(tag).doubleValue() + words.size()))
                ));
            });
        });

        setKeywords(normalize(keywords));
    }

    @Override
    public String tagText(String text) {
        // Probabilities for doc to have a tag P(doc|tag)
        Map<String, MutableDouble> probByTag = new HashMap<>();

        getKeywords().tags().forEach(tag -> {

            // assume a prior to be equal to zero
            MutableDouble prob = new MutableDouble(0);

            Map<String, Keyword> kws = getKeywords().byTag(tag);

            bagOfWords(text).forEach((word, count) -> {
                Keyword kw = kws.get(word);
                if (kw != null) {
                    prob.add(kw.weight() * count.intValue());
                }
            });
            probByTag.put(tag, prob);

            //System.out.println(tag + ": " + probByTag.get(tag));
        });

        // return a tag with max probability
        return probByTag.entrySet().stream().max((e1, e2) -> - e1.getValue().compareTo(e2.getValue())).get().getKey();
    }
}
