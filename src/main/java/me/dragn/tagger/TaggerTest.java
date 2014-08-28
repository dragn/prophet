package me.dragn.tagger;

import me.dragn.tagger.data.Catalogue;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.io.IOException;
import java.util.Collection;

/**
 * User: dsabelnikov
 * Date: 8/21/14
 * Time: 5:22 PM
 */
public class TaggerTest {

    public static void score(Catalogue test, Catalogue output) {
        MutableDouble totalPrecision = new MutableDouble(0);
        MutableDouble totalRecall = new MutableDouble(0);
        MutableDouble totalF1 = new MutableDouble(0);
        output.forEach((tag, sites) -> {
            final Collection<String> testSites = test.byTag(tag);
            if (testSites.isEmpty()) return;
            long mutualCount = sites.stream().filter(testSites::contains).count();
            double precision = (double) mutualCount / sites.size();
            double recall = (double) mutualCount / testSites.size();
            double f1 = 2 * precision * recall / (precision + recall);
            totalPrecision.add(precision);
            totalRecall.add(recall);
            totalF1.add(f1);
            System.out.printf("%s\n  precision: %f\n  recall: %f\n  F1: %f\n", tag, precision, recall, f1);
        });
        System.out.printf("-- Average --\n  precision: %f\n  recall: %f\n  F1 %f\n",
                totalPrecision.doubleValue() / test.tags().size(),
                totalRecall.doubleValue() / test.tags().size(),
                totalF1.doubleValue() / test.tags().size()
        );
    }

    public static void main(String[] args) throws IOException {
        Catalogue test = Catalogue.fromFile("./catalogue2.out");
        Catalogue output = Catalogue.fromFile("./catalogue.out");
        score(test, output);
    }
}
