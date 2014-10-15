package me.prophet.tag;

import me.prophet.data.Catalogue;
import me.prophet.data.Keyword;
import me.prophet.data.Keywords;
import me.prophet.prov.DataProvider;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Transformed Weight-Normalized Complement Naive Bayes.
 * <p>
 * Based on the article by Rennie et al. [2003]: http://people.csail.mit.edu/jrennie/papers/icml03-nb.pdf
 * User: dsabelnikov
 * Date: 8/25/14
 * Time: 5:48 PM
 */
public class TWCNBTagger extends Tagger {

    private final int SITE_LIMIT = 40;

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
            Collections.shuffle((List) docs);
            docs.stream().limit(SITE_LIMIT).forEach(docKey -> {
                if (verbose) System.out.println(docKey);
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
                count.setValue(Math.log10(count.getValue() + 1) * Math.log10((double) docCount.size() / delta.get(word)));
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

            //System.out.println(tag + ": " + probByTag.get(tag));
        });

        return getSigmaBest(probByTag, 0);
    }

    @Override
    public List<String> multitagText(String text) {
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

        double mean = probByTag.values().stream().mapToDouble(MutableDouble::getValue).average().getAsDouble();

        // Standard deviation
        double stDev = Math.sqrt(probByTag.values().stream().mapToDouble(
                prob -> Math.pow(prob.doubleValue() - mean, 2)).sum() / probByTag.size());

        //System.out.printf("Mean: %f, st.dev.: %f\n", mean, stDev);

        // Check for a 4-sigma certainty
        return probByTag.entrySet().stream()
                .filter(entry -> entry.getValue().doubleValue() > (mean + stDev))
                .sorted((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
