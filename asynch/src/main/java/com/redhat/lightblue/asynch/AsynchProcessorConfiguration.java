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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.redhat.lightblue.util.JsonInitializable;

import com.redhat.lightblue.extensions.asynch.AsynchronousExecutionConfiguration;

public class AsynchProcessorConfiguration implements JsonInitializable {

    private long schedulerTimeoutMsecs=10l*60l*1000l;
    private long processorTimeoutMsecs=15l*60l*10000l;
    private int maxProcessorThreads=20;
    private boolean process=true;
    
    public long getSchedulerTimeoutMsecs() {
        return schedulerTimeoutMsecs;
    }
    
    public void setSchedulerTimeoutMsecs(long i) {
        schedulerTimeoutMsecs=i;
    }

    public long getProcessorTimeoutMsecs() {
        return processorTimeoutMsecs;
    }
    
    public void setProcessorTimeoutMsecs(long i) {
        processorTimeoutMsecs=i;
    }

    public int getMaxProcessorThreads() {
        return maxProcessorThreads;
    }

    public void setMaxProcessorThreads(int i) {
        maxProcessorThreads=i;
    }

    public boolean isProcess() {
        return process;
    }

    public void setProcess(boolean b) {
        process=b;
    }
    
    @Override
    public void initializeFromJson(JsonNode node) {
        try {
            JsonNode x=node.get("schedulerTimeoutMsecs");
            if(x!=null) {
                schedulerTimeoutMsecs=x.asLong();
            } 
            x=node.get("processorTimeoutMsecs");
            if(x!=null) {
                processorTimeoutMsecs=x.asLong();
            }
            x=node.get("maxProcessorThreads");
            if(x!=null) {
                maxProcessorThreads=x.asInt();
            }
            x=node.get("process");
            if(x!=null) {
                process=x.asBoolean();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an instance of the configuration from the "processor" property of asynch options
     */
    public static AsynchProcessorConfiguration initializeFromCfg(AsynchronousExecutionConfiguration cfg) {
        if(cfg!=null) {
            if(cfg.getProcessor()!=null) {
                AsynchProcessorConfiguration config=new AsynchProcessorConfiguration();
                config.initializeFromJson(cfg.getProcessor());
                return config;
            }
        }
        return null;
    }
}
