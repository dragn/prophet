package me.dragn.tagger;

import java.io.File;

/**
 * User: dsabelnikov
 * Date: 8/21/14
 * Time: 5:22 PM
 */
public class TaggerTest {
    public static void main(String[] args) {
        Tagger tagger = new Tagger();
        tagger.readKeywords(new File("./keywords2.out"));

    }
}
