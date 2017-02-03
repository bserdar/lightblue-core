package com.redhat.lightblue.extensions.tempstorage;

import com.redhat.lightblue.extensions.Extension;

import com.redhat.lightblue.metadata.DocIdExtractor;

/**
 * Temporary storage support for large resultsets
 *
 */
public interface TempStorageSupport extends Extension {

    /**
     * Create a new handle to store temporary documents. 
     *
     * @param idExtractor Optional ID extractor. Given a document, returns the document id
     *
     * If idExtractor is not given, documents won't be accessible by
     * their id, but they can still be streamed. 
     */
    StorageHandle createTempStorage(DocIdExtractor idExtractor);
}
