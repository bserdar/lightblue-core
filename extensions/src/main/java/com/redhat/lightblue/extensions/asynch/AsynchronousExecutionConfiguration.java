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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.util.JsonInitializable;

public class AsynchronousExecutionConfiguration implements JsonInitializable, Serializable {

    private static final long serialVersionUID = 1l;

    private String backend;
    private Class<? extends AsynchronousExecutionSupport> schedulerClass;
    private ObjectNode options;

    
    /**
     * @return the backend
     */
    public String getBackend() {
        return backend;
    }

    /**
     * @param backend the backend to set
     */
    public void setBackend(String backend) {
        this.backend = backend;
    }

    /**
     * @return the asynchronous execution implementation
     */
    public Class<? extends AsynchronousExecutionSupport> getSchedulerClass() {
        return schedulerClass;
    }

    /**
     * @return the asynchronous execution implementation
     */
    public void setSchedulerClass(Class<? extends AsynchronousExecutionSupport> clazz) {
        schedulerClass = clazz;
    }

    /**
     * The options
     */
    public ObjectNode getOptions() {
        return options;
    }

    /**
     * The  options 
     */
    public void setOptions(ObjectNode node) {
        options = node;
    }

    @Override
    public void initializeFromJson(JsonNode node) {
        try {
            if (node != null) {
                JsonNode x = node.get("backend");
                if (x != null) {
                    backend = x.asText();
                }
                x = node.get("schedulerClass");
                if (x != null) {
                    schedulerClass = (Class<AsynchronousExecutionSupport>) Thread.currentThread().getContextClassLoader().loadClass(x.asText());
                }
                if(backend!=null&&schedulerClass!=null)
                    throw new RuntimeException("Both backend and schedulerClass are given. Only one should be defined");
                options = (ObjectNode) node.get("options");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
