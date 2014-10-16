package me.prophet.util;

import me.prophet.data.Catalogue;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class YandexCatalogueFetcher extends CatalogueFetcher {

    @Override
    protected String pageUrl(String baseUrl, int page) {
        return baseUrl.contains("?") ?
                baseUrl.replace("?", "/" + page + ".html?") :
                baseUrl + page + ".html";
    }

    @Override
    protected void addLinks(Collection<String> result, Document doc) {
        doc.select("a.b-result__name").stream().map(element -> element.attr("href")).forEach(result::add);
    }

    public static void main(String[] args) throws IOException {
        CatalogueFetcher cf = new YandexCatalogueFetcher();
        cf.readFile("conf/yaca.catalog");
        Catalogue cat = cf.fetch();
        cat.toFile("./catalogue.out");
    }
}
