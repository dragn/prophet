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
 * Complement Naive Bayes implementation.
 * Keyword weights represent the probability of a given word to be not in document of given class.
 * Thus the subtraction in the classification rule instead of addition.
 * <p>
 * User: dsabelnikov
 * Date: 8/25/14
 * Time: 5:48 PM
 */
public class CNBTagger extends Tagger {

    @Override
    public void learn(Catalogue catalogue) throws IOException {
        Keywords keywords = new Keywords();

        // Instead of counting how many word occurrences in the document of class C,
        // we count word occurrences in the documents of other classes.

        // 1. Count the occurrences of word i in sites with tag C
        // N(C,i) = wordCount.get(C).get(i)
        Map<String, Map<String, MutableInt>> wordCount = new ConcurrentHashMap<>();

        catalogue.parallelForEach((tag, sites) -> {
            Map<String, MutableInt> map = new HashMap<>();
            sites.forEach(site -> {
                System.out.println(site);
                bagOfWords(getSiteText(site)).forEach((word, count) -> {
                    addToMapValue(map, word, count.intValue());
                });
            });
            wordCount.put(tag, map);
        });

        // 2. Count the occurrences of word i not in sites with tag C
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
        // Probabilities for site to have a tag P(site|tag)
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

        // return a tag with max probability
        return probByTag.entrySet().stream().max((e1, e2) -> e1.getValue().compareTo(e2.getValue())).get().getKey();
    }
}
