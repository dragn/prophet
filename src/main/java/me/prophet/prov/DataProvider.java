package me.prophet.prov;

/**
 * Provides an access to the documents, specified by keys (which are specified in Catalogue).
 *
 * User: dsabelnikov
 * Date: 8/26/14
 * Time: 7:07 PM
 */
public interface DataProvider {

    /**
     * Returns the document text. Key is specific to the provider's implementation.
     */
    public String getDocument(String key);
}
