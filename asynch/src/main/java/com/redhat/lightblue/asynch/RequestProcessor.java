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
package com.redhat.lightblue.asynch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.lightblue.Request;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.OperationStatus;

import com.redhat.lightblue.crud.InsertionRequest;
import com.redhat.lightblue.crud.SaveRequest;
import com.redhat.lightblue.crud.UpdateRequest;
import com.redhat.lightblue.crud.DeleteRequest;
import com.redhat.lightblue.crud.FindRequest;
import com.redhat.lightblue.crud.BulkRequest;
import com.redhat.lightblue.crud.BulkResponse;
import com.redhat.lightblue.crud.CRUDOperation;
import com.redhat.lightblue.crud.CrudConstants;

import com.redhat.lightblue.util.Error;

import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.mediator.OperationContext;

import com.redhat.lightblue.extensions.asynch.AsynchronousJob;
import com.redhat.lightblue.extensions.asynch.AsynchronousExecutionSupport;

/**
 * This class contains the logic that processes an asynchronous request
 */
public class RequestProcessor {

    private static final Logger LOGGER=LoggerFactory.getLogger(RequestProcessor.class);
    
    private final AsynchronousJob job;
    private final String processorId;
    private final Mediator mediator;

    /**
     * This mediator implementation overrides generation of operation context to add 
     * asynchronous execution flags to it
     */
    private static class AsyncMediator extends Mediator {
        public AsyncMediator(Mediator source) {
            super(source);
        }

        @Override
        protected OperationContext newCtx(Request request, CRUDOperation CRUDOperation) {
            OperationContext ctx=super.newCtx(request,CRUDOperation);
            ctx.setAsynch(true);
            ctx.setProperty(AsynchronousExecutionSupport.ASYNCH_JONID_PROPERTY,jobId);
            return ctx;
        }        
    }
    
    public RequestProcessor(Mediator mediator,AsynchronousJob job,String processorId) {
        this.job=job;
        this.mediator=new AsyncMediator(mediator,job.jobId);
        this.processorId=processorId;
    }

    public void processRequest() {
        LOGGER.debug("{}: Start processing {}",processorId,job);
        if(!job.isBulkRequest()) {
            job.singleData.response=processSingleRequest(job.singleData.request);
        } else {
            job.bulkData.response=processBulkRequest(job.bulkData.request);
        }
        LOGGER.debug("{}: End processing {}",processorId,job.jobId);
    }
    
    protected Response processSingleRequest(Request request) {
        Response response;
        if(request instanceof InsertionRequest) {
            response=mediator.insert( (InsertionRequest)request);
        } else if(request instanceof SaveRequest) {
            response=mediator.save( (SaveRequest)request);
        } else if(request instanceof UpdateRequest) {
            response=mediator.update( (UpdateRequest)request);
        } else if(request instanceof DeleteRequest) {
            response=mediator.delete( (DeleteRequest)request);
        } else if(request instanceof FindRequest) {
            response=mediator.find( (FindRequest) request);
        } else {
            response=new Response();
            response.setStatus(OperationStatus.ERROR);
            response.getErrors().add(Error.get(CrudConstants.ERR_ASYNCH));
        }
        return response;
    }
    
    protected BulkResponse processBulkRequest(BulkRequest request) {
        return mediator.bulkRequest(request);
    }
}
