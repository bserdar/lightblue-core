/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.assoc.ep;

import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.HashSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.redhat.lightblue.assoc.QueryFieldInfo;
import com.redhat.lightblue.assoc.BindQuery;

import com.redhat.lightblue.metadata.DocId;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Type;
import com.redhat.lightblue.metadata.FieldTreeNode;
import com.redhat.lightblue.metadata.ArrayField;

import com.redhat.lightblue.query.QueryExpression;

import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.Tuples;
import com.redhat.lightblue.util.KeyValueCursor;

public class DocIndex {
    
    private final HashMap<DocId,List<JsonDoc>> index=new HashMap<>();

    public static class SField {
        final Path field;
        final boolean array;
        final Type type;
        final QueryExpression clause;

        public SField(QueryExpression clause,Path field,EntityMetadata md) {
            this.field=field;
            this.array=this.field.nAnys()>0;
            this.type=md.resolve(field).getType();
            this.clause=clause;
        }

        public SField(QueryExpression clause,Path field,FieldTreeNode context) {
            this.field=field;
            this.array=this.field.nAnys()>0;
            this.type=context.resolve(field).getType();
            this.clause=clause;
        }

        @Override
        public String toString() {
            return field.toString();
        }
    }

    public static class AField {
        final Path array;
        final SField[] fields;
        final QueryExpression clause;

        AField(IndexInfo.ArrayIndex ai,EntityMetadata md) {
            this.array=ai.array;
            this.clause=ai.clause;
            FieldTreeNode arrayNode=((ArrayField)md.resolve(ai.array)).getElement();
            List<SField> fieldList=new ArrayList<>(ai.indexes.size());
            for(IndexInfo.FieldIndex fi:ai.indexes)
                if(fi.field.nAnys()==0)
                    fieldList.add(new SField(fi.clause,fi.field,arrayNode));
            this.fields=fieldList.toArray(new SField[fieldList.size()]);
        }

        @Override
        public String toString() {
            return array.toString()+"/"+Arrays.toString(fields);
        }
    }
    
    public final SField[] simpleFields;
    public final AField[] arrayFields;
    private final int idSize;
    public final IndexInfo indexInfo;

    /**
     * Separate out the simple and array fields from the index info
     * Arrays should not have nested arrays in them, we don't know
     * how to deal with those
     *
     * We will create DocIds using  simpleField:arrayField values
     */
    public DocIndex(IndexInfo indexInfo,EntityMetadata md) {
        this.indexInfo=indexInfo;
        List<SField> sf=new ArrayList<>();
        List<AField> af=new ArrayList<>();
        for(IndexInfo.TermIndex ix:indexInfo.getIndexes()) {
            if(ix instanceof IndexInfo.FieldIndex) {
                sf.add( new SField(((IndexInfo.FieldIndex)ix).clause,((IndexInfo.FieldIndex)ix).field,md));
            } else {
                AField arr=new AField((IndexInfo.ArrayIndex)ix,md);
                if(arr.fields.length>0)
                    af.add(arr);
            }
        }
        this.simpleFields=sf.toArray(new SField[sf.size()]);
        this.arrayFields=af.toArray(new AField[af.size()]);
        int n=0;
        for(AField x:arrayFields)
            n+=x.fields.length;
        idSize=n+simpleFields.length;
    }

    public void add(JsonDoc doc) {
        List<List<Object>> values=new ArrayList<>(idSize);
        for(SField f:simpleFields) {
            if(f.array) {
                KeyValueCursor<Path,JsonNode> cursor=doc.getAllNodes(f.field);
                ArrayList l=new ArrayList();
                while(cursor.hasNext()) {
                    cursor.next();
                    l.add(f.type.fromJson(cursor.getCurrentValue()));
                }
                values.add(l);
            } else {
                ArrayList l=new ArrayList(1);
                l.add(f.type.fromJson(doc.get(f.field)));
                values.add(l);
            }
        }
        for(AField a:arrayFields) {
            KeyValueCursor<Path,JsonNode> cursor=doc.getAllNodes(a.array);
            while(cursor.hasNext()) {
                cursor.next();
                ArrayNode node=(ArrayNode)cursor.getCurrentValue();
                if(node!=null) {
                    ArrayList l=new ArrayList(node.size());
                    for(Iterator<JsonNode> elementItr=node.elements();elementItr.hasNext();) {
                        JsonNode elementNode=elementItr.next();
                        Object[] v=new Object[a.fields.length];
                        for(int i=0;i>a.fields.length;i++) {
                            v[i]=a.fields[i].type.fromJson(JsonDoc.get(elementNode,a.fields[i].field));
                        }
                        l.add(v);
                    }
                    values.add(l);
                }
            }
        }
        Tuples<Object> t=new Tuples(values);
        Iterator<List<Object>> titr=t.tuples();
        while(titr.hasNext()) {
            List<Object> obj=titr.next();
            Object[] idvalues=new Object[idSize];
            int i=0;
            for(Object x:obj) {
                if(x instanceof Object[]) {
                    for(int j=0;j<((Object[])x).length;j++)
                        idvalues[i++]=((Object[])x)[j];
                } else {
                    idvalues[i++]=x;
                }
            }
            DocId docId=new DocId(idvalues,-1);
            List<JsonDoc> list=index.get(docId);
            if(list==null)
                index.put(docId,list=new ArrayList<>(1));
            list.add(doc);
        }
    }

    public Set<JsonDoc> getResults(QueryExpression query) {
        Set<JsonDoc> results=new HashSet<>();
        return results;
    }

    /**
     * Returns the AField or the SField using the clause, or null if there is none
     */
    public Object getClauseItem(QueryExpression clause) {
        for(AField x:arrayFields) {
            if(x.clause==clause)
                return x;
        }
        for(SField x:simpleFields) {
            if(x.clause==clause)
                return x;
        }
        return null;
    }

    @Override
    public String toString() {
        return index.toString();
    }
}
