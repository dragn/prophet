package me.dragn;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * User: dsabelnikov
 * Date: 8/20/14
 * Time: 3:08 PM
 */
public class Crawler {

    private String baseUrl;
    private Set<String> history = new HashSet<>();

    private final int REQUEST_TIMEOUT = 30 * 1000; // 30sec
    private final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/36.0.1985.18 Safari/537.36";

    private int maxDepth;
    private int limit = 40;

    public Crawler(String baseUrl, int maxDepth, int limit) {
        this.limit = limit;
        this.maxDepth = maxDepth;
        this.baseUrl = baseUrl;
    }

    public Crawler(String baseUrl, int maxDepth) {
        this.baseUrl = baseUrl;
        this.maxDepth = maxDepth;
    }

    public void crawl(Consumer<Document> consumer) {
        history = new HashSet<>();

        Document doc = getWithRetry(baseUrl);
        if (doc == null) return;
        baseUrl = doc.location();

        history.add(baseUrl);
        crawl(baseUrl, 0, consumer);
    }

    private void crawl(String url, int depth, Consumer<Document> consumer) {

        if (depth == maxDepth) return;

        Document doc = getWithRetry(url);
        if (doc == null) return;

        if (!doc.location().startsWith(baseUrl)) return; // redirected away!

        consumer.accept(doc);
        doc.select("a").stream().limit(limit).forEach(a -> {
            String link = a.attr("href");

            link = link.replaceAll("\\?.*$", "");

            if (link.isEmpty() || link.startsWith("#") || link.startsWith("mailto:") ||
                    link.startsWith("javascript:")) return;

            // relative with root
            if (link.startsWith("/")) link = baseUrl.replaceAll("/$", "") + link;

            // relative without root
            if (!link.startsWith("http://") && !link.startsWith("https://")) {
                link = url.endsWith("/") ? baseUrl + link : baseUrl + "/" + link;
            } else if (!link.startsWith(baseUrl)) {
                // possibly external link
                return;
            }

            if (!history.contains(link)) {
                history.add(link);
                crawl(link, depth + 1, consumer);
            }
        });
    }

    private Document getWithRetry(String url) {
        Document doc = null;
        int retries = 0;
        while (doc == null && retries < 5) {
            try {
                doc = Jsoup.connect(url).timeout(REQUEST_TIMEOUT).userAgent(USER_AGENT).get();
            } catch (SocketTimeoutException | ConnectException ex) {
                // .. retry
                retries++;
            } catch (HttpStatusException ex) {
                if (ex.getStatusCode() != 404 && ex.getStatusCode() != 403) ex.printStackTrace();
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return doc;
    }
}
