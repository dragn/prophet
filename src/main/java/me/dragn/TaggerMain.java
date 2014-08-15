package me.dragn;

import java.io.File;
import java.io.StringReader;

/**
 * Created by dsabe_000 on 8/14/2014.
 */
public class TaggerMain {

    public static void main(String... args) {
        CatalogFetcher cf = new YandexCatalogueFetcher(new File("./catalogue.out"));

        String str = "" +
                "Фото и видео|http://yaca.yandex.ru/yca/cat/Culture/Photography/";
        cf.read(new StringReader(str));

        //cf.readFile(new File("conf/yaca.catalog"));
        cf.fetch();
    }
}
