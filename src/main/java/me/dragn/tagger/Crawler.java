package me.dragn.tagger;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Queue;
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

    private final static int REQUEST_TIMEOUT = 30 * 1000; // 30sec
    private final static String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/36.0.1985.18 Safari/537.36";

    private int maxDepth;
    private int limit = 40;

    private Queue<Page> queue = new PriorityQueue<>((p1, p2) -> p1.depth - p2.depth);

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

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://" + baseUrl;
        }

        Document doc = getWithRetry(baseUrl);
        if (doc == null) return;
        baseUrl = doc.location();

        if (!baseUrl.endsWith("/")) baseUrl += "/";

        queue.add(new Page(baseUrl, 1));
        history.add(baseUrl);

        Page page;
        while ((page = queue.poll()) != null) {
            doc = getWithRetry(page.url);
            if (doc == null) continue;

            if (!domainMatch(doc.location(), baseUrl)) continue; // redirected away!

            consumer.accept(doc);

            if (page.depth < maxDepth) {
                final int depth = page.depth + 1;
                final String url = page.url;
                doc.select("a").stream().filter(a -> {
                    String link = getLink(url, a);
                    return link != null && !history.contains(link);
                }).limit(limit).forEach(a -> {
                    String link = getLink(url, a);
                    history.add(link);
                    queue.add(new Page(link, depth));
                });
            }
        }
    }

    private boolean domainMatch(String url, String baseUrl) {
        return url.replaceAll("^https://", "http://").startsWith(
                baseUrl.replaceAll("^https://", "http://"));
    }

    private String getLink(String url, Element a) {
        String link = a.attr("href");

        link = link.replaceAll("^//", "http://"); // handle "//site.com" notation

        if (link.isEmpty() || link.startsWith("#") || link.startsWith("mailto:") ||
                link.startsWith("javascript:")) return null;

        link = link.replaceAll("/./", "/");

        // relative with root
        if (link.startsWith("/")) link = baseUrl.replaceAll("/$", "") + link;

        link = link.replaceAll(";jsessionid=.*$", ""); // strip jsessionid;

        // relative without root
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = url.endsWith("/") ? url + link : url + "/" + link;
        } else if (!domainMatch(link, baseUrl)) {
            // possibly external link
            return null;
        }

        link = link.replaceAll("[^/]*/../", ""); // resolve "/../"

        return link;
    }

    public static Document getWithRetry(String url) {
        return getWithRetry(url, REQUEST_TIMEOUT, 5);
    }

    public static Document getWithRetry(String url, int requestTimeout, int maxRetries) {
        Document doc = null;
        int retries = 0;
        while (doc == null && retries < maxRetries) {
            try {
                doc = Jsoup.connect(url).timeout(requestTimeout).userAgent(USER_AGENT).get();
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

    private static class Page {
        final String url;
        final int depth;

        private Page(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }
}
