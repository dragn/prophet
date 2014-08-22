package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * Naive Bayes Classifier
 * <p>
 * User: dsabelnikov
 * Date: 8/20/14
 * Time: 2:49 PM
 */
public class Tagger {

    private Keywords keywords;

    /**
     * Read already learned keywords from file.
     */
    public void fromFile(String file) {
        keywords = Keywords.fromFile(file);
    }

    /**
     * Learn keywords by parsing sites from catalogue.
     */
    public void learn(String catalogueFile) throws IOException {
        Catalogue catalogue = Catalogue.fromFile(catalogueFile);

        // Learn probabilities using bag-of-words model and Bernoulli events model.

        // for every tag do
        //   1. count in how many document given word occur:
        //        for each document get the bag-of-words (without multiplicity)
        //        for each word in the bag increment counter.
        //   2. apply 1/count-of-sites to each word count.
        //   got raw probabilities of encountering word x in tag. Basically done here!
        // Future improvements: use tf-idf measure to remove common words.

        keywords = new Keywords();

        catalogue.forEach((tag, sites) -> {
            Map<String, MutableInt> wordCount = new HashMap<>();
            sites.forEach(site -> {
                System.out.println(site);
                bagOfWords(getSiteText(site)).forEach(word -> {
                    MutableInt count = wordCount.get(word);
                    if (count != null) {
                        count.increment();
                    } else {
                        count = new MutableInt(1);
                        wordCount.put(word, count);
                    }
                });
            });
            final int limit = 5;
            final int siteCount = sites.size();
            wordCount.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().intValue() > limit)
                    .forEach(entry -> {
                        keywords.add(tag, new Keyword(entry.getKey(), entry.getValue().doubleValue() / siteCount));
                    });
        });
    }

    /**
     * Learn keywords by parsing sites from catalogue.
     */
    public void learnWithTfIdf(String catalogueFile) throws IOException {
        Catalogue catalogue = Catalogue.fromFile(catalogueFile);

        // Learn probabilities using bag-of-words model and Bernoulli events model.

        // for every tag do
        //   1. swipe all sites, store the words that appear in at least 5 sites.
        //   2. for each word store, in how many documents it appears
        //   3. put all encountered words in a big counter for "idf" calculation.
        //   4. compute tf-idf = count(site|word) * log(count(site) / count(doc|word))

        keywords = new Keywords();

        Map<String, MutableInt> allWords = new HashMap<>();
        Map<String, Map<String, MutableInt>> wordsByTag = new HashMap<>();

        MutableInt totalSites = new MutableInt(0);

        catalogue.forEach((tag, sites) -> {
            Map<String, MutableInt> wordCount = new HashMap<>();
            sites.parallelStream().forEach(site -> {
                System.out.println(site);
                bagOfWords(getSiteText(site)).forEach(word -> {
                    MutableInt count = wordCount.get(word);
                    MutableInt allCount = allWords.get(word);
                    if (count != null) {
                        count.increment();
                    } else {
                        count = new MutableInt(1);
                        wordCount.put(word, count);
                    }
                    if (allCount != null) {
                        allCount.increment();
                    } else {
                        allCount = new MutableInt(1);
                        allWords.put(word, allCount);
                    }
                });
                totalSites.increment();
            });
            wordsByTag.put(tag, wordCount);
        });

        wordsByTag.forEach((tag, words) -> {
            words.forEach((word, count) -> {
                if (allWords.containsKey(word)) {
                    double tfidf = count.doubleValue() * Math.log(totalSites.doubleValue() / allWords.get(word).doubleValue());
                    keywords.add(tag, new Keyword(word, tfidf));
                }
            });
        });
    }

    private String getSiteText(String site) {
        Document doc = Crawler.getWithRetry(site, 5000, 3);
        return doc != null ? doc.text() : "";
    }

    /**
     * Learn keywords from dumped sites data.
     */
    public void learnFromDump(String dumpFile) {
        // TODO
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
        input.forEach((tag, sites) -> sites.stream().forEach(site -> {
            String calc = tagSite(site);
            System.out.printf("%s: %s\n", site, calc);
            myCatalogue.add(calc, site);
        }));
        TaggerTest.score(input, myCatalogue);
    }

    private Collection<String> bagOfWords(String text) {
        Set<String> words = new HashSet<>();
        Matcher matcher = KeywordsFetcher.wordPattern.matcher(text);
        while (matcher.find()) {
            words.add(matcher.group().toLowerCase());
        }
        return words;
    }

    public String tagSite(String url) {
        Collection<String> bag = bagOfWords(getSiteText(url));

        // Probabilities for site to have a tag P(site|tag)
        Map<String, MutableDouble> probByTag = new HashMap<>();

        // Init probabilities to 1 (for multiplication)
        keywords.tags().forEach(tag -> probByTag.put(tag, new MutableDouble(1)));

        // Count probabilities for each tag
        keywords.tags().forEach(tag -> {

            // For each keyword: if word is in document, multiply by P(word|tag) (keyword.weight())
            // if word is not in document, multiply by (1 - P(word|tag))
            Map<String, Keyword> kws = keywords.byTag(tag);
            Set<String> inDocument = bag.stream().filter(kws::containsKey).collect(Collectors.toCollection(HashSet::new));

            kws.forEach((word, keyword) -> {
                MutableDouble prob = probByTag.get(tag);
                if (inDocument.contains(word)) {
                    // keyword is in document
                    prob.setValue(prob.doubleValue() * keyword.weight());
                } else {
                    // keyword is not in document
                    prob.setValue(prob.doubleValue() * (1 - keyword.weight()));
                }
            });
        });

        // return a tag with max probability
        return probByTag.entrySet().stream().max((e1, e2) -> -e1.getValue().compareTo(e2.getValue())).get().getKey();
    }

    private Double countScores(Collection<Keyword> words, Map<String, Keyword> keywords) {
        MutableDouble score = new MutableDouble(0);
        words.forEach(word -> {
            if (keywords.containsKey(word.word())) {
                Double freq = keywords.get(word.word()).weight();
                if (freq != null) {
                    score.add(word.weight() * freq);
                }
            }
        });
        return score.toDouble();
    }
}
