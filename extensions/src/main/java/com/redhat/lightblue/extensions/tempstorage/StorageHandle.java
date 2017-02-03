package com.redhat.lightblue.extensions.tempstorage;

import java.util.Iterator;

import com.redhat.lightblue.metadata.DocId;

import com.redhat.lightblue.util.JsonDoc;

public interface StorageHandle {

    /**
     * Close, and cleanup the storage handle. This should be called
     * after the work with the storage is completed. It marks all the
     * documents associated with this storage handle as stale.
     */
    void close();

    /**
     * Add documents to the storage
     */
    void add(Iterator<JsonDoc> documentStream);

    /**
     * Stream documents from the storage. Iteration order is arbitrary.
     */
    Iterator<JsonDoc> getDocuments();

    /**
     * If there is an id extractor defined for the handle, returns the
     * document with the given id.
     */
    JsonDoc getDocumentById(DocId id);
}
