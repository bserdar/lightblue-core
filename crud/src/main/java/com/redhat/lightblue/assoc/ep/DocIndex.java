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

import com.redhat.lightblue.metadata.DocId;

import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.Path;

public class DocIndex {
    
    private final HashMap<DocId,JsonDoc> index=new HashMap<>();

    private static class SimpleField {
        final Path field;
        final boolean array;
        final Type type;

        public SimpleField(Path field,EntityMetadata md) {
            this.field=field;
            this.array=this.field.nAnys()>0;
            this.type=md.resolve(field).getType();
        }

        public SimpleField(Path field,FieldTreeNode context) {
            this.field=field;
            this.array=this.field.nAnys()>0;
            this.type=context.resolve(field).getType();
        }
    }

    private static class ArrayField {
        final Path array;
        final SimpleField[] fields;

        ArrayField(IndexInfo.ArrayIndex ai,EntityMetadata md) {
            this.array=ai.array;
            FieldTreeNode arrayNode=((ArrayField)md.resolve(ai.array)).getElement();
            List<Path> fieldList=new ArrayList(ai.fields.size());
            for(IndexInfo.FieldIndex fi:ai.fields)
                if(fi.field.nAnys()==0)
                    fieldList.add(new SimpelField(fi.field,arrayNode));
            this.fields=fieldList.toArray(new SimpleField[fieldList.size()]);
        }

    }
    
    private final SimpleField[] simpleFields;
    private final ArrayField[] arrayFields;
    private final int idSize;

    /**
     * Separate out the simple and array fields from the index info
     * Arrays should not have nested arrays in them, we don't know
     * how to deal with those
     *
     * We will create DocIds using object for simpleField:arrayField
     */
    public DocIndex(IndexInfo indexInfo,EntityMetadata md) {
        List<SimpleField> sf=new ArrayList<>();
        List<ArrayField> af=new ArrayList<>();
        for(IndexInfo.TermIndex ix:indexInfo.getIndexes()) {
            if(ix instanceof IndexInfo.FieldIndex) {
                sf.add( new SimpleField(((IndexInfo.FieldIndex)ix).field,md));
            } else {
                ArrayField arr=new ArrayField((IndexInfo.ArrayIndex)ix,md);
                if(arr.fields.length>0)
                    af.add(arr);
            }
        }
        this.simpleFields=sf.toArray(new Path[sf.size()]);
        this.arrayFields=af.toArray(new ArrayField[af.size()]);
        int n=0;
        for(ArrayField x:arrayFields)
            n+=x.fields.length;
        idSize=n+simpleFields.length;
    }

    public void add(JsonDoc doc) {
        List<List<Object>> values=new ArrayList<>(idSize);
        for(SimpleField f:simpleFields) {
            if(f.array)
                values.add(getValues(doc,f.type,f.field));
            else {
                ArrayList l=new ArrayList(1);
                l.add(f.type.fromJson(doc.get(f.field)));
                values.add(l);
            }
        }
        for(ArrayField a:arrayFields) {
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
            index.put(new DocId(idvalues,-1),doc);
        }
    }
}
