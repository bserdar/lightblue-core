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

import java.util.HashSet;
import java.util.Set;

import com.redhat.lightblue.query.QueryExpression;

import com.redhat.lightblue.util.Path;

/**
 * Contains information about the in-memory index of documents
 *
 * Logical combination of terms would create a set of individual field indexes.
 *
 * An array elemMatch query combines the individual indexes into an array index.
 *
 * Nested array indexes are not included in the computation
 */
public class IndexInfo {

    public interface TermIndex {}

    /**
     * Each fieldIndex entry denotes a field to be indexed
     */
    public static class FieldIndex implements TermIndex {
        public final Path field;
        public final QueryExpression clause;

        public FieldIndex(QueryExpression clause,Path field) {
            this.field=field;
            this.clause=clause;
        }
        
        @Override
        public boolean equals(Object o) {
            if(o instanceof FieldIndex)
                return ((FieldIndex)o).field.equals(field);
            else
                return false;
        }

        @Override
        public int hashCode() {
            return field.hashCode();
        }

        @Override
        public String toString() {
            return field.toString();
        }
    }

    /**
     * The arrayIndex entry gives a set of fieldIndex entries that will be indexed as a tuple
     */
    public static class ArrayIndex implements TermIndex {
        public final Set<FieldIndex> indexes;
        public final Path array;
        public final QueryExpression clause;

        public ArrayIndex(QueryExpression clause,Path array,Set<FieldIndex> indexes) {
            this.array=array;
            this.indexes=indexes;
            this.clause=clause;
        }

        @Override
        public boolean equals(Object o) {
            if(o instanceof ArrayIndex) 
                return ((ArrayIndex)o).indexes.equals(indexes);
            else
                return false;
        }

        @Override
        public int hashCode() {
            return indexes.hashCode();
        }

        @Override
        public String toString() {
            return array.toString()+":"+indexes.toString();
        }
    }
    
    private final HashSet<TermIndex> indexes=new HashSet<>();
    
    IndexInfo() {}
    IndexInfo(QueryExpression clause,Path p) {add(clause,p);}
    IndexInfo(QueryExpression clause, Path p,IndexInfo ii) {addArray(clause,p,ii);}

    public Set<TermIndex> getIndexes() {
        return indexes;
    }
        

    /**
     * Adds a field index for the field p in the clause
     */
    void add(QueryExpression clause,Path p) {
        add(new FieldIndex(clause,p));
    }
    
    /**
     * Adds a field index 
     */
    void add(FieldIndex p) {
        indexes.add(p);
    }

    void addFields(IndexInfo ii) {
        for(TermIndex ti:ii.indexes)
            if(ti instanceof FieldIndex)
                indexes.add(ti);        
    }

    /**
     * Adds an array index for the array p in clause. The index info
     * ii contains the index info for the nested clauses
     */
    void addArray(QueryExpression clause,Path p,IndexInfo ii) {
        // If the array has only one field, then we only need to get that field without the enclosing array
        if(ii.indexes.size()==1) {
            TermIndex ti=ii.indexes.iterator().next();
            if(ti instanceof FieldIndex) { // No support for nested array indexes
                if(((FieldIndex)ti).field.prefix(p.numSegments()).equals(p)) {
                    // Field is really under this array
                    add(clause,((FieldIndex)ti).field);
                }
            }
        } else if(ii.indexes.size()>1) {
            // The array has more than one fields
            HashSet<FieldIndex> findexes=new HashSet<>();
            for(TermIndex ti:ii.indexes)
                if(ti instanceof FieldIndex) {
                    if(((FieldIndex)ti).field.prefix(p.numSegments()).equals(p))
                        findexes.add((FieldIndex)ti);
                }
            if(!findexes.isEmpty())
                indexes.add(new ArrayIndex(clause,p,findexes));
        }
    }
    
    @Override
    public String toString() {
        return indexes.toString();
    }

    public int size() {
        return indexes.size();
    }
}
