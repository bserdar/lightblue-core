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

import java.util.Date;

import com.redhat.lightblue.Request;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.AsynchStatus;
import com.redhat.lightblue.crud.BulkRequest;
import com.redhat.lightblue.crud.BulkResponse;


/**
 * This is the lowest common denominator for the internal
 * representation of an asynchronous job
 */
public class AsynchronousJob {

    public static class SingleRequestData {
        public Request request;
        public Response response;
    }

    public static class BulkRequestData {
        public BulkRequest request;
        public BulkResponse response;
    }

    public String jobId;
    public SingleRequestData singleData;
    public BulkRequestData bulkData;
    public int priority;
    public Date scheduledTime;
    public Date executionStartTime;
    public Date completionTime;
    public Date timeoutTime;
    public AsynchStatus asynchStatus;

    public boolean isBulkRequest() {
        return bulkData!=null;
    }

    @Override
    public String toString() {
        return "JobId:"+jobId;
    }
}
