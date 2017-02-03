package com.redhat.lightblue.extensions.tempstorage;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.Test;
import org.junit.Assert;

import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.Path;

public class LimitingStorageHandleTest {

    @Test
    public void testStorageSwitch() {
        MemoryStorageHandle tempStorage=new MemoryStorageHandle(null);
        LimitingStorageHandle st=new LimitingStorageHandle(null,tempStorage,10);

        ArrayList<JsonDoc> list=new ArrayList<>();
        for(int i=0;i<9;i++) {
            JsonDoc doc=new JsonDoc(JsonNodeFactory.instance.objectNode());
            doc.modify(new Path("id"),JsonNodeFactory.instance.numberNode(i),true);
            list.add(doc);
        }
        st.add(list.iterator());
        Assert.assertEquals(0,tempStorage.size());
        List<JsonDoc> result=new ArrayList<>();
        Iterator<JsonDoc> itr=st.getDocuments();
        while(itr.hasNext())
            result.add(itr.next());
        Assert.assertEquals(9,result.size());

        list.clear();
        for(int i=10;i<12;i++) {
            JsonDoc doc=new JsonDoc(JsonNodeFactory.instance.objectNode());
            doc.modify(new Path("id"),JsonNodeFactory.instance.numberNode(i),true);
            list.add(doc);
        }
        st.add(list.iterator());
        Assert.assertEquals(11,tempStorage.size());
        result.clear();
        itr=st.getDocuments();
        while(itr.hasNext())
            result.add(itr.next());
        Assert.assertEquals(11,result.size());
        
        
    }
}

