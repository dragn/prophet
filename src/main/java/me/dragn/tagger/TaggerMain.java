package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.impl.TWCNBTagger;
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
        DataProvider provider = new SiteDownloadDataProvider(1);
        Tagger tagger = new TWCNBTagger(provider);
        tagger.learn(Catalogue.fromFile("./learn-data/mini-catalogue.out"));
        //tagger.fromFile("./keywords_TWCNB_mini.out");
        tagger.toFile("./keywords_TWCNB_mini.out");
        tagger.test(Catalogue.fromFile("test-data/mini-catalogue.out"));
//        String text = provider.getDocument("http://nadevaemfartuki.ru/");
//        System.out.println(text);
//        if (StringUtils.isEmpty(text)) {
//            System.out.println("No text!");
//        } else {
//            System.out.println(tagger.multitagText(text));
//        }

//        Catalogue c = Catalogue.fromFile("./test-data/mini-catalogue.out");
//        Catalogue c1 = new Catalogue();
//        c.parallelForEach((tag, docs) -> {
//            List<String> filtered = docs.stream().filter(doc -> {
//                System.out.println(doc);
//                return StringUtils.isNotEmpty(provider.getDocument(doc));
//            }).collect(Collectors.toList());
//            synchronized (c1) {
//                c1.add(tag, filtered);
//            }
//        });
//        c.toFile("test-data/mini-catalogue.out");
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
