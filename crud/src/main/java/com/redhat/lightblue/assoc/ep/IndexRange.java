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

/**
 * A range of values, or a single value on the index. Both limits are inclusive
 */
class IndexRange implements IndexValues {
    public final Object from,to;
    public final Type t;
    
    IndexRange(Type t,Object value) {
        this.from=this.to=value;
        this.type=t;
    }
    
    IndexRange(Type t,Object from,Object to) {
        this.from=from;
        this.to=to;
        this.type=t;
    }

    boolean singleValue() {
        return (from!=null&&to!=null&&from.equals(to))||(from==null&&to==null);
    }

    boolean contains(Object value) {
        boolean gtfrom=false;
        boolean ltto=false;
        if(value!=null) {
            if(from!=null) {
                gtfrom=type.compare(from,value)<0;
            } else {
                gtfrom=true;
            }
            if(to!=null) {
                ltto=type.compare(value,to)<0;
            } else {
                ltto=true;
            }
            return gtfrom&&ltto;
        } else {
            return to!=null;
        }
    }

    /**
     * Computes the intersection of the two intervals. Returns true if intersection is nonempty.
     */
    boolean intersect(IndexRange operand,Set<IndexValues> newResults) {
        if(singleValue()) {
            if(operand.singleValue()) {
                if(type.compare(from,operand.from)==0) {
                    newResults.add(this);
                    return true;
                } else {
                    return false;
                }
            } else {
                if(operand.contains(from)) {
                    newResults.add(operand);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            if(operand.singleValue()) {
                if(contains(operand.from)) {
                    newResults.append(this);
                    return true;
                } else {
                    return false;
                }
            } else
                {
                // Both are ranges
            }
        }
    }
}
