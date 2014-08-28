package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.impl.CNBTagger;
import me.dragn.tagger.prov.DataProvider;
import me.dragn.tagger.prov.SiteDownloadDataProvider;
import me.dragn.tagger.util.Crawler;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class TaggerMain {

    public static void main(String... args) throws IOException {
        DataProvider provider = new SiteDownloadDataProvider(2);
        Tagger tagger = new CNBTagger(provider);
        //tagger.learn(Catalogue.fromFile("./learn-data/catalogue.out"));
        tagger.fromFile("./keywords_TWCNB.out");
        //tagger.toFile("./keywords_TWCNB.out");
        tagger.test(Catalogue.fromFile("test-data/mini-catalogue.out"));
        //System.out.println(tagger.tagText(provider.getDocument("http://www.zvuki.ru/")));
    }

    public static void dump(String catalogueFile, String outFile) throws IOException {
        Catalogue catalogue = Catalogue.fromFile("./catalogue2.out");
        try (PrintWriter print = new PrintWriter(Files.newBufferedWriter(Paths.get("dump.out")))) {
            catalogue.forEach((tag, sites) -> {
                print.println("**" + tag);
                sites.forEach(site -> {
                    new Crawler(site, 1, 10).crawl(doc -> {
                        System.out.println(doc.location());
                        print.println(doc.location() + "|" + doc.text());
                    });
                });
            });
        }
    }
}
