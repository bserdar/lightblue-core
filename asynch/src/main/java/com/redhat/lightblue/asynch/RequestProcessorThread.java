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

import java.util.concurrent.atomic.AtomicInteger;

import com.redhat.lightblue.Response;

import com.redhat.lightblue.config.LightblueFactory;

import com.redhat.lightblue.extensions.asynch.AsynchronousJob;

public class RequestProcessorThread extends AbstractAsynchProcessorThread {

    private static AtomicInteger idCounter=new AtomicInteger(1);

    private final String processorId;
    private final AsynchConfiguration cfg;
    private final LightblueFactory lightblueFactory;
    private final RequestProcessor processor;

    public RequestProcessorThread(AsynchConfiguration cfg,
                                  LightblueFactory lbf,
                                  ThreadGroup group,
                                  AsynchronousJob job) {
        super(group,"");
        this.processorId=Response.HOSTNAME+"-"+idCounter.incrementAndGet();
        setName(processorId);
        this.cfg=cfg;
        this.lightblueFactory=lbf;
        processor=new RequestProcessor(lbf.getMediator(),job,processorId);
    }

    @Override
    protected void process() {
        processor.processRequest();
    }
}

