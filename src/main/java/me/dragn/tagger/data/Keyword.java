package me.dragn.tagger.data;

/**
 * User: dsabelnikov
 * Date: 8/21/14
 * Time: 5:29 PM
 */
public class Keyword {
    private String word;
    private Double weight;

    public Keyword(String word, Double weight) {
        this.word = word;
        this.weight = weight;
    }

    public String word() {
        return word;
    }

    public Double weight() {
        return weight;
    }
}
