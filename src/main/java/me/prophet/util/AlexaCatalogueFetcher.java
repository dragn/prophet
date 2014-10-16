package me.prophet.util;

import org.jsoup.nodes.Document;

import java.util.Collection;

/**
 * User: dsabelnikov
 * Date: 10/16/14
 * Time: 2:44 PM
 */
public class AlexaCatalogueFetcher extends CatalogueFetcher {

    @Override
    protected String pageUrl(String baseUrl, int page) {
        return baseUrl.replaceAll("/category(;\\d*)?/", "/category;" + page + "/");
    }

    @Override
    protected void addLinks(Collection<String> result, Document doc) {
        doc.select(".desc-paragraph a").stream().map((e) -> e.text().toLowerCase()).forEach(result::add);
    }
}
