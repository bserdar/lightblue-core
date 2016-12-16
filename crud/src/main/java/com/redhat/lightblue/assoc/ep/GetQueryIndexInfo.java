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

import java.util.Map;
import java.util.HashMap;

import com.redhat.lightblue.query.*;

import com.redhat.lightblue.util.Path;

public class GetQueryIndexInfo extends QueryIteratorSkeleton<IndexInfo> {

    /**
     * We need a data structure to keep values and ranges of values
     * for fields and tuples. For instance, a query of the form
     *
     *  field = value
     *
     * would create an entry { field: [value,value] }
     *
     * A query of the form
     *
     *  field1 = value1 and field2 = value2
     *
     * would create an entry {field1: [value1,value1] , field2: [value2, value2] }
     *
     * A query of the form
     *
     * field1 = value1 or field1 = value2
     *
     * would create an entry {field1: { [value1,value1], [value2,vakue2] }
     *
     * If several predicates are under an array elemMatch:
     *
     *   array: a, elemMatch: { field1=value1 and field2=value2 }
     *
     * we get
     *
     *  { [ a.field1, a.field2 ]: { [ [value1,value2] , [value1,value2] ] }
     *
     * So the most general case is:
     * 
     *    field n-tuple : list< Range < value tuple > >
     */

    public static class FieldTuple extends HashSet<Path> {
        FieldTuple() {}
        
        FieldTuple(Path p) {
            add(p);
        }
    }

    public static class ValueTuple extends HashMap<Path,Value> {
    }


    public static class ValueRange  {
        public final ValueTuple from=new ValueTuple();
        public final ValueTuple to=new ValueTuple();

        /**
         * Ctor for a range for a single field
         */
        ValueRange(Path path,Value from,Value to) {
            this.from.put(path,from);
            this.to.put(path,to);
        }
    }

    public static class FieldTupleIndexInfo {
        public FieldTuple fields;
        public final List<ValueRange> values=new ArrayList<>();

        FieldTupleIndexInfo(FieldTuple fields,ValueRange value) {
            this.fields=fields;
            values.add(value);
        }

        /**
         * Invert all ranges
         */
        void invert() {
            
        }
    }
    
    public static class IndexInfo extends HashMap<FieldTuple,FieldTupleIndexInfo> {

        public IndexInfo() {}

        /**
         * Index info for one field, with a single value
         */
        public IndexInfo(Path field,BinaryComparisonOperator op,Value value) {
            ValueRange range;
            switch(op) {
            case _eq:
                range=new ValueRange(field,value,value);
                break;
                
            case _neq:
                range=null;
                break;
            case _lte:
            case _lt:
                range=new ValueRange(field,null,value);
                break;
                
            case _gt:
            case _gte:
                range=new ValueRange(field,value,null);
                break;
            }
            if(range!=null) {
                FieldTuple ft=new FieldTuple(field);
                put(ft,new FieldTupleIndexInfo(ft,range));
            }
        }

        /**
         * Index info for one field, with a list of values
         */
        public IndexInfo(Path field,NaryRelationalOperator op,List<Value> values) {
            if(op==NaryRelationalOperator._in) {
                FieldTuple ft=new FieldTuple(field);
                put(ft,new FieldTupleIndexInfo(ft,values.stream().map(v->new ValueRange(field,v,v)).collect(Collectors.toList())));
            }
        }

        /**
         * Index info for one array field, with a list of values, for contains._any
         */
        public IndexInfo(Path field,ContainsOperator op,List<Value> values) {
            if(op==ContainsOperator._any||op==ContainsOperator._all) {
                FieldTuple ft=new FieldTuple(field);
                put(ft,new FieldTupleIndexInfo(ft,values.stream().map(v->new ValueRange(field,v,v)).collect(Collectors.toList())));
            }
        }

        public IndexInfo invert() {
            values().stream().forEach(FieldTupleIndexInfo::invert);
        }

        public IndexInfo intersect(IndexInfo info) {
        }

        public IndexInfo union(IndexInfo info) {
        }

        public IndexInfo prepend(Path p) {
            
        }
    }

    /**
     * Can't do anything useful with a field comparison expression
     */
    protected IndexInfo itrNaryFieldRelationalExpression(NaryFieldRelationalExpression q, Path context) {
        return new IndexInfo();
    }

    /**
     * Can't do anything useful with a field comparison expression
     */
    @Override
    protected IndexInfo itrFieldComparisonExpression(FieldComparisonExpression q, Path context) {
        return new IndexInfo();
    }

    @Override
    protected IndexInfo itrValueComparisonExpression(ValueComparisonExpression q, Path context) {
        // _neq is useless for index use
        if(q.getOp()==BinaryComparisonOperator._neq)
            return new IndexInfo();
        else
            return new IndexInfo(q.getField(),q.getOp(),q.getValue());
    }
        

    @Override
    protected IndexInfo itrAllMatchExpression(AllMatchExpression q, Path context) {
        return new IndexInfo();
    }

    @Override
    protected IndexInfo itrRegexMatchExpression(RegexMatchExpression q, Path context) {
        return new IndexInfo();
    }

    @Override
    protected IndexInfo itrNaryValueRelationalExpression(NaryValueRelationalExpression q, Path context) {
        return new IndexInfo(q.getField(),q.getOp(),q.getValues());
    }

    @Override
    protected IndexInfo itrArrayContainsExpression(ArrayContainsExpression q, Path context) {
        if(q.getOp()==ContainsOperator._any)
            return new IndexInfo(q.getArray(),q.getOp(),q.getValues());
        else
            reutrn new IndexInfo();
    }

    @Override
    protected IndexInfo itrUnaryLogicalExpression(UnaryLogicalExpression q, Path context) {
        return iterate(q.getQuery(),context).invert();
    }

    @Override
    protected IndexInfo itrNaryLogicalExpression(NaryLogicalExpression q, Path context) {
        IndexInfo ret=new IndexInfo();
        for(QuryExpression x:q.getQueries()) {
            if(q.getOp()==NaryLogicalOperator._and)
                ret.intersect(iterate(x,context));
            else
                ret.union(iterate(x,context));
        }
        return ret;
    }

    @Override
    protected IndexInfo itrArrayMatchExpression(ArrayMatchExpression q, Path context) {
        IndexInfo info=iterate(q.getElemMatch(),context);
        info.prepend(q.getArray());
        return info;
    }
}
