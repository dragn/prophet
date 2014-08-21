package me.dragn.tagger.data;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Representation of a bunch of keywords. May be stored to or read from file.
 * <p>
 * User: dsabelnikov
 * Date: 8/21/14
 * Time: 5:26 PM
 */
public class Keywords {

    /**
     * tag: word: keyword
     */
    private Map<String, Map<String, Keyword>> map = new HashMap<>();

    public void addAll(String tag, Collection<Keyword> keywords) {
        final Map<String, Keyword> m = byTag(tag);
        keywords.forEach(word -> m.put(word.word(), word));
    }

    public Map<String, Keyword> byTag(String tag) {
        Map<String, Keyword> m = map.get(tag);
        if (m == null) {
            m = new HashMap<>();
            map.put(tag, m);
        }
        return m;
    }

    public Stream<Map.Entry<String, Map<String, Keyword>>> stream() {
        return map.entrySet().stream();
    }

    public void forEach(BiConsumer<String, Map<String, Keyword>> cons) {
        map.forEach(cons);
    }

    public static Keywords fromFile(File file) {
        Keywords keywords = new Keywords();
        try (BufferedReader read = new BufferedReader(new FileReader(file))) {
            String line;
            Map<String, Keyword> words = new HashMap<>();
            String tag = null;
            while ((line = read.readLine()) != null) {
                if (line.startsWith(" ")) {
                    String[] tokens = line.split(" ");
                    words.put(tokens[1], new Keyword(tokens[1], Double.parseDouble(tokens[2])));
                } else {
                    if (!words.isEmpty() && tag != null) {
                        keywords.map.put(tag, words);
                        words = new HashMap<>();
                    }
                    tag = line.split(":")[0];
                }
            }
            if (!words.isEmpty() && tag != null) {
                keywords.map.put(tag, words);
            }
            System.out.println(keywords.map.size() + " tags.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keywords;
    }

    public void add(String tag, Keyword keyword) {
        byTag(tag).put(keyword.word(), keyword);
    }

    public void toFile(String path) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get(path)))) {
            forEach((tag, words) -> {
                writer.println(tag);
                words.forEach((word, keyword) -> writer.printf(" %s %f\n", word, keyword.weight()));
            });
        }
    }
}
