package me.dragn.tagger.impl;

import me.dragn.tagger.Tagger;
import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import me.dragn.tagger.prov.DataProvider;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transformed Weight-Normalized Complement Naive Bayes.
 * <p>
 * User: dsabelnikov
 * Date: 8/25/14
 * Time: 5:48 PM
 */
public class TWCNBTagger extends Tagger {

    private final int SITE_LIMIT = 50;

    public TWCNBTagger(DataProvider provider) {
        super(provider);
    }

    @Override
    public void learn(Catalogue catalogue) throws IOException {
        Keywords keywords = new Keywords();

        // Set of all words
        Set<String> allWords = new HashSet<>();

        // 1. Count word occurrences for each document

        // d(i,j) - number of occurrences of word i in document j
        Map<String, Map<String, MutableDouble>> docCount = new HashMap<>();

        catalogue.parallelForEach((tag, docs) -> {
            Collections.shuffle((List)docs);
            docs.stream().limit(SITE_LIMIT).forEach(docKey -> {
                System.out.println(docKey);
                Map<String, MutableDouble> currentDocCount = new HashMap<>();
                bagOfWords(getDocument(docKey)).forEach((word, count) -> {
                    addToMapValueDouble(currentDocCount, word, count.intValue());
                    allWords.add(word);
                });
                docCount.put(docKey, currentDocCount);
            });
        });

        // 2. Calculate delta(i) - number of document which has word i.
        Map<String, Long> delta = new HashMap<>();
        allWords.forEach(word -> {
            long count = docCount.entrySet().stream().filter(entry -> entry.getValue().containsKey(word)).count();
            delta.put(word, count);
        });

        // 3. Apply TF-IDF transform. d(i,j) = log(d(i,j) + 1) * log((document-count) / delta(i)),
        docCount.forEach((docKey, words) -> {
            words.forEach((word, count) -> {
                count.setValue(Math.log10(count.getValue() + 1) * Math.log10(docCount.size() / delta.get(word)));
            });
        });

        // 4. Normalize
        docCount.forEach((docKey, words) -> {
            Double sum = words.entrySet().stream().mapToDouble(entry -> entry.getValue().toDouble()).sum();
            words.forEach((word, count) -> {
                count.setValue(count.getValue() / sum);
            });
        });

        // 5. Calculate ~N(c,i)
        // ~N(C,i)
        Map<String, Map<String, MutableDouble>> notWordCount = new ConcurrentHashMap<>(catalogue.tags().size());
        catalogue.tags().forEach(tag -> notWordCount.put(tag, new HashMap<>()));

        // ~N(C) count of words occurrences not in class C
        Map<String, MutableDouble> totalCount = new HashMap<>(catalogue.tags().size());

        catalogue.tags().forEach(tag -> {
            catalogue.map().entrySet().stream().filter(entry -> !entry.getKey().equals(tag)).forEach(entry -> {
                entry.getValue().forEach(docKey -> {
                    Map<String, MutableDouble> docs = docCount.get(docKey);
                    if (docs != null) {
                        docs.forEach((word, count) -> {
                            addToMapValueDouble(notWordCount.get(tag), word, count.doubleValue());
                            addToMapValueDouble(totalCount, tag, count.doubleValue());
                        });
                    }
                });
            });
        });

        // 6. Calculate weights as log10((~N(C,i) + 1) / (~N(c) + allWords.size())
        catalogue.tags().forEach(tag -> {
            allWords.forEach(word -> {
                MutableDouble countMutable = notWordCount.get(tag).get(word);
                double count = countMutable == null ? 0 : countMutable.getValue();
                keywords.add(tag, new Keyword(word,
                        Math.log10((count + 1) / (totalCount.get(tag).doubleValue() + allWords.size()))
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

            System.out.println(tag + ": " + probByTag.get(tag));
        });

        return getSigmaBest(probByTag, 0);
    }
}
