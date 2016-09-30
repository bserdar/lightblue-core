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
package com.redhat.lightblue.extensions.asynch;

import java.io.Serializable;

import java.util.Date;

import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.BulkResponse;

public class AsynchResponse implements Serializable {
    private static final long serialVersionUID=1l;

    private final String jobId;
    private Response response;
    private BulkResponse bulkResponse;
    private int priority;
    private Date scheduledTime;
    private Date executionStartTime;
    private Date completionTime;
    private Date timeoutTime;

    public AsynchResponse(String jobId) {
        this.jobId=jobId;
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

}
