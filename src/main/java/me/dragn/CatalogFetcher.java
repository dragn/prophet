package me.dragn;

import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public abstract class CatalogFetcher {

    /**
     * tag -> catalog URL map
     */
    private List<Pair<String, String>> urls = new ArrayList<>();

    public CatalogFetcher() {
    }

    public void readFile(File file) {
        try (FileReader fr = new FileReader(file)) {
            read(fr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void read(Reader read) {
        try (BufferedReader reader = new BufferedReader(read)) {
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, "|");
                if (st.countTokens() < 2) throw new IllegalArgumentException("Invalid file format: " + line);
                urls.add(Pair.of(st.nextToken(), st.nextToken()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns 'tag' -> 'site url' list
     */
    public List<Pair<String, List<String>>> fetch() {
        List<Pair<String, List<String>>> list = new ArrayList<>();
        for (Pair<String, String> pair : urls) {
            list.add(Pair.of(pair.getLeft(), fetchSites(pair.getRight())));
        }
        return list;
    }

    protected abstract List<String> fetchSites(String url);
}
