package me.dragn;

/**
* User: dsabelnikov
* Date: 8/20/14
* Time: 3:06 PM
*/
public class Word implements Comparable<Word> {

    private String string;
    private Double frequency = 0.;

    public Word(String string, double frequency) {
        this.string = string;
        this.frequency = frequency;
    }

    public String string() {
        return string;
    }

    public Double frequency() {
        return frequency;
    }

    @Override
    public int compareTo(Word o) {
        return - this.frequency.compareTo(o.frequency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Word word = (Word) o;
        return !(string != null ? !string.equals(word.string) : word.string != null);
    }

    @Override
    public int hashCode() {
        return string != null ? string.hashCode() : 0;
    }
}
