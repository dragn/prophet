package me.dragn.tagger.data;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Sites catalogued by tag.
 * <p>
 * User: dsabelnikov
 * Date: 8/21/14
 * Time: 5:46 PM
 */
public class Catalogue {

    /**
     * Map tag -> list of sites.
     */
    private Map<String, Collection<String>> sites = new HashMap<>();

    public static Catalogue fromFile(Path file) throws IOException {
        Catalogue catalogue = new Catalogue();
        Files.lines(file).forEach(line -> {
            StringTokenizer st = new StringTokenizer(line, "|");
            if (st.countTokens() < 2) throw new IllegalArgumentException("Illegal file format");

            catalogue.sites.put(st.nextToken(), Arrays.asList(st.nextToken().split(",")));
        });
        return catalogue;
    }

    public Collection<String> tags() {
        return sites.keySet();
    }

    public Collection<String> byTag(String tag) {
        Collection<String> sites = this.sites.get(tag);
        if (sites == null) {
            sites = new ArrayList<>();
            this.sites.put(tag, sites);
        }
        return sites;
    }

    public void add(String tag, List<String> sites) {
        byTag(tag).addAll(sites);
    }

    public void toFile(String file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file))) {
            sites.forEach((tag, sites) -> {
                try {
                    writer.write(tag);
                    writer.write("|");
                    writer.write(StringUtils.join(sites, ","));
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
        sites.forEach(cons);
    }
}
