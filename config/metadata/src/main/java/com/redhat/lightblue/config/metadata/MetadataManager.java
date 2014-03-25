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
package com.redhat.lightblue.config.metadata;

import com.redhat.lightblue.util.JsonInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.gson.Gson;
import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.metadata.MetadataConstants;
import com.redhat.lightblue.metadata.parser.DataStoreParser;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.util.JsonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Because rest resources are instantiated for every request this manager exists
 * to keep the number of Metadata instances created down to a reasonable level.
 *
 * @author nmalik
 */
public final class MetadataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataManager.class);

    private static Metadata metadata = null;
    private static JSONMetadataParser parser = null;
    private static MetadataConfiguration configuration = null;
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.withExactBigDecimals(true);

    private MetadataManager() {

    }

    private static synchronized void initializeParser() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException, InstantiationException {
        if (parser != null) {
            return;
        }
        initializeMetadata();

        Extensions<JsonNode> extensions = new Extensions<>();
        extensions.addDefaultExtensions();
        if(configuration != null && configuration.getDataStoreParserNames() != null) {
            for (Map.Entry<String,String> entry : configuration.getDataStoreParserNames().entrySet()) {
                Class<DataStoreParser> databaseConfigurationClass = (Class<DataStoreParser>) Class.forName(entry.getValue());
                DataStoreParser i = databaseConfigurationClass.newInstance();
                final String defaultName = entry.getKey() == null? i.getDefaultName() : entry.getKey();
                extensions.registerDataStoreParser(defaultName, i);
            }
        }

        parser = new JSONMetadataParser(extensions, new DefaultTypes(), NODE_FACTORY);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static synchronized void initializeMetadata() throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (metadata != null) {
            // already initalized
            return;
        }
        LOGGER.debug("Initializing metadata");
        StringBuilder buff = new StringBuilder();

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(MetadataConfiguration.FILENAME);
                InputStreamReader isr = new InputStreamReader(is, Charset.defaultCharset());
                BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                buff.append(line).append("\n");
            }
        }

        // get the root json node so can throw subsets of the tree at Gson later
        JsonNode root = JsonUtils.json(buff.toString());
        LOGGER.debug("Config root:{}",root);

        // convert root to Configuration object
        // TODO swap out something other than Gson
        Gson g = new Gson();

        configuration = g.fromJson(buff.toString(), MetadataConfiguration.class);

        if (null == configuration) {
            throw new IllegalStateException(MetadataConstants.ERR_CONFIG_NOT_FOUND +" - "+ MetadataConfiguration.FILENAME);
        }
        LOGGER.debug("Configuration:{}",configuration);

        // instantiate the database specific configuration object
        Class databaseConfigurationClass = Class.forName(configuration.getDatabaseConfigurationClass());
        JsonNode dbNode = root.findValue("databaseConfiguration");
        JsonInitializable databaseConfiguration = (JsonInitializable) databaseConfigurationClass.newInstance();
        databaseConfiguration.initializeFromJson(dbNode);
        configuration.setDatabaseConfiguration(databaseConfiguration);
        LOGGER.debug("database configuration:{}",configuration.getDatabaseConfiguration());

        // validate
        if (!configuration.isValid()) {
            throw new IllegalStateException(MetadataConstants.ERR_CONFIG_NOT_VALID +" - "+ MetadataConfiguration.FILENAME);
        }

        Method m = databaseConfigurationClass.getDeclaredMethod(configuration.getMetadataFactoryMethod(), databaseConfigurationClass);

        metadata = (Metadata) m.invoke(null, configuration.getDatabaseConfiguration());
    }

    public static Metadata getMetadata() throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (metadata == null) {
            initializeMetadata();
        }

        return metadata;
    }

    public static JSONMetadataParser getJSONParser() throws ClassNotFoundException, NoSuchMethodException, IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (parser == null) {
            initializeParser();
        }

        return parser;
    }
}
