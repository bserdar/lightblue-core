package com.redhat.lightblue.extensions.tempstorage;

import java.util.Iterator;
import java.util.LinkedHashMap;

import com.redhat.lightblue.metadata.DocIdExtractor;
import com.redhat.lightblue.metadata.DocId;

import com.redhat.lightblue.util.JsonDoc;

/**
 * An implementation of storage handle that stores documents in
 * memory.
 */
public class MemoryStorageHandle implements StorageHandle {

    private LinkedHashMap<Object,JsonDoc> storage;
    private final DocIdExtractor idExtractor;

    public MemoryStorageHandle(DocIdExtractor x) {
        idExtractor=x;
    }

    public int size() {
        return storage==null?0:storage.size();
    }
    
    @Override
    public void close() {
        storage=null;
    }

    @Override
    public void add(Iterator<JsonDoc> documentStream) {
        if(storage==null) {
            storage=new LinkedHashMap<>();
        }
        if(idExtractor==null) {
            while(documentStream.hasNext()) {
                JsonDoc doc=documentStream.next();
                storage.put(doc,doc);
            }
        } else {
            while(documentStream.hasNext()) {
                JsonDoc doc=documentStream.next();
                storage.put(idExtractor.getDocId(doc),doc);
            }
        }
    }

    @Override
    public Iterator<JsonDoc> getDocuments() {
        return storage.values().iterator();
    }
    
    @Override
    public JsonDoc getDocumentById(DocId id) {
        if(idExtractor!=null)
            return storage.get(id);
        else
            throw new IllegalArgumentException("no id defined for storage");
    }
}
