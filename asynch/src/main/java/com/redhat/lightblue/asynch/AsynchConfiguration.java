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

public class AsynchConfiguration {

    private long schedulerTimeoutMsecs;
    private long processorTimeoutMsecs;
    private int maxProcessorThreads;
    
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
}
