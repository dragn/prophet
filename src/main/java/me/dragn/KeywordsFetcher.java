package me.dragn;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
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
    private Map<String, Set<Word>> keywords = new HashMap<>();

    private List<Pair<String, List<String>>> sites = new ArrayList<>();

    public static Pattern wordPattern = Pattern.compile("[a-zA-Zа-яА-Я]{4,}");

    private static final double LIMIT_FREQUENCY = 0.0008;
    private static final int MAX_SITE_COUNT = 400;
    private static final int MAX_WORD_MULTITAGS = 3;

    public KeywordsFetcher() {
    }

    public void readCatalogue(File file) {
        try (BufferedReader f = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = f.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, "|");
                if (st.countTokens() < 2) throw new IllegalArgumentException("Illegal file format");
                String tag = st.nextToken();

                StringTokenizer sitesTokenizer = new StringTokenizer(st.nextToken(), ",");
                List<String> list = new ArrayList<>(sitesTokenizer.countTokens());
                while (sitesTokenizer.hasMoreTokens()) {
                    String next = sitesTokenizer.nextToken();
                    if (StringUtils.isNotEmpty(next)) {
                        list.add(next);
                    }
                }
                sites.add(Pair.of(tag, list));
            }
            for (Pair<String, List<String>> pair : sites) {
                System.out.println(pair.getLeft() + ": " + pair.getRight().size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void fetchKeywords() {
        sites.parallelStream().forEach(site -> {
            String tag = site.getKey();

            SortedSet<Word> words = fetchKeywords(site.getValue()).headSet(new Word("", LIMIT_FREQUENCY));
            keywords.put(tag, words);

            System.out.println(tag);

            for (Word word : words) {
                System.out.println("  " + word.string() + ": " + word.frequency());
            }
        });
    }

    public void cleanOut() {
        System.out.println("*** Clean out started");
        Map<String, Set<Word>> newKeywords = new HashMap<>();
        for (Map.Entry<String, Set<Word>> entry : keywords.entrySet()) {
            String tag = entry.getKey();
            System.out.println("Tag: " + tag);
            Set<Word> newSet = new TreeSet<>();
            entry.getValue().parallelStream().forEach(word -> {
                int found = 0;
                for (Map.Entry<String, Set<Word>> e : keywords.entrySet())
                    if (!e.getKey().equals(tag)) {
                        for (Word word1 : e.getValue()) { // may be optimized by using proper data structure
                            if (word.equals(word1)) {
                                found++;
                                if (found == MAX_WORD_MULTITAGS) break;
                            }
                        }
                    }
                if (found < MAX_WORD_MULTITAGS) newSet.add(word);
            });
            newKeywords.put(tag, newSet);
            System.out.println(entry.getValue().size() + " reduced to " + newSet.size());
        }
        keywords = newKeywords;
    }

    public void storeKeywords(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, Set<Word>> entry : keywords.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue().size() + "\n");
                for (Word word : entry.getValue()) {
                    writer.write(" " + word.string() + " " + word.frequency() + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SortedSet<Word> fetchKeywords(List<String> sites) {

        // number of occurrences of words in sites' texts.
        Map<String, MutableDouble> words = new HashMap<>();

        Collections.shuffle(sites);

        int siteCount = Math.min(sites.size(), MAX_SITE_COUNT);

        for (int i = 0; i < siteCount; i++) {

            Crawler c = new Crawler(sites.get(i), 3, 10);

            final Map<String, MutableInt> siteWords = new HashMap<>();
            final MutableInt totalWords = new MutableInt(0);

            c.crawl(doc -> {
                System.out.println(doc.location());

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
            });

            siteWords.forEach((word, count) -> {
                MutableDouble d = words.get(word);
                double add = count.doubleValue() / totalWords.doubleValue() / siteCount;
                if (d != null) d.add(add);
                else words.put(word, new MutableDouble(add));
            });

            System.out.println("Site " + i + "/" + siteCount);
        }

        SortedSet<Word> sorted = words.entrySet().parallelStream()
                .map(entry -> new Word(entry.getKey(), entry.getValue().toDouble()))
                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        return sorted;
    }
}
