package me.dragn;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
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

    private Pattern wordPattern = Pattern.compile("[a-zA-Zа-яА-Я]{4,}");

    private static final double LIMIT_FREQUENCY = 0.001;
    private static final int MAX_SITE_COUNT = 100;

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
                System.out.println("  " + word.string() + ": " + word.count());
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
                boolean found = false;
                for (Map.Entry<String, Set<Word>> e : keywords.entrySet())
                    if (!e.getKey().equals(tag)) {
                        for (Word word1 : e.getValue()) { // may be optimized by using proper data structure
                            if (word.equals(word1)) {
                                found = true;
                                break;
                            }
                        }
                    }
                if (!found) newSet.add(word);
            });
            newKeywords.put(tag, newSet);
            System.out.println(entry.getValue().size() + " reduced to " + newSet.size());
        }
        keywords = newKeywords;
    }

    public void storeKeywords(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, Set<Word>> entry : keywords.entrySet()) {
                writer.write(entry.getKey() + " " + entry.getValue().size() + "\n");
                for (Word word : entry.getValue()) {
                    writer.write(" " + word.string() + " " + word.count() + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Word implements Comparable<Word> {

        private String string;
        private Double count = 0.;

        public Word(String string, double count) {
            this.string = string;
            this.count = count;
        }

        public String string() {
            return string;
        }

        public Double count() {
            return count;
        }

        @Override
        public int compareTo(Word o) {
            return - this.count.compareTo(o.count);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Word word = (Word) o;
            return !(string != null ? !string.equals(word.string) : word.string != null);
        }

        @Override
        public int hashCode() {
            return string != null ? string.hashCode() : 0;
        }
    }

    private SortedSet<Word> fetchKeywords(List<String> sites) {

        // number of occurrences of words in sites' texts.
        Map<String, MutableInt> words = new HashMap<>();

        int wordCount = 0;
        for (int i = 0; i < sites.size() && i < MAX_SITE_COUNT; i++) {
            System.out.println("Next site: " + sites.get(i));
            Document doc = getWithRetry(sites.get(i));
            if (doc == null) continue;

            Matcher matcher = wordPattern.matcher(doc.text());
            while (matcher.find()) {
                String str = matcher.group().toLowerCase();
                MutableInt count = words.get(str);
                if (count == null) {
                    words.put(str, new MutableInt(1));
                } else {
                    count.increment();
                }
                wordCount++;
            }
            System.out.println("Words now: " + words.size() + ", total processed: " + wordCount);
        }

        final int total = wordCount;
        SortedSet<Word> sorted = words.entrySet().parallelStream()
                .map(entry -> new Word(entry.getKey(), (double) entry.getValue().getValue() / total))
                .collect(Collectors.toCollection(() -> new TreeSet<>()));

        return sorted;
    }

    private Document getWithRetry(String url) {
        Document doc = null;
        int retries = 0;
        while (doc == null && retries < 5) {
            try {
                doc = Jsoup.connect(url).get();
            } catch (SocketTimeoutException | ConnectException ex) {
                // .. retry
                retries++;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        return doc;
    }
}
