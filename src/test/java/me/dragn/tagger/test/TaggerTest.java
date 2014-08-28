package me.dragn.tagger.test;

import me.dragn.tagger.Tagger;
import me.dragn.tagger.data.Catalogue;
import me.dragn.tagger.data.Keyword;
import me.dragn.tagger.data.Keywords;
import me.dragn.tagger.impl.CNBTagger;
import me.dragn.tagger.impl.MNBTagger;
import me.dragn.tagger.impl.TWCNBTagger;
import me.dragn.tagger.prov.DataProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * User: dsabelnikov
 * Date: 8/26/14
 * Time: 7:30 PM
 */
public class TaggerTest {

    private DataProvider testDataProvider = new DataProvider() {

        Map<String, String> texts = new HashMap<String, String>() {{
            put("111", "aaaa bbbb");
            put("222", "bbbb cccc");
            put("333", "aaaa aaaa");
            put("444", "dddd cccc bbbb");
            put("555", "cccc dddd ssss");
            put("666", "aaaa vvvv");
            put("777", "cccc bbbb dddd");
        }};

        @Override
        public String getDocument(String key) {
            return texts.get(key);
        }
    };

    @Before
    public void setUp() {

    }

    @Test
    public void testMBNTagger() throws IOException {
        Catalogue c = new Catalogue();

        c.add("AAA", Arrays.asList("111", "222"));
        c.add("BBB", Arrays.asList("333", "444"));
        c.add("CCC", Arrays.asList("555", "666", "777"));

        Tagger tagger = new MNBTagger(testDataProvider);

        tagger.learn(c);

        // some of weights in each class should be equal to 1
        tagger.getKeywords().forEach((tag, words) -> {
            double sum = words.values().stream().mapToDouble(Keyword::weight).sum();
            assertEquals(Math.abs(sum), 1, 0.0001);
        });

        Keywords kws = tagger.getKeywords();

        assertEquals(-0.1420, kws.byTag("AAA").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1063, kws.byTag("AAA").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1420, kws.byTag("AAA").get("cccc").weight(), 0.0001);
        assertEquals(-0.2032, kws.byTag("AAA").get("dddd").weight(), 0.0001);
        assertEquals(-0.2032, kws.byTag("AAA").get("ssss").weight(), 0.0001);
        assertEquals(-0.2032, kws.byTag("AAA").get("vvvv").weight(), 0.0001);

        assertEquals(-0.1159, kws.byTag("BBB").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1520, kws.byTag("BBB").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1520, kws.byTag("BBB").get("cccc").weight(), 0.0001);
        assertEquals(-0.1520, kws.byTag("BBB").get("dddd").weight(), 0.0001);
        assertEquals(-0.2139, kws.byTag("BBB").get("ssss").weight(), 0.0001);
        assertEquals(-0.2139, kws.byTag("BBB").get("vvvv").weight(), 0.0001);

        assertEquals(-0.1791, kws.byTag("CCC").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1791, kws.byTag("CCC").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1418, kws.byTag("CCC").get("cccc").weight(), 0.0001);
        assertEquals(-0.1418, kws.byTag("CCC").get("dddd").weight(), 0.0001);
        assertEquals(-0.1791, kws.byTag("CCC").get("ssss").weight(), 0.0001);
        assertEquals(-0.1791, kws.byTag("CCC").get("vvvv").weight(), 0.0001);

        tagger.test(c);
    }

    @Test
    public void testCNBTagger() throws IOException {
        Catalogue c = new Catalogue();

        c.add("AAA", Arrays.asList("111", "222"));
        c.add("BBB", Arrays.asList("333", "444"));
        c.add("CCC", Arrays.asList("555", "666", "777"));

        Tagger tagger = new CNBTagger(testDataProvider);

        tagger.learn(c);

        // some of weights in each class should be equal to 1
        tagger.getKeywords().forEach((tag, words) -> {
            double sum = words.values().stream().mapToDouble(Keyword::weight).sum();
            assertEquals(Math.abs(sum), 1, 0.0001);
        });

        Keywords kws = tagger.getKeywords();

        assertEquals(-0.1414, kws.byTag("AAA").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1675, kws.byTag("AAA").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1414, kws.byTag("AAA").get("cccc").weight(), 0.0001);
        assertEquals(-0.1414, kws.byTag("AAA").get("dddd").weight(), 0.0001);
        assertEquals(-0.2042, kws.byTag("AAA").get("ssss").weight(), 0.0001);
        assertEquals(-0.2042, kws.byTag("AAA").get("vvvv").weight(), 0.0001);

        assertEquals(-0.1631, kws.byTag("BBB").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1370, kws.byTag("BBB").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1370, kws.byTag("BBB").get("cccc").weight(), 0.0001);
        assertEquals(-0.1631, kws.byTag("BBB").get("dddd").weight(), 0.0001);
        assertEquals(-0.2000, kws.byTag("BBB").get("ssss").weight(), 0.0001);
        assertEquals(-0.2000, kws.byTag("BBB").get("vvvv").weight(), 0.0001);

        assertEquals(-0.1131, kws.byTag("CCC").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1131, kws.byTag("CCC").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1378, kws.byTag("CCC").get("cccc").weight(), 0.0001);
        assertEquals(-0.1725, kws.byTag("CCC").get("dddd").weight(), 0.0001);
        assertEquals(-0.2318, kws.byTag("CCC").get("ssss").weight(), 0.0001);
        assertEquals(-0.2318, kws.byTag("CCC").get("vvvv").weight(), 0.0001);

        tagger.test(c);
    }

    @Test
    public void testTWCNBTagger() throws IOException {
        Catalogue c = new Catalogue();

        c.add("AAA", Arrays.asList("111", "222"));
        c.add("BBB", Arrays.asList("333", "444"));
        c.add("CCC", Arrays.asList("555", "666", "777"));

        Tagger tagger = new TWCNBTagger(testDataProvider);

        tagger.learn(c);

        // some of weights in each class should be equal to 1
        tagger.getKeywords().forEach((tag, words) -> {
            double sum = words.values().stream().mapToDouble(Keyword::weight).sum();
            assertEquals(Math.abs(sum), 1, 0.0001);
        });

        Keywords kws = tagger.getKeywords();

        assertEquals(-0.1470, kws.byTag("AAA").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1786, kws.byTag("AAA").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1699, kws.byTag("AAA").get("cccc").weight(), 0.0001);
        assertEquals(-0.1574, kws.byTag("AAA").get("dddd").weight(), 0.0001);
        assertEquals(-0.1768, kws.byTag("AAA").get("ssss").weight(), 0.0001);
        assertEquals(-0.1703, kws.byTag("AAA").get("vvvv").weight(), 0.0001);

        assertEquals(-0.1667, kws.byTag("BBB").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1480, kws.byTag("BBB").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1600, kws.byTag("BBB").get("cccc").weight(), 0.0001);
        assertEquals(-0.1781, kws.byTag("BBB").get("dddd").weight(), 0.0001);
        assertEquals(-0.1769, kws.byTag("BBB").get("ssss").weight(), 0.0001);
        assertEquals(-0.1704, kws.byTag("BBB").get("vvvv").weight(), 0.0001);

        assertEquals(-0.1219, kws.byTag("CCC").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1347, kws.byTag("CCC").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1539, kws.byTag("CCC").get("cccc").weight(), 0.0001);
        assertEquals(-0.1764, kws.byTag("CCC").get("dddd").weight(), 0.0001);
        assertEquals(-0.2066, kws.byTag("CCC").get("ssss").weight(), 0.0001);
        assertEquals(-0.2066, kws.byTag("CCC").get("vvvv").weight(), 0.0001);

        tagger.test(c);
    }
}
