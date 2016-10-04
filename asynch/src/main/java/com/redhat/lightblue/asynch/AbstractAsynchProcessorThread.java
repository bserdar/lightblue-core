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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAsynchProcessorThread extends Thread {

    protected final Logger LOGGER=LoggerFactory.getLogger(getClass().getName());
    
    private Date startTime;
    private Date endTime;
    private Date lastPing;
    private boolean stopRequested=false;

    public AbstractAsynchProcessorThread() {}

    public AbstractAsynchProcessorThread(String name) {
        super(name);
    }
    
    public AbstractAsynchProcessorThread(ThreadGroup group,String name) {
        super(group,name);
    }

    public Date getLastPing() {
        return lastPing;
    }

    public void ping() {
        lastPing=new Date();
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public synchronized void requestStop(boolean interrupt) {
        stopRequested=true;
        notify();
        if(interrupt)
            interrupt();
    }

    @Override
    public final void run() {
        LOGGER.debug("Start {}",getName());
        startTime=new Date();
        try {
            process();
        } catch(Throwable t) {
            LOGGER.error("{} terminated with exception {}",getName(),t,t);
        }
        endTime=new Date();
        LOGGER.debug("End {}",getName());
    }

    /**
     * Returns the number of milliseconds elapsed between from and now
     */
    public static long durationMsecs(Date from) {
        return durationMsecs(from.getTime());
    }

    /**
     * Returns the number of milliseconds elapsed between d and now
     */
    public static long durationMsecs(long fromMsecs) {
        return System.currentTimeMillis()-fromMsecs;
    }

    /**
     * Returns a snapshot of threads in a thread group
     */
    public static Thread[] getThreads(ThreadGroup g) {
        Thread arr[];
        int next=g.activeCount()+10;        
        int n;
        int nRead;
        do {
            n=next;
            arr=new Thread[n];
            nRead=g.enumerate(arr);
            next=next*2;
        } while(nRead>=n);
        return arr;            
    }
    
    protected abstract void process();
}
