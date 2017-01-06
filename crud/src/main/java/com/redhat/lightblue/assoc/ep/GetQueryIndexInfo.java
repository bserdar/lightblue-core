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

import com.redhat.lightblue.assoc.QueryFieldInfo;
import com.redhat.lightblue.assoc.AssocConstants;

import com.redhat.lightblue.query.*;

import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.Error;

/**
 * Builds a set of paths from which to build a composite index for
 * efficient retrieval of the query
 *
 * This class is tightly coupled with how DocIndex and GetIndedRanges works
 */
class GetQueryIndexInfo extends IndexQueryProcessorBase<IndexInfo> {

    public GetQueryIndexInfo(List<QueryFieldInfo> fields) {
        super(fields);
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
        IndexInfo info=new IndexInfo();
        if(q.getOp()==BinaryComparisonOperator._neq) {
            return info;
        } else {
            QueryFieldInfo finfo=findFieldInfo(q.getField(),q);
            info.add(q,finfo.getEntityRelativeFieldNameWithContext());
        }
        return info;
    }
    
    @Override
    protected IndexInfo itrAllMatchExpression(AllMatchExpression q, Path context) {
        return new IndexInfo();
    }
    
    @Override
    protected IndexInfo itrRegexMatchExpression(RegexMatchExpression q, Path context) {
        String pattern=q.getRegex();
        if(pattern.length()>0 && pattern.charAt(0)=='^') {
            QueryFieldInfo finfo=findFieldInfo(q.getField(),q);
            return new IndexInfo(q,finfo.getEntityRelativeFieldNameWithContext());
        } else {
            return new IndexInfo();
        }
    }

    @Override
    protected IndexInfo itrNaryValueRelationalExpression(NaryValueRelationalExpression q, Path context) {
        if(q.getOp()==NaryRelationalOperator._in) {
            QueryFieldInfo finfo=findFieldInfo(q.getField(),q);
            return new IndexInfo(q,finfo.getEntityRelativeFieldNameWithContext());
        } else
            return new IndexInfo();
    }
    
    @Override
    protected IndexInfo itrArrayContainsExpression(ArrayContainsExpression q, Path context) {
        if(q.getOp()==ContainsOperator._any) {
            QueryFieldInfo finfo=findFieldInfo(q.getArray(),q);
            return new IndexInfo(q,finfo.getEntityRelativeFieldNameWithContext());
        } else
            return new IndexInfo();
    }
    
    @Override
    protected IndexInfo itrUnaryLogicalExpression(UnaryLogicalExpression q, Path context) {
        // Negation makes index useless
        return new IndexInfo();
    }
    
    @Override
    protected IndexInfo itrNaryLogicalExpression(NaryLogicalExpression q, Path context) {
        IndexInfo ret=new IndexInfo();
        if(q.getOp()==NaryLogicalOperator._or) {
            // Keep it simple: X or Y means we need to scan two indexes. Lets not do that
            return new IndexInfo();
        } else {
            // X and Y means we need an index scanning both
            // but there can be sub-expressions with ORs in them, and they will return empty IndexInfo
            // So, we collect only nonempty indexinfos, and create an index from them
            for(QueryExpression query:q.getQueries()) {
                ret.addFields(super.iterate(query,context));
            }
        }
        return ret;
    }
    
    @Override
    protected IndexInfo itrArrayMatchExpression(ArrayMatchExpression q, Path context) {
        QueryFieldInfo finfo=findFieldInfo(q.getArray(),q);
        return new IndexInfo(q,finfo.getEntityRelativeFieldNameWithContext(),
                             iterate(q.getElemMatch(), new Path(new Path(context, q.getArray()), Path.ANYPATH)));
    }
}
