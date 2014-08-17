package me.dragn;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
    private Map<String, List<String>> keywords;

    private List<Pair<String, List<String>>> sites = new ArrayList<>();

    private Pattern wordPattern = Pattern.compile("[a-zA-Zа-яА-Я]{4,}");

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

        fetchKeywords(sites.get(0).getLeft(), sites.get(0).getRight());
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
    }

    private void fetchKeywords(String tag, List<String> sites) {

        // number of occurrences of words in sites' texts.
        Map<String, MutableInt> words = new HashMap<>();

        int wordCount = 0;
        for (int i = 0; i < sites.size() && i < 40; i++) {
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

        System.out.println(tag);

        Iterator<Word> it = sorted.iterator();
        for (int index = 0; index < 25 && it.hasNext(); index++) {
            Word word = it.next();
            System.out.println("  " + word.string() + ": " + word.count());
        }
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
