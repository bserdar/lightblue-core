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

import com.redhat.lightblue.config.LightblueFactory;

public class DriverThread extends AbstractAsynchProcessorThread {

    private SchedulerThread schedulerThread=null;
    private ThreadGroup processors=new ThreadGroup("Processors");
    private final AsynchProcessorConfiguration cfg;
    private final LightblueFactory lightblueFactory;
    
    public DriverThread(AsynchProcessorConfiguration cfg,LightblueFactory lbf)
        throws Exception {
        super("DriverThread");
        this.cfg=cfg;
        this.lightblueFactory=lbf;
        if(lightblueFactory.getFactory().getAsynchronousExecutionSupport()==null)
            throw new RuntimeException("No asynchronous execution support");
    }

    @Override
    protected void process() {
        LOGGER.info("Driver thread starting");
        while(!isStopRequested()) {
            try {
                ping();
                
                // There must be one scheduler thread
                if(schedulerThread!=null) {
                    if(schedulerThread.isAlive()) {
                        // Scheduler thread will set its status to working
                        // before it fetches a new job                    
                        if(schedulerThread.getStatus()==SchedulerThread.Status.working) {
                            // Scheduler thread is working. Has it been working too long?
                            long workingTime=durationMsecs(schedulerThread.getWorkStartTime());
                            if(workingTime>cfg.getSchedulerTimeoutMsecs()) {
                                // Scheduler thread needs to die
                                LOGGER.warn("{}: Scheduler thread timed out",getName());
                                schedulerThread.requestStop(true);
                                schedulerThread=null;
                            }
                        }
                    } else {
                        schedulerThread=null;
                    }
                }
                if(schedulerThread==null) {
                    LOGGER.debug("{}: Starting scheduler thread",getName());
                    schedulerThread=new SchedulerThread(cfg,lightblueFactory,processors);
                    schedulerThread.start();
                }
                
                ping();

                // Check the processor threads, kill any that needs to die
                Thread[] processorThreads=getThreads(processors);
                for(Thread t:processorThreads) {
                    if(t!=null) {
                        RequestProcessorThread pt=(RequestProcessorThread)t;
                        if(pt.isAlive()) {
                            long workingTime=durationMsecs(pt.getLastPing());
                            if(workingTime>pt.getProcessorTimeoutMsecs()) {
                                LOGGER.warn("{}: Processor thread {} timed out, working for {} msecs",
                                            getName(),
                                            pt.getName(),
                                            workingTime);
                                pt.requestStop(true);
                            } 
                        }
                    }
                }
                ping();
                
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {};
                
            } catch(Exception e) {
                LOGGER.error("Exception in driver thread",e);
            }
        }
    }
}

