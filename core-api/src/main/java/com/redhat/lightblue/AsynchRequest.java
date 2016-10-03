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
package com.redhat.lightblue;

import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.redhat.lightblue.util.JsonObject;
import com.redhat.lightblue.util.Constants;

import com.redhat.lightblue.crud.*;

public class AsynchRequest extends JsonObject {

    private final Request request;
    private final BulkRequest bulkRequest;
    private Date executeAfter;
    private int priority;

    /**
     * Creates an asynchronous request based on the given request
     */
    public AsynchRequest(Request req) {
        this.request=req;
        this.bulkRequest=null;
    }

    /**
     * Creates an asynchronous request based on the given bulk request
     */
    public AsynchRequest(BulkRequest req) {
        this.request=null;
        this.bulkRequest=req;
    }

    /**
     * If not null, the time after which the job should execute
     */
    public Date getExecuteAfter() {
        return executeAfter;
    }

    /**
     * If not null, the time after which the job should execute
     */
    public void setExecuteAfter(Date d) {
        executeAfter=d;
    }

    /**
     * Priority of the job. 0 is the highest priority, and increasing
     * value means decreasing priority. Jobs with the lowest priority
     * value execute earlier than jobs with higher priority value.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Priority of the job. 0 is the highest priority, and increasing
     * value means decreasing priority. Jobs with the lowest priority
     * value execute earlier than jobs with higher priority value.
     */
    public void setPriority(int p) {
        priority=p;
    }

    /**
     * Returns the request. If this is a bulk request, returns null.
     */
    public Request getRequest() {
        return request;
    }

    /**
     * Returns the bulk request. If this is a single request, returns null
     */
    public BulkRequest getBulkRequest() {
        return bulkRequest;
    }

    /**
     * Returns if this is a bulk request
     */
    public boolean isBulk() {
        return bulkRequest!=null;
    }

    @Override
    public JsonNode toJson() {
        ObjectNode node=getFactory().objectNode();
        if(request!=null) {
            node.set("request",request.toJson());
            node.set("op",getFactory().textNode(request.getOperation().toString()));
        } else {
            node.set("bulkRequest",bulkRequest.toJson());
        }
        if(executeAfter!=null) {
            node.set("executeAfter",getFactory().textNode(Constants.getDateFormat().format(executeAfter)));
        }
        node.set("priority",getFactory().numberNode(priority));
        return node;
    }

    public static AsynchRequest fromJson(ObjectNode node) {
        AsynchRequest req;
        JsonNode x=node.get("request");
        if(x==null) {
            x=node.get("bulkRequest");
            if(x==null)
                throw new IllegalArgumentException();
            req=new AsynchRequest(BulkRequest.fromJson((ObjectNode)x));
        } else {
            JsonNode opNode = node.get("op");
            if (opNode != null) {
                Request r;
                String opstr = opNode.asText();
                if (x instanceof ObjectNode) {
                    if (opstr.equalsIgnoreCase(CRUDOperation.FIND.toString())) {
                        r = FindRequest.fromJson((ObjectNode) x);
                    } else if (opstr.equalsIgnoreCase(CRUDOperation.INSERT.toString())) {
                        r = InsertionRequest.fromJson((ObjectNode) x);
                    } else if (opstr.equalsIgnoreCase(CRUDOperation.SAVE.toString())) {
                        r = SaveRequest.fromJson((ObjectNode) x);
                    } else if (opstr.equalsIgnoreCase(CRUDOperation.UPDATE.toString())) {
                        r = UpdateRequest.fromJson((ObjectNode) x);
                    } else if (opstr.equalsIgnoreCase(CRUDOperation.DELETE.toString())) {
                        r = DeleteRequest.fromJson((ObjectNode) x);
                    } else {
                        throw new IllegalArgumentException(opstr);
                    }
                    req=new AsynchRequest(r);
                } else {
                    throw new IllegalArgumentException(opstr);
                }
            } else {
                throw new IllegalArgumentException("op");
            }
        }
        x=node.get("priority");
        if(x!=null)
            req.priority=x.asInt();
        x=node.get("executeAfter");
        if(x!=null)
            try {
                req.executeAfter=Constants.getDateFormat().parse(x.asText());
            } catch (Exception e) {
                throw new IllegalArgumentException(x.asText());
            }
        return req;
    }
}
