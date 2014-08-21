package me.dragn;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * User: dsabelnikov
 * Date: 8/20/14
 * Time: 2:49 PM
 */
public class Tagger {

    private Map<String, Map<String, Double>> keywords = new HashMap<>();

    public void readKeywords(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            readKeywords(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void readKeywords(BufferedReader read) throws IOException {
        String line;
        Map<String, Double> words = new HashMap<>();
        String tag = null;
        while ((line = read.readLine()) != null) {
            if (line.startsWith(" ")) {
                String[] tokens = line.split(" ");
                words.put(tokens[1], Double.parseDouble(tokens[2]));
            } else {
                if (!words.isEmpty() && tag != null) {
                    keywords.put(tag, words);
                    words = new HashMap<>();
                }
                tag = line.split(":")[0];
            }
        }
        if (!words.isEmpty() && tag != null) {
            keywords.put(tag, words);
        }
        System.out.println(keywords.size() + " tags.");
    }

    public List<String> tagSite(String url) {
        List<String> tags = new ArrayList<>();

        Map<String, MutableInt> words = new HashMap<>();
        final MutableInt wordCount = new MutableInt(0);

        new Crawler(url, 4, 50).crawl(doc -> {
            System.out.println(doc.location());

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
        SortedSet<Word> sorted = words.entrySet().parallelStream()
                .map(entry -> new Word(entry.getKey(), (double) entry.getValue().getValue() / total))
                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        Map<String, Double> scores = new HashMap<>();

        keywords.forEach((word, list) -> {
            Double score = countScores(sorted, list);
            if (score > 0) {
                scores.put(word, countScores(sorted, list));
                System.out.println(word + ": " + score);
            }
        });

        return tags;
    }

    private Double countScores(SortedSet<Word> words, Map<String, Double> keywords) {
        Set<Word> head = words.headSet(new Word("", 0.00001));

        MutableDouble score = new MutableDouble(0);

        head.forEach(word -> {
            Double freq = keywords.get(word.string());
            if (freq != null) {
                score.add(word.frequency() > freq ? 1 : (freq - word.frequency()) / freq);
            }
        });

        return score.toDouble();
    }
}
