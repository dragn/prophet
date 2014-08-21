package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * User: dsabelnikov
 * Date: 8/15/14
 * Time: 12:43 PM
 */
public class KeywordsFetcher {

    // 'tag' -> keywords map
    private Keywords keywords = new Keywords();

    private Catalogue catalogue;

    public static Pattern wordPattern = Pattern.compile("[a-zA-Zа-яА-Я]{4,}");

    private static final double LIMIT_FREQUENCY = 0.001;
    private static final int MAX_SITE_COUNT = 400;
    private static final int MAX_WORD_MULTITAGS = 1;

    public KeywordsFetcher() {
    }

    public void readCatalogue(String file) throws IOException {
        catalogue = Catalogue.fromFile(Paths.get(file));
    }

    public void fetchKeywords() {
        catalogue.forEach((tag, sites) -> {
            Collection<Keyword> words =
                    fetchKeywords(sites).stream().filter(word -> word.weight() > LIMIT_FREQUENCY)
                            .collect(Collectors.toList());
            keywords.addAll(tag, words);

            System.out.println(tag);
            words.forEach(keyword -> System.out.format("  %s: %f\n", keyword.word(), keyword.weight()));
        });
    }

    public void cleanOut() {
        System.out.println("*** Clean out started");

        Keywords newKeywords = new Keywords();

        keywords.forEach((tag, words) -> {
            System.out.println("Tag: " + tag);

            words.forEach((word, keyword) -> {
                long found = keywords.stream()
                        .filter(entry -> !entry.getKey().equals(tag) &&
                                (entry.getValue().containsKey(keyword.word()))).count();
                if (found < MAX_WORD_MULTITAGS) newKeywords.add(tag, keyword);
            });
            System.out.println(words.size() + " reduced to " + newKeywords.byTag(tag).size());
        });
        keywords = newKeywords;
    }

    public void storeKeywords(String file) throws IOException {
        keywords.toFile(file);
    }

    private Collection<Keyword> fetchKeywords(Collection<String> sites) {

        // number of occurrences of words in sites' texts.
        Map<String, MutableDouble> words = new HashMap<>();

        List<String> list = sites.stream().collect(Collectors.toList());
        Collections.shuffle(list);

        int siteCount = Math.min(list.size(), MAX_SITE_COUNT);

        for (int i = 0; i < siteCount; i++) {

            Crawler c = new Crawler(list.get(i), 2, 5);

            final Map<String, MutableInt> siteWords = new HashMap<>();
            final MutableInt totalWords = new MutableInt(0);

            c.crawl(doc -> {
                System.out.println(doc.location());
                countWords(doc, siteWords, totalWords);
            });

            siteWords.forEach((word, count) -> {
                MutableDouble d = words.get(word);
                double add = count.doubleValue() / totalWords.doubleValue() / siteCount;
                if (d != null) d.add(add);
                else words.put(word, new MutableDouble(add));
            });

            System.out.println("Site " + i + "/" + siteCount);
        }

        return words.entrySet().stream().map(entry -> new Keyword(entry.getKey(), entry.getValue().toDouble()))
                .collect(Collectors.toList());
    }

    private void countWords(Document doc, Map<String, MutableInt> siteWords, MutableInt totalWords) {
        Matcher matcher = wordPattern.matcher(doc.text());
        while (matcher.find()) {
            String str = matcher.group().toLowerCase();
            MutableInt count = siteWords.get(str);
            if (count == null) {
                siteWords.put(str, new MutableInt(1));
            } else {
                count.increment();
            }
            totalWords.increment();
        }
    }

    public static void main(String[] args) throws IOException {
        KeywordsFetcher kf = new KeywordsFetcher();
        kf.readCatalogue("./catalogue.out");
        kf.fetchKeywords();
        kf.cleanOut();
        kf.storeKeywords("./keywords.out");
    }
}
