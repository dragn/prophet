package me.prophet.data;

import java.io.Serializable;
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
public class Keywords implements Serializable {

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

    public Collection<String> tags() {
        return map.keySet();
    }

    public Stream<Map.Entry<String, Map<String, Keyword>>> stream() {
        return map.entrySet().stream();
    }

    public void forEach(BiConsumer<String, Map<String, Keyword>> cons) {
        map.forEach(cons);
    }

    public void add(String tag, Keyword keyword) {
        byTag(tag).put(keyword.word(), keyword);
    }

}
