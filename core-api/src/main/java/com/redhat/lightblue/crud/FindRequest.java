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
package com.redhat.lightblue.crud;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.Request;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.query.Sort;

/**
 * Request to find documents
 */
public class FindRequest extends Request implements WithQuery, WithProjection, WithRange {

    private QueryExpression query;
    private Projection projection;
    private Sort sort;
    private Long from;
    private Long to;

    /**
     * The query
     */
    public QueryExpression getQuery() {
        return query;
    }

    /**
     * The query
     */
    public void setQuery(QueryExpression q) {
        query = q;
    }

    /**
     * Specifies what fields of the documents to return
     */
    public Projection getProjection() {
        return projection;
    }

    /**
     * Specifies what fields of the documents to return
     */
    public void setProjection(Projection x) {
        projection = x;
    }

    /**
     * Specifies the order in which the documents will be returned
     */
    public Sort getSort() {
        return sort;
    }

    /**
     * Specifies the order in which the documents will be returned
     */
    public void setSort(Sort s) {
        sort = s;
    }

    /**
     * Specifies the index in the result set to start returning documents.
     * Meaningful only if sort is given. Starts from 0.
     */
    @Override
    public Long getFrom() {
        return from;
    }

    /**
     * Specifies the index in the result set to start returning documents.
     * Meaningful only if sort is given. Starts from 0.
     */
    public void setFrom(Long l) {
        from = l;
    }

    /**
     * Specifies the last index of the document in the result set to be
     * returned. Meaningful only if sort is given. Starts from 0.
     */
    @Override
    public Long getTo() {
        return to;
    }

    /**
     * Specifies the last index of the document in the result set to be
     * returned. Meaningful only if sort is given. Starts from 0.
     */
    public void setTo(Long l) {
        to = l;
    }

    /**
     * Shallow copy from r to this
     */
    public void shallowCopyFrom(FindRequest r) {
        super.shallowCopyFrom(r);
        query = r.query;
        projection = r.projection;
        sort = r.sort;
        from = r.from;
        to = r.to;
    }

    /**
     * Populates an object node with the JSON representation of this
     */
    public JsonNode toJson() {
        ObjectNode node = (ObjectNode) super.toJson();
        if (query != null) {
            node.set("query", query.toJson());
        }
        if (projection != null) {
            node.set("projection", projection.toJson());
        }
        if (sort != null) {
            node.set("sort", sort.toJson());
        }
        WithRange.toJson(this, JsonNodeFactory.instance, node);
        return node;
    }

    /**
     * Parses a find request from a json object. Unrecognized elements are
     * ignored.
     */
    public static FindRequest fromJson(ObjectNode node) {
        FindRequest req=new FindRequest();
        req.parse(node);
        JsonNode x = node.get("query");
        if (x != null) {
            req.query = QueryExpression.fromJson(x);
        }
        x = node.get("projection");
        if (x != null) {
            req.projection = Projection.fromJson(x);
        }
        x = node.get("sort");
        if (x != null) {
            req.sort = Sort.fromJson(x);
        }
        Range r = WithRange.fromJson(node);
        req.setFrom(r.from);
        req.setTo(r.to);
        return req;
    }

    @Override
    public CRUDOperation getOperation() {
        return CRUDOperation.FIND;
    }
}
