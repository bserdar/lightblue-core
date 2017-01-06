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

import java.util.Set;
import java.util.HashSet;

import com.redhat.lightblue.assoc.QueryFieldInfo;

/**
 * The values requested for a field. If it is an array field, the
 * values set contains other IndexFieldValues objects for the sub-elements.
 */
class IndexFieldValues implements IndexValues {
    final QueryFieldInfo field;
    Set<IndexValues> values=new HashSet<>();
    
    IndexFieldValues(QueryFieldInfo field) {
        this.field=field;
    }
    
    IndexFieldValues(QueryFieldInfo field,IndexValues v) {
        this(field);
        values.add(v);
    }
    
    boolean hasNestedArrays() {
        for(IndexValues v:values)
            if(v instanceof IndexFieldValues)
                return true;
        return false;
    }

    /**
     * Computes the intersection of the ranges of this, and the operand
     * If the intersection is empty, returns false
     */
    boolean intersect(IndexFieldValues operand) {
        Set<IndexValues> newResult=new HashSet<>();
        for(IndexValues thisv:values) {
            for(IndexValues operandv:operand.values) {
                if(thisv.getClass()==operandv.getClass()) {
                    if(thisv instanceof IndexRange) {
                        if(!((IndexRange)thisv).intersect((IndexRange)operandv,newResult)) {
                            values.clear();
                            return false;
                        }
                    } else if(thisv instanceof IndexPrefix) {
                        // Intersction of two prefixes is the common prefix of the two
                        String c=commonPrefix( ((IndexPrefix)thisv).prefix, ((IndexPrefix)operandv).prefix);
                        if(c.length()>0) {
                            newResult.add(new IndexPrefix(c));
                        } else {
                            values.clear();
                            return false;
                        }
                    } else if(thisv instanceof IndexFieldValues) {
                        values.clear();
                        return false;
                    }
                } else {
                    values.clear();
                    return false;
                }
            }
        }
        values=newResult;
        return !values.isEmpty();
    }

    /**
     * Computes the union of the ranges of this, and the operand
     * If the union is empty, returns false
     */
    boolean union(IndexFieldValues operand) {
        
    }

    private static String commonPrefix(String s1,String s2) {
        int n=Math.min(s1.length(),s2.length());
        int commonPrefixIndex=-1;
        for(int i=0;i<n;i++) {
            if(s1.charAt(i)==s2.charAt(i)) {
                commonPrefixIndex=i;
            } else {
                break;
            }
        }
        if(commonPrefixIndex>=0)
            return s1.substring(0,commonPrefixIndex+1);
        else
            return "";
    }
}
