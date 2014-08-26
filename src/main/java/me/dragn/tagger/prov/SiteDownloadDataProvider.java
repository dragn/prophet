package me.dragn.tagger.prov;

import me.dragn.tagger.util.Crawler;
import org.jsoup.nodes.Document;

/**
 * DataProvider implementation that downloads site text from the remote URL's.
 * Keys to documents are URL's.
 *
 * User: dsabelnikov
 * Date: 8/26/14
 * Time: 7:10 PM
 */
public class SiteDownloadDataProvider implements DataProvider {
    /**
     * Retrieves a site's text.
     * @param key
     * @return
     */
    @Override
    public String getDocument(String key) {
        Document doc = Crawler.getWithRetry(key, 10, 3);
        return doc != null ? doc.text() : "";
    }
}
