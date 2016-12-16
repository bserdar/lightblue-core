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
     ( field1 = value1 or field1 = value2
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

    public static class FieldTuple extends List<Path> { }

    
    public static class ValueRange {
        public Value from;
        public Value to;
    }

    public static class SingleFieldIndexInfo {
        public Path field;
        public List<ValueRange> valueRanges;
    }
    
    public static class IndexInfo {
        public final Map<Path,SingleFieldIndexInfo> singleFields=new HashMap<>();

        public IndexInfo() {}

        /**
         * Index info for one field, with a single value
         */
        public IndexInfo(Path field,BinaryComparisonOperator op,Value value) {
            FieldIndexInfo finfo=new FieldIndexInfo(field,op,value);
            fields.put(finfo.field,finfo);
        }

        /**
         * Index info for one field, with a list of values
         */
        public IndexInfo(Path field,NaryRelationalOperator op,List<Value> values) {
            FieldIndexInfo finfo=new FieldIndexInfo(field,op,values);
            fields.put(finfo.field,finfo);
        }

        /**
         * Index info for one array field, with a list of values, for contains._any
         */
        public IndexInfo(Path field,ContainsOperator op,List<Value> values) {
            FieldIndexInfo finfo=new FieldIndexInfo(field,op,values);
            fields.put(finfo.field,finfo);
        }

        public IndexInfo invert() {
            for(FieldIndexInfo f:fields.values())
                f.invert();
            return this;
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
