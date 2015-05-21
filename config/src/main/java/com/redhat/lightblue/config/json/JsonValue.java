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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.redhat.lightblue.config.ConfigPropertyList;

/**
 * A configuration value based on a Json document value
 */
public class JsonValue implements ConfigValue {

    private final JsonNode value;

    public JsonValue(JsonNode node) {
        this.value=node;
    }

    @Override
    public String asString() {
        return value.asText();
    }

    @Override
    public int asInt() {
        return value.asInt();
    }

    @Override
    public boolean asBoolean() {
        return value.asBoolean();
    }

    /**
     * Returns the appropriate configuration object for the node
     */
    public static ConfigItem getItem(JsonNode node) {
        if(node==null||node instanceof NullNode)
            return null;
        else if(node instanceof ObjectNode)
            return new JsonObject(node);
        else if(node instanceof ArrayNode)
            return new JsonArray(node);
        else
            return new JsonValue(node);
    }
}
