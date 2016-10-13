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

import com.redhat.lightblue.AsynchRequest;
import com.redhat.lightblue.AsynchResponse;;

import com.redhat.lightblue.extensions.Extension;

/**
 * This class provides support for scheduling asynchronous execution of requests.
 */
public interface AsynchronousExecutionSupport extends Extension {

    public static final String ASYNCH_JOBID_PROPERTY="asynch:jobid";

    /**
     * Schedule execution of a request
     *
     * @param request The request
     *
     * @return The asynchronous response, which contains the job id
     */
    AsynchResponse scheduleAsynchronousExecution(AsynchRequest request);

    /**
     * Retrieve the status, or the results of an asynchronous execution
     *
     * @param jobId The job identifier, returned by scheduleAsynchronousExecution
     *
     * @return The response. If the execution is complete, the
     * response will include the results of the execution. Once
     * execution is complete, the backend may choose to remove the job
     * from job storage, so calling this api again might fail.
     */
    AsynchResponse getAsynchronousExecutionStatus(String jobId);

    /**
     * Retrieves and locks the next available request from the job
     * queue. This call should change the status of the job to
     * 'executing' and set the necessary timestamps
     *
     * Returns the next available request, or null if no requests are
     * in the job queue.
     */
    AsynchronousJob getAndLockNextAsynchronousJob();

    /**
     * Update an asymchronous job
     */
    void updateAsynchronousJob(AsynchronousJob job);
}
