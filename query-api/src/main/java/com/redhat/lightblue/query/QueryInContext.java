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
package com.redhat.lightblue.query;

import com.redhat.lightblue.util.Path;

import java.io.Serializable;

/**
 * Contains a query expression that needs to be interpreted with respect to the
 * given context. This class is used to return query clauses that contain
 * bindable fields.
 */
public class QueryInContext implements Serializable {
    private static final long serialVersionUID = 1l;

    private final Path context;
    private final QueryExpression query;
    private final QueryExpression nestedExpressions[];

    /**
     * Ctor
     */
    public QueryInContext(Path context, QueryExpression query,QueryExpression[] nested) {
        this.context = context;
        this.query = query;
        this.nestedExpressions=nested;
    }

    /**
     * Returns the path under which the query needs to be interpreted
     */
    public Path getContext() {
        return context;
    }

    /**
     * If the query is in an array expression, this contains the array
     * of nested array expressions collected from the root of the
     * query all the way to the query clause. If the query is not in an array
     * expression, this will be an empty array.
     */
    public QueryExpression[] getNestedExpressions() {
        return nestedExpressions;
    }

    /**
     * Returns the query that needs to be interpreted under the context path
     */
    public QueryExpression getQuery() {
        return query;
    }

    public String toString() {
        if (context.numSegments() > 0) {
            return query.toString() + "@" + context.toString();
        } else {
            return query.toString();
        }
    }
}
