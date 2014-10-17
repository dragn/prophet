package me.prophet.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * User: dsabelnikov
 * Date: 10/17/14
 * Time: 12:57 PM
 */
public class Knowledge {
    private String taggerClassName;
    private Keywords keywords;

    public Knowledge(String taggerClassName, Keywords keywords) {
        this.taggerClassName = taggerClassName;
        this.keywords = keywords;
    }

    public void toFile(String path) throws IOException {
        try (ObjectOutputStream writer = new ObjectOutputStream(Files.newOutputStream(Paths.get(path)))) {
            writer.writeObject(this);
        }
    }

    public static Knowledge fromFile(String path) throws IOException {
        try (ObjectInputStream writer = new ObjectInputStream(Files.newInputStream(Paths.get(path)))) {
            try {
                return (Knowledge) writer.readObject();
            } catch (ClassNotFoundException | ClassCastException e) {
                System.err.println("Error while reading knowledge file");
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getTaggerClassName() {
        return taggerClassName;
    }

    public void setTaggerClassName(String taggerClassName) {
        this.taggerClassName = taggerClassName;
    }

    public Keywords getKeywords() {
        return keywords;
    }

    public void setKeywords(Keywords keywords) {
        this.keywords = keywords;
    }
}
