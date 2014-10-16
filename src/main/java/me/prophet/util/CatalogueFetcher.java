package me.prophet.util;

import me.prophet.data.Catalogue;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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

    protected List<String> fetchSites(String baseUrl) {
        List<String> result = new ArrayList<>();
        int page = 0;
        System.out.println("Parsing " + baseUrl);
        try {
            while (page < getMaxPages()) {
                String url = pageUrl(baseUrl, page);
                System.out.println("Page " + (page + 1) + ": " + url);
                Document doc;
                try {
                    doc = Jsoup.connect(url).get();
                } catch (SocketTimeoutException ex) {
                    continue;
                }
                addLinks(result, doc);
                page++;
            }
        } catch (HttpStatusException ex) {
            // ...
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Parsed " + page + " pages");
        return result;
    }

    protected abstract String pageUrl(String baseUrl, int page);
    protected abstract void addLinks(Collection<String> result, Document doc);
}
