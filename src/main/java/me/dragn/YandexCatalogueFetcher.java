package me.dragn;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
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
            while (page < 10) {
                System.out.println("Page " + (page + 1));
                Document doc = Jsoup.connect(url).get();
                for (Element element : doc.select("a.b-result__name")) {
                    result.add(element.attr("href"));
                }
                url += baseUrl + (++page) + ".html";
            }
        } catch (HttpStatusException ex) {
            // ...
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Parsed " + page + " pages");
        return result;
    }
}
