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

import java.util.Date;

import com.redhat.lightblue.config.LightblueFactory;

import com.redhat.lightblue.extensions.asynch.AsynchronousExecutionSupport;
import com.redhat.lightblue.extensions.asynch.AsynchronousJob;

public class SchedulerThread extends AbstractAsynchProcessorThread {

    private final AsynchConfiguration cfg;
    private final ThreadGroup group;
    private final LightblueFactory lightblueFactory;

    public enum Status {working,waiting};

    private Status status=Status.waiting;
    private Date workStartTime;
    
    public SchedulerThread(AsynchConfiguration cfg,LightblueFactory lbf,ThreadGroup group) {
        super("SchedulerThread");
        this.cfg=cfg;
        this.group=group;
        this.lightblueFactory=lbf;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized Date getWorkStartTime() {
        return workStartTime;
    }

    private int getNumActiveThreads() {
        int nActive=0;
        Thread[] processorThreads=getThreads(group);
        for(Thread t:processorThreads) {
            if(t!=null&&t.isAlive()&&!((AbstractAsynchProcessorThread)t).isStopRequested()) {
                nActive++;
            }
        }
        return nActive;
    }

    @Override
    protected void process() {
        AsynchronousExecutionSupport async;
        try {
            async=lightblueFactory.getFactory().getAsynchronousExecutionSupport();
            if(async==null)
                throw new RuntimeException("No asynchronous execution support");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        while(!isStopRequested()) {
            synchronized(this) {
                status=Status.working;
                workStartTime=new Date();
            }
            while(getNumActiveThreads()<cfg.getMaxProcessorThreads()) {            
                try {
                    AsynchronousJob job=async.getAndLockNextAsynchronousJob();
                    if(job!=null) {
                        LOGGER.debug("{}: Scheduling processing of {}",getName(),job.jobId);
                        RequestProcessorThread rpt=new RequestProcessorThread(cfg,lightblueFactory,group,job);
                        rpt.start();
                    } else {
                        LOGGER.debug("{}: No more jobs, idling",getName());
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.error("{}: {}",getName(),e,e);
                }
            }
            synchronized(this) {
                status=Status.waiting;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }        
    }
}

