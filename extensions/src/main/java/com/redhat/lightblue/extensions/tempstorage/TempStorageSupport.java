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
     * @param md Metadata for the documents that will be stored in the storage
     *
     */
    StorageHandle createTempStorage(EntityMetadata md);
}
