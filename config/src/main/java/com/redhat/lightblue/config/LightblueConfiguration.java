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
package com.redhat.lightblue.config;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration is of the following form:
 *
 * <pre>
 * datasources (Object)
 *    datasourceName (Object) : Name of the datasource
 *      backend (String): Name of the backend that manages this datasource
 * metadata (Object)
 *    backend (String): Name of the backend used to store metadata
 * backends (Object)
 *   backendName (Object) : Name of the backend
 *     type (String): Class name for the backend descriptor (extends BackendDescriptor)
 *
 * SectionName (Object)
 *    section specific configuration
 * </pre>
 *
 * The "SectionName" could be any section name recognized by the
 * higher layers. This allows higher layers to add their custom
 * configurations.
 */
public class LightblueConfiguration implements Serializable {

    public static final String STR_BACKENDS="backends";
    public static final String STR_METADATA="metadata";
    public static final String STR_DATASOURCES="datasources";

    private static final long serialVersionUID = 1l;

    private static final Logger LOGGER = LoggerFactory.getLogger(LightblueConfiguration.class);

    private final ConfigPropertyMap cfg;

    /**
     * Constructs a configuration using the given configuration
     * property map. The configuration property map could be file
     * based, or any other implementation of the ConfigItem tree.
     */
    public LightblueConfiguration(ConfigPropertyMap cfg) {
        this.cfg=cfg;
    }

    /**
     * Returns the list of backend names in the configuration. Returns null if there is none.
     */
    public String[] getBackendNames() {
        ConfigPropertyMap backend=getConfigurationSection(STR_BACKENDS);
        if(backend!=null) {
            return backend.getPropertyNames();
        } else {
            return null;
        }
    }

    /**
     * Returns the backend configuration with the given name. Returns
     * null if no such backend configuration exists.
     */
    public ConfigPropertyMap getBackendConfiguration(String name) {
        ConfigPropertyMap backend=getConfigurationSection(STR_BACKENDS);
        if(backend!=null) {
            return (ConfigPropertyMap)backend.get(name);
        } else {
            return null;
        }
    }

    /**
     * Returns the metadata configuration. Returns null if no metadata
     * configuration exists.
     */
    public ConfigPropertyMap getMetadataConfiguration() {
        return (ConfigPropertyMap)getConfigurationSection(STR_METADATA);
    }

    /**
     * Returns the names of the datasources in the
     * configuration. Returns null if no datasrouces exist.
     */
    public String[] getDatasourceNames() {
        ConfigPropertyMap ds=getConfigurationSection(STR_DATASOURCES);
        if(ds!=null) {
            return ds.getPropertyNames();
        } else {
            return null;
        }
    }

    /**
     * Returns the datasource configuration with the given
     * name. Returns null if no such datasource exists.
     */
    public ConfigPropertyMap getDatasourceConfiguration(String name) {
        ConfigPropertyMap ds=getConfigurationSection(STR_DATASOURCES);
        if(ds!=null) {
            return (ConfigPropertyMap)ds.get(name);
        } else {
            return null;
        }
    }
    
    public ConfigPropertyMap getConfigurationSection(String sectionName) {
        return (ConfigPropertyMap)cfg.get(sectionName);
    }
    
    public String[] getConfigurationSectionNames() {
        return cfg.getPropertyNames();
    }

}
