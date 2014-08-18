package me.dragn;

import java.io.File;
import java.io.StringReader;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class TaggerMain {

    public static void main(String... args) {
        fetchKeywords();
        //fetchYaca();
    }

    public static void fetchKeywords() {
        KeywordsFetcher kf = new KeywordsFetcher();
        kf.readCatalogue(new File("./catalogue2.out"));
        kf.fetchKeywords();
        kf.cleanOut();
        kf.storeKeywords(new File("./keywords.out"));
    }

    public static void fetchYaca() {
        CatalogFetcher cf = new YandexCatalogueFetcher(new File("./catalogue.out"));

        String str = "Книги и журналы|http://yaca.yandex.ru/yca/cat/Culture/Literature/";
        cf.read(new StringReader(str));

        //cf.readFile(new File("conf/yaca.catalog"));
        cf.fetch();
    }
}
