package me.dragn.tagger;

import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * User: dsabelnikov
 * Date: 8/20/14
 * Time: 2:49 PM
 */
public class Tagger {

    private Keywords keywords;

    public void readKeywords(File file) {
        keywords = Keywords.fromFile(file);
    }

    public List<String> tagSite(String url) {
        List<String> tags = new ArrayList<>();

        Map<String, MutableInt> words = new HashMap<>();
        final MutableInt wordCount = new MutableInt(0);

        new Crawler(url, 3, 10).crawl(doc -> {
            System.out.println(" -- " + doc.location());

            Matcher matcher = KeywordsFetcher.wordPattern.matcher(doc.text());
            while (matcher.find()) {
                String str = matcher.group().toLowerCase();
                MutableInt count = words.get(str);
                if (count == null) {
                    words.put(str, new MutableInt(1));
                } else {
                    count.increment();
                }
                wordCount.increment();
            }
            //System.out.println("Words now: " + words.size() + ", total processed: " + wordCount);
        });

        final int total = wordCount.intValue();
        List<Word> wordList = words.entrySet().parallelStream()
                .map(entry -> new Word(entry.getKey(), (double) entry.getValue().getValue() / total))
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Double> scores = new HashMap<>();

        keywords.forEach((word, list) -> {
            Double score = countScores(wordList, list);
            if (score > 0) {
                scores.put(word, score);
            }
        });

        scores.entrySet().stream().sorted((o1, o2) -> - o1.getValue().compareTo(o2.getValue()))
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));

        return tags;
    }

    private Double countScores(Collection<Word> words, Map<String, Keyword> keywords) {
        MutableDouble score = new MutableDouble(0);
        words.forEach(word -> {
            Double freq = keywords.get(word.string()).weight();
            if (freq != null) {
                score.add(word.frequency() / freq);
            }
        });
        return score.toDouble();
    }
}
