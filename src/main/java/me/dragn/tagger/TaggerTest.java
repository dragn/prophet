package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;

import java.io.IOException;
import java.util.Collection;

/**
 * User: dsabelnikov
 * Date: 8/21/14
 * Time: 5:22 PM
 */
public class TaggerTest {

    public static void score(Catalogue test, Catalogue output) {
        output.forEach((tag, sites) -> {
            final Collection<String> testSites = test.byTag(tag);
            if (testSites.isEmpty()) return;
            long mutualCount = sites.stream().filter(testSites::contains).count();
            double precision = (double) mutualCount / sites.size();
            double recall = (double) mutualCount / testSites.size();
            double f1 = 2 * precision * recall / (precision + recall);
            System.out.printf("%s\n  precision: %f\n  recall: %f\n  F1: %f\n", tag, precision, recall, f1);
        });
    }

    public static void main(String[] args) throws IOException {
        Catalogue test = Catalogue.fromFile("./catalogue2.out");
        Catalogue output = Catalogue.fromFile("./catalogue.out");
        score(test, output);
    }
}
