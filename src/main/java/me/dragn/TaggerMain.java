package me.dragn;

import org.apache.commons.lang3.tuple.Pair;

import java.io.StringReader;
import java.util.List;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class TaggerMain {

    public static void main(String... args) {
        CatalogFetcher cf = new YandexCatalogueFetcher();

        String str =
                "Социальные сети|http://yaca.yandex.ru/yca/cat/Entertainment/community/\n" +
                        "Эзотерика|http://yaca.yandex.ru/yca/cat/Entertainment/Occultism/\n" +
                        "Эротика|http://yaca.yandex.ru/yca/cat/Entertainment/Erotica/";

        cf.read(new StringReader(str));

        for (Pair<String, List<String>> pair : cf.fetch()) {
            System.out.println(pair.getLeft() + pair.getRight());
        }
    }
}
