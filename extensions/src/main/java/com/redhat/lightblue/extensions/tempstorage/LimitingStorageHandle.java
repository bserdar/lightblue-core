package com.redhat.lightblue.extensions.tempstorage;

import java.util.Iterator;

import com.redhat.lightblue.metadata.DocId;
import com.redhat.lightblue.metadata.DocIdExtractor;

import com.redhat.lightblue.util.JsonDoc;

/**
 * An implementation of storage handle that stores the documents in
 * memory up to a certain number. OInce that number is reached, the
 * documents are written to the delegate handle, which is to store
 * these documents in a temporary persistent storage.
 */
public class LimitingStorageHandle implements StorageHandle {

    private final int documentLimit;
    private final StorageHandle tempDelegate;
    private MemoryStorageHandle memDelegate;
    private boolean mem=true;
    
    /**
     * Constructs a storage handle with the given delegate and document count limit
     */
    public LimitingStorageHandle(DocIdExtractor idExtractor,
                                 StorageHandle delegate,
                                 int documentLimit) {
        this.tempDelegate=delegate;
        this.documentLimit=documentLimit;
        memDelegate=new MemoryStorageHandle(idExtractor);
    }
    
    @Override
    public void close() {
        if(memDelegate!=null)
            memDelegate.close();
        memDelegate=null;
        memDelegate.close();
    }

    @Override
    public void add(Iterator<JsonDoc> documentStream) {
        if(mem) {
            memDelegate.add(documentStream);
            if(memDelegate.size()>documentLimit) {
                tempDelegate.add(memDelegate.getDocuments());
                memDelegate.close();
                memDelegate=null;
                mem=false;
            }
        } else {
            tempDelegate.add(documentStream);
        }
    }

    @Override
    public Iterator<JsonDoc> getDocuments() {
        return mem?memDelegate.getDocuments():tempDelegate.getDocuments();
    }

    @Override
    public JsonDoc getDocumentById(DocId id) {
        return mem?memDelegate.getDocumentById(id):tempDelegate.getDocumentById(id);
    }
}
