package me.prophet.util;

import me.prophet.data.Catalogue;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public abstract class CatalogueFetcher {

    private int maxPages = 20;

    /**
     * tag -> catalog URL map
     */
    private List<Pair<String, List<String>>> urls = new ArrayList<>();

    public void readFile(String file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, "|");
                if (st.countTokens() < 2) throw new IllegalArgumentException("Invalid file format: " + line);
                urls.add(Pair.of(st.nextToken(), Arrays.asList(st.nextToken().split(","))));
            }
        }
    }

    /**
     * Returns 'tag' -> 'site url' list
     */
    public Catalogue fetch() {
        Catalogue catalogue = new Catalogue();
        urls.forEach(pair ->
            pair.getValue().forEach(chapter ->
                catalogue.add(pair.getKey(), fetchSites(chapter))
            )
        );
        return catalogue;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getMaxPages() {
        return maxPages;
    }

    protected abstract List<String> fetchSites(String url);
}
