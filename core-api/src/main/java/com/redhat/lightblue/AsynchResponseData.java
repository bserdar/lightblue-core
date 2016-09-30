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
import java.text.DateFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.redhat.lightblue.util.JsonObject;
import com.redhat.lightblue.util.Constants;

public class AsynchResponseData extends JsonObject {

    public enum Status { scheduled, executing, completed, timedout };
    
    private String jobId;
    private int priority;
    private Date scheduledTime;
    private Date executionStartTime;
    private Date completionTime;
    private Date timeoutTime;
    private Status status;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String s) {
        jobId=s;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int i) {
        priority=i;
    }

    public Date getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Date t) {
        scheduledTime=t;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status s) {
        status=s;
    }

    @Override
    public JsonNode toJson() {
        DateFormat fmt=Constants.getDateFormat();
        ObjectNode node=JsonNodeFactory.instance.objectNode();
        if(jobId!=null)
            node.set("jobId",JsonNodeFactory.instance.textNode(jobId));
        node.set("priority",JsonNodeFactory.instance.numberNode(priority));
        if(scheduledTime!=null)
            node.set("scheduledTime",JsonNodeFactory.instance.textNode(fmt.format(scheduledTime)));
        if(executionStartTime!=null)
            node.set("executionStartTime",JsonNodeFactory.instance.textNode(fmt.format(executionStartTime)));
        if(completionTime!=null)
            node.set("completionTime",JsonNodeFactory.instance.textNode(fmt.format(completionTime)));
        if(timeoutTime!=null)
            node.set("timeoutTime",JsonNodeFactory.instance.textNode(fmt.format(timeoutTime)));
        if(status!=null)
            node.set("status",JsonNodeFactory.instance.textNode(status.toString()));
        return node;
    }
}
