package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class TaggerMain {

    public static void main(String... args) throws IOException {
        Tagger tagger = new MNBTagger();
        tagger.learn(Catalogue.fromFile("./learn-data/mini-catalogue.out"));
        //tagger.fromFile("./keywords.out");
        //tagger.toFile("./keywords.out");
        tagger.test(Catalogue.fromFile("test-data/mini-catalogue.out"));
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
