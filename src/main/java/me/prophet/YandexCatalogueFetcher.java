package me.prophet;

import me.prophet.data.Catalogue;
import me.prophet.util.CatalogFetcher;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class YandexCatalogueFetcher extends CatalogFetcher {

    @Override
    protected List<String> fetchSites(String baseUrl) {
        List<String> result = new ArrayList<>();
        int page = 0;
        String url = baseUrl;
        System.out.println("Parsing " + baseUrl);
        try {
            while (page < 500) {
                System.out.println("Page " + (page + 1) + ": " + url);
                Document doc;
                try {
                    doc = Jsoup.connect(url).get();
                } catch (SocketTimeoutException ex) {
                    continue;
                }
                Elements links = doc.select("a.b-result__name");
                if (links.isEmpty()) break;
                for (Element element : links) {
                    result.add(element.attr("href"));
                }
                if (baseUrl.contains("?")) {
                    url = baseUrl.replace("?", "/" + (++page) + ".html?");
                } else {
                    url = baseUrl + (++page) + ".html";
                }
            }
        } catch (HttpStatusException ex) {
            // ...
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Parsed " + page + " pages");
        return result;
    }

    public static void main(String[] args) throws IOException {
        CatalogFetcher cf = new YandexCatalogueFetcher();
        cf.readFile("conf/yaca.catalog");
        Catalogue cat = cf.fetch();
        cat.toFile("./catalogue.out");
    }
}
