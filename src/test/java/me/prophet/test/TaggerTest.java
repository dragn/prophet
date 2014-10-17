package me.prophet.test;

import me.prophet.data.Catalogue;
import me.prophet.data.Keyword;
import me.prophet.data.Keywords;
import me.prophet.prov.DataProvider;
import me.prophet.tag.CNBTagger;
import me.prophet.tag.MNBTagger;
import me.prophet.tag.TWCNBTagger;
import me.prophet.tag.Tagger;
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

        Tagger tagger = new MNBTagger();
        tagger.setProvider(testDataProvider);

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

        Tagger tagger = new CNBTagger();
        tagger.setProvider(testDataProvider);

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

        Tagger tagger = new TWCNBTagger();
        tagger.setProvider(testDataProvider);

        tagger.learn(c);

        // some of weights in each class should be equal to 1
        tagger.getKeywords().forEach((tag, words) -> {
            double sum = words.values().stream().mapToDouble(Keyword::weight).sum();
            assertEquals(Math.abs(sum), 1, 0.0001);
        });

        Keywords kws = tagger.getKeywords();

        assertEquals(-0.1446, kws.byTag("AAA").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1800, kws.byTag("AAA").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1707, kws.byTag("AAA").get("cccc").weight(), 0.0001);
        assertEquals(-0.1525, kws.byTag("AAA").get("dddd").weight(), 0.0001);
        assertEquals(-0.1794, kws.byTag("AAA").get("ssss").weight(), 0.0001);
        assertEquals(-0.1728, kws.byTag("AAA").get("vvvv").weight(), 0.0001);

        assertEquals(-0.1625, kws.byTag("BBB").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1500, kws.byTag("BBB").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1603, kws.byTag("BBB").get("cccc").weight(), 0.0001);
        assertEquals(-0.1740, kws.byTag("BBB").get("dddd").weight(), 0.0001);
        assertEquals(-0.1799, kws.byTag("BBB").get("ssss").weight(), 0.0001);
        assertEquals(-0.1733, kws.byTag("BBB").get("vvvv").weight(), 0.0001);

        assertEquals(-0.1208, kws.byTag("CCC").get("aaaa").weight(), 0.0001);
        assertEquals(-0.1366, kws.byTag("CCC").get("bbbb").weight(), 0.0001);
        assertEquals(-0.1547, kws.byTag("CCC").get("cccc").weight(), 0.0001);
        assertEquals(-0.1745, kws.byTag("CCC").get("dddd").weight(), 0.0001);
        assertEquals(-0.2066, kws.byTag("CCC").get("ssss").weight(), 0.0001);
        assertEquals(-0.2066, kws.byTag("CCC").get("vvvv").weight(), 0.0001);

        tagger.test(c);
    }
}
