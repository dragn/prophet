package me.dragn.tagger;

import java.io.File;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class TaggerMain {

    public static void main(String... args) {
        //fetchKeywords();
        //fetchYaca();

        Tagger tagger = new Tagger();
        tagger.readKeywords(new File("./keywords2.out"));
        tagger.tagSite("http://quora.com");
    }
}
