package me.prophet.prov;

import me.prophet.util.Crawler;

/**
 * DataProvider implementation that downloads site text from the remote URL's.
 * Keys to documents are URL's.
 *
 * User: dsabelnikov
 * Date: 8/26/14
 * Time: 7:10 PM
 */
public class SiteDownloadDataProvider implements DataProvider {

    private int crawlDepth;

    public SiteDownloadDataProvider(int crawlDepth) {
        this.crawlDepth = crawlDepth;
    }

    public SiteDownloadDataProvider() {
        this(1);
    }

    /**
     * Retrieves a site's text.
     * @param key
     * @return
     */
    @Override
    public String getDocument(String key) {
        Crawler crawler = new Crawler(key, crawlDepth);
        crawler.setRequestTimeout(5000);
        crawler.setConnectionRetries(3);
        final StringBuilder text = new StringBuilder();
        crawler.crawl(doc -> {
            text.append(doc.text()).append(" ");
        });
        return text.toString();
    }
}
