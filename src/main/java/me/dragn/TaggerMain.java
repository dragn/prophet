package me.dragn;

import java.io.File;
import java.io.StringReader;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class TaggerMain {

    public static void main(String... args) {
        KeywordsFetcher kf = new KeywordsFetcher();

        kf.readCatalogue(new File("./catalogue2.out"));
    }

    public static void fetchYaca() {
        CatalogFetcher cf = new YandexCatalogueFetcher(new File("./catalogue.out"));

        String str = "Музыка|http://yaca.yandex.ru/yca/cat/Culture/Music/Mp3/,http://yaca.yandex.ru/yca/cat/Culture/Music/General/,http://yaca.yandex.ru/yca/cat/Culture/Music/Radio/";
        cf.read(new StringReader(str));

        //cf.readFile(new File("conf/yaca.catalog"));
        cf.fetch();
    }
}
