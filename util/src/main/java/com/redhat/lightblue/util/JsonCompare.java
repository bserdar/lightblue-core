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
package com.redhat.lightblue.util;

public class JsonCompare {

    public interface ValueComparator {
        boolean equals(Path field1,JsonNode value1,Path field2,JsonNode value2);
    }

    private class Distance {
        int numFields;
        int numDifferent;

        public Distance() {}
        
        public Distance(int numFields,int numDifferent) {
            this.numFields=numFields;
            this.numDifferent=numDifferent;
        }

        public void add(Distance d) {
            numFields+=d.numFields;
            numDifferent+=d.numDifferent;
        }
    }

    public List<Delta> compare(JsonNode doc1,JsonNode doc2,ValueComparator cmp) {
        MutablePath field1=new MutablePath();
        MutablePath field2=new MutablePath();
        List<Delta> diff=new ArrayList<>();
        compareNode(diff,field1,node1,field2,node2,cmp);
        return diff;
    }

    private Distance compareNode(List<Delta> diff,
                                 MutablePath field1,
                                 JsonNode node1,
                                 MutablePath field2,
                                 JsonNode node2,
                                 ValueComparator cmp) {
        if(node1 instanceof ValueNode && node2 instanceof ValueNode) {
            return compareValue(diff,field1,node1,field2,node2,cmp);
        } else if(node1 instanceof ArrayNode && node2 instanceof ArrayNode) {
            return compareArray(diff,field1,(ArrayNode)node1,field2,(ArrayNode)node2,cmp);
        } else if(node1 instanceof ObjectNode && node2 instanceof ObjectNode) {
            return compareObject(diff,field1,(ObjectNode)node1,field2,(ObjectNode)node2,cmp);
        } else {
            diff.add(new Delta(Delta.node_diff,field1,node1,field2,node2));
            return new Distance(1,1);
        }
    }

    private Distance compareValue(List<Delta> diff,
                                  MutablePath field1,
                                  JsonNode node1,
                                  MutablePath field2,
                                  JsonNode node2,
                                  ValueComparator cmp) {
        if(!cmp.equals(field1,value1,field1,value2)) {
            diff.add(new Delta(Delta.value_diff,field1,node1,field2,node2));
            return new Distance(0,1);
        }
        return new Distance(0,0);
    }

    private Distance compareObject(List<Delta> diff,
                                   MutablePath field1,
                                   ObjectNode node1,
                                   MutablePath field2,
                                   ObjectNode node2,
                                   ValueComparator cmp) {
        Distance ret=new Distance();
        for(Iterator<Map.Entry<String,JsonNode>> fields=node1.fields();fields.hasNext();) {
            Map.Entry<String,JsonNode> field=fields.next();
            ret.numFields++;
            field1.push(field.getKey());
            field2.push(field.getKey());
            JsonNode value1=field.getValue();
            JsonNode value2=node2.get(field.getKey());
            if(value2 == null||value2 instanceof NullNode) {
                diff.add(new Delta(Delta.removeField,field1,value1,field2,null));
                ret.numDifferent++;
            } else {
                ret.add(compareNode(diff,field1,value1,field2,value2,cmp));
            }
        }
        for(Iterator<String> fieldNames=node2.fieldNames();fieldNames.hasNext();) {
            String fieldName=fieldNames.next();
            if(!node1.has(fieldName)) {
                field2.push(fieldName);
                diff.add(new Delta(Delta.newField,field2,null,field2,node2.get(fieldName)));
                ret.numFields++;
                ret.numDifferent++;
            }
        }
        return ret;
    }

    private Distance compareArray(List<Delta> diff,
                                  MutablePath field1,
                                  ArrayNode node1,
                                  MutablePath field2,
                                  ArrayNode node2,
                                  ValueComparator cmp) {
    }
}
