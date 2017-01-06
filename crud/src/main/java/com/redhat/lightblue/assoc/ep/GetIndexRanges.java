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

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.redhat.lightblue.assoc.QueryFieldInfo;
import com.redhat.lightblue.assoc.AssocConstants;

import com.redhat.lightblue.metadata.ArrayField;

import com.redhat.lightblue.query.*;

import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.Error;

/**
 * Given a query and index info, build index
 */
public class GetIndexRanges extends IndexQueryProcessorBase<Map<QueryFieldInfo,IndexFieldValues>> {

    public GetIndexRanges(List<QueryFieldInfo> l) {
        super(l);
    }
    
    /**
     * Can't do anything useful with a field comparison expression
     */
    @Override
    protected Map<QueryFieldInfo,IndexFieldValues>itrNaryFieldRelationalExpression(NaryFieldRelationalExpression q, Path context) {
        return new HashMap<>();
    }

    /**
     * Can't do anything useful with a field comparison expression
     */
    @Override
    protected Map<QueryFieldInfo,IndexFieldValues> itrFieldComparisonExpression(FieldComparisonExpression q, Path context) {
        return new HashMap<>();
    }

    @Override
    protected Map<QueryFieldInfo,IndexFieldValues> itrAllMatchExpression(AllMatchExpression q, Path context) {
        return new HashMap<>();
    }

    /**
     * A value comparison expression returns a range for all operators except _ne. For _ne, it returns empty.
     */
    @Override
    protected Map<QueryFieldInfo,IndexFieldValues> itrValueComparisonExpression(ValueComparisonExpression q, Path context) {
        Map<QueryFieldInfo,IndexFieldValues> ret=new HashMap<>();
        QueryFieldInfo finfo=findFieldInfo(q.getField(),q);
        IndexValues value=null;
        switch(q.getOp()) {
        case _eq:
            value=new IndexRange(finfo.getFieldMd().getType().cast(q.getRvalue()));
            break;
        case _lte:
        case _lt:
            value=new IndexRange(null,finfo.getFieldMd().getType().cast(q.getRvalue()));
            break;
        case _gte:
        case _gt:
            value=new IndexRange(finfo.getFieldMd().getType().cast(q.getRvalue()),null);
            break;
        }
        if(value!=null) {
            ret.put(finfo,new IndexFieldValues(finfo,value));
        }
        return ret;
    }

    /**
     * Returns a pattern if the pattern starts with ^, otherwise returns empty
     */
    @Override
    protected Map<QueryFieldInfo,IndexFieldValues> itrRegexMatchExpression(RegexMatchExpression q, Path context) {
        Map<QueryFieldInfo,IndexFieldValues> ret=new HashMap<>();
        String pattern=q.getRegex();
        String prefix=IndexPrefix.getPrefix(pattern);
        if(prefix.length()>0) {
            QueryFieldInfo finfo=findFieldInfo(q.getField(),q);
            ret.put(finfo,new IndexFieldValues(finfo,new IndexPrefix(prefix)));
        }
        return ret;
    }

    @Override
    protected Map<QueryFieldInfo,IndexFieldValues> itrNaryValueRelationalExpression(NaryValueRelationalExpression q, Path context) {
        Map<QueryFieldInfo,IndexFieldValues> ret=new HashMap<>();
        if(q.getOp()==NaryRelationalOperator._in) {
            QueryFieldInfo finfo=findFieldInfo(q.getField(),q);
            IndexFieldValues fv=new IndexFieldValues(finfo);
            for(Value v:q.getValues()) {
                fv.values.add(new IndexRange(finfo.getFieldMd().getType().cast(v.getValue())));
            }
            ret.put(finfo,new IndexFieldValues(finfo,fv));
        }
        return ret;
    }
    
    @Override
    protected Map<QueryFieldInfo,IndexFieldValues> itrArrayContainsExpression(ArrayContainsExpression q, Path context) {
        Map<QueryFieldInfo,IndexFieldValues> ret=new HashMap<>();
        if(q.getOp()==ContainsOperator._any) {
            QueryFieldInfo finfo=findFieldInfo(q.getArray(),q);
            IndexFieldValues fv=new IndexFieldValues(finfo);
            for(Value v:q.getValues()) {
                fv.values.add(new IndexRange(((ArrayField)finfo.getFieldMd()).getElement().getType().cast(v.getValue())));
            }
            ret.put(finfo,new IndexFieldValues(finfo,fv));
        }
        return ret;
    }
    
    @Override
    protected  Map<QueryFieldInfo,IndexFieldValues> itrUnaryLogicalExpression(UnaryLogicalExpression q, Path context) {
        // Keep it simple: negation makes index useless
        return new HashMap<>();
    }
    
    @Override
    protected Map<QueryFieldInfo,IndexFieldValues> itrNaryLogicalExpression(NaryLogicalExpression q, Path context) {
        Map<QueryFieldInfo,IndexFieldValues> ret=new HashMap<>();
        if(q.getOp()==NaryLogicalOperator._and) {
            // Each nested query returns a range
            // If these are for different fields, we add them to the map
            // If there are common fields, we intersect them
            for(QueryExpression query:q.getQueries()) {
                Map<QueryFieldInfo,IndexFieldValues> subq=super.iterate(query,context);
                for(IndexFieldValues fv:subq.values()) {
                    IndexFieldValues existingValues=ret.get(fv.field);
                    if(existingValues==null) {
                        ret.put(fv.field,fv);
                    } else {
                        // There is already an index range for this field
                        if(!existingValues.intersect(fv))
                            ret.remove(existingValues);
                    }
                }
            }
        } else {
            // If the nested queries refer to the same field, we compute a union, otherwise we return empty
            for(QueryExpression query:q.getQueries()) {
                Map<QueryFieldInfo,IndexFieldValues> subq=super.iterate(query,context);
                if(subq.size()>1) {
                    // Many fields: return empty
                    ret.clear();
                    break;
                } else if(!subq.isEmpty()) {
                    // There must be at most one field in the return map
                    IndexFieldValues fv=subq.values().iterator().next();
                    if(ret.isEmpty()) {
                        ret.put(fv.field,fv);
                    } else {
                        if(ret.containsKey(fv.field)) {
                            if(!ret.get(fv.field).union(fv))
                                ret.remove(fv.field);
                        } else {
                            // Multiple fields, return empty
                            ret.clear();
                            break;
                        }
                    }
                }
            }
        }   
        return ret;
    }
    
    @Override
    protected Map<QueryFieldInfo,IndexFieldValues> itrArrayMatchExpression(ArrayMatchExpression q, Path context) {
        Map<QueryFieldInfo,IndexFieldValues> ret=new HashMap<>();
        QueryFieldInfo finfo=findFieldInfo(q.getArray(),q);
        Map<QueryFieldInfo,IndexFieldValues> nested=iterate(q.getElemMatch(), new Path(new Path(context, q.getArray()), Path.ANYPATH));
        if(!nested.isEmpty()) {
            // If the nested contains a single field, no need for array treatment
            if(nested.size()==1) {
                ret.putAll(nested);
            } else {
                // nested has more than one field
                List<IndexFieldValues> listFv=new ArrayList<>();
                for(IndexFieldValues fv:nested.values()) {
                    if(fv.hasNestedArrays()) {
                        break;
                    } else {
                        if(fv.field.getEntityRelativeFieldNameWithContext().prefix(finfo.getEntityRelativeFieldNameWithContext().numSegments()).
                           equals(finfo.getEntityRelativeFieldNameWithContext()))
                            listFv.add(fv);
                    }
                }
                // listFv contains the fields referring to fields under the array
                if(listFv.isEmpty()) {
                    ret.clear();
                } else {
                    IndexFieldValues fv=new IndexFieldValues(finfo);
                    fv.values.addAll(listFv);
                    ret.put(finfo,fv);
                }
            }
        }
        return ret;
    }
}
