package me.dragn;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public abstract class CatalogFetcher {

    /**
     * tag -> catalog URL map
     */
    private List<Pair<String, List<String>>> urls = new ArrayList<>();

    private File output;

    public CatalogFetcher(File output) {
        this.output = output;
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
                urls.add(Pair.of(st.nextToken(), Arrays.asList(st.nextToken().split(","))));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns 'tag' -> 'site url' list
     */
    public void fetch() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            for (Pair<String, List<String>> pair : urls) {
                writer.write(pair.getLeft());
                writer.write("|");
                for (String url : pair.getRight()) {
                    writer.write(StringUtils.join(fetchSites(url), ","));
                    writer.write(",");
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract List<String> fetchSites(String url);
}
