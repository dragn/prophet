package me.dragn.tagger.data;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Documents catalogued by tag.
 * <p>
 * User: dsabelnikov
 * Date: 8/21/14
 * Time: 5:46 PM
 */
public class Catalogue {

    /**
     * Map tag -> list of documents.
     */
    private Map<String, Collection<String>> documents = new HashMap<>();

    public static Catalogue fromFile(String file) throws IOException {
        Catalogue catalogue = new Catalogue();
        Files.lines(Paths.get(file)).forEach(line -> {
            StringTokenizer st = new StringTokenizer(line, "|");
            if (st.countTokens() < 2) throw new IllegalArgumentException("Illegal file format");

            catalogue.documents.put(st.nextToken(), Arrays.asList(st.nextToken().split(",")));
        });
        return catalogue;
    }

    public Collection<String> tags() {
        return documents.keySet();
    }

    public Collection<String> byTag(String tag) {
        Collection<String> documents = this.documents.get(tag);
        if (documents == null) {
            documents = new ArrayList<>();
            this.documents.put(tag, documents);
        }
        return documents;
    }

    public void add(String tag, List<String> documents) {
        byTag(tag).addAll(documents);
    }

    public void toFile(String file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file))) {
            documents.forEach((tag, docs) -> {
                try {
                    writer.write(tag);
                    writer.write("|");
                    writer.write(StringUtils.join(docs, ","));
                    writer.write("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void forEach(BiConsumer<String, Collection<String>> cons) {
        documents.forEach(cons);
    }

    public void parallelForEach(BiConsumer<String, Collection<String>> cons) {
        documents.entrySet().parallelStream().forEach(entry -> cons.accept(entry.getKey(), entry.getValue()));
    }

    public void add(String tag, String document) {
        byTag(tag).add(document);
    }

    public Map<String, Collection<String>> map() {
        return documents;
    }
}
