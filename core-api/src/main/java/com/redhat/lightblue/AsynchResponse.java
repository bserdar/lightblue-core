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
import java.util.List;
import java.util.ArrayList;
import java.text.DateFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import com.redhat.lightblue.util.JsonObject;
import com.redhat.lightblue.util.Constants;
import com.redhat.lightblue.util.Error;;

import com.redhat.lightblue.crud.BulkResponse;

public class AsynchResponse extends JsonObject {
    public enum AsynchStatus { scheduled, executing, completed, timedout };

    private String jobId;
    private Response response;
    private BulkResponse bulkResponse;
    private int priority;
    private Date scheduledTime;
    private Date executionStartTime;
    private Date completionTime;
    private Date timeoutTime;
    private AsynchStatus asynchStatus;
    private final List<Error> errors = new ArrayList<>();

    public void setJobId(String s) {
        jobId=s;
    }
    
    public String getJobId() {
        return jobId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int i) {
        priority=i;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response r) {
        this.response=r;
    }

    public BulkResponse getBulkResponse() {
        return bulkResponse;
    }

    public void setBulkResponse(BulkResponse r) {
        this.bulkResponse=r;
    }

    public Date getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Date d) {
        scheduledTime=d;
    }

    public Date getExecutionStartTime() {
        return executionStartTime;
    }

    public void setExecutionStartTime(Date d) {
        executionStartTime=d;
    }

    public Date getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(Date d) {
        completionTime=d;
    }

    public Date getTimeoutTime() {
        return timeoutTime;
    }

    public void setTimeoutTime(Date d) {
        timeoutTime=d;
    }
    
    public boolean isCompleted() {
        return completionTime!=null;
    }

    public boolean isRunning() {
        return completionTime==null&&executionStartTime!=null;
    }

    public AsynchStatus getAsynchStatus() {
        return asynchStatus;
    }

    public void setAsynchStatus(AsynchStatus s) {
        asynchStatus=s;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> l) {
        errors.clear();
        if(l!=null)
            errors.addAll(l);
    }

    @Override
    public JsonNode toJson() {
        JsonNodeFactory factory=getFactory();
        ObjectNode node=factory.objectNode();
        if(response!=null) {
            node.set("response",response.toJson());
        } else {
            node.set("bulkResponse",bulkResponse.toJson());
        }
        node.set("jobId",factory.textNode(jobId));
        node.set("priority",factory.numberNode(priority));
        DateFormat fmt=Constants.getDateFormat();
        if(scheduledTime!=null)
            node.set("scheduledTime",factory.textNode(fmt.format(scheduledTime)));
        if(executionStartTime!=null)
            node.set("executionStartTime",factory.textNode(fmt.format(executionStartTime)));
        if(completionTime!=null)
            node.set("ccompletionTime",factory.textNode(fmt.format(completionTime)));
        if(timeoutTime!=null)
            node.set("timeoutTime",factory.textNode(fmt.format(timeoutTime)));
        node.set("asynchStatus",factory.textNode(asynchStatus.toString()));
        if(errors!=null&&!errors.isEmpty()) {
            ArrayNode arr=factory.arrayNode();
            for(Error x: errors)
                arr.add(x.toJson());
            node.set("errors",arr);
        }
        node.set("host",factory.textNode(Response.HOSTNAME));
        node.set("status",factory.textNode(errors.isEmpty()?OperationStatus.ASYNC.toString():OperationStatus.ERROR.toString()));
        return node;
    }
}
