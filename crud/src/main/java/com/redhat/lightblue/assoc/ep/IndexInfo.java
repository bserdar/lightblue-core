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

    public static class FieldIndex implements TermIndex {
        public final Path field;

        public FieldIndex(Path field) {
            this.field=field;
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

    public static class ArrayIndex implements TermIndex {
        public final Set<FieldIndex> indexes;
        public final Path array;

        public ArrayIndex(Path array,Set<FieldIndex> indexes) {
            this.array=array;
            this.indexes=indexes;
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
    IndexInfo(Path p) {add(new FieldIndex(p));}
    IndexInfo(Path p,IndexInfo ii) {addArray(p,ii);}

    public Set<TermIndex> getIndexes() {
        return indexes;
    }
        

    void add(Path p) {
        add(new FieldIndex(p));
    }
    
    void add(FieldIndex p) {
        indexes.add(p);
    }

    void addFields(IndexInfo ii) {
        for(TermIndex ti:ii.indexes)
            if(ti instanceof FieldIndex)
                indexes.add(ti);        
    }

    void addArray(Path p,IndexInfo ii) {
        HashSet<FieldIndex> findexes=new HashSet<>();
        for(TermIndex ti:ii.indexes)
            if(ti instanceof FieldIndex) {
                if(((FieldIndex)ti).field.prefix(p.numSegments()).equals(p))
                    findexes.add((FieldIndex)ti);
            }
        if(!findexes.isEmpty())
            indexes.add(new ArrayIndex(p,findexes));
    }

    @Override
    public String toString() {
        return indexes.toString();
    }

    public int size() {
        return indexes.size();
    }
}
