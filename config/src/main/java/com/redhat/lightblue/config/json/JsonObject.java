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

import java.util.ArrayList;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.redhat.lightblue.config.ConfigPropertyMap;

/**
 * A configuration property map based on a Json object node
 */
public class JsonObject implements ConfigPropertyMap {

    private final ObjectNode node;

    public JsonObject(JsonNode node) {
        this.node=(ObjectNode)node;
    }

    @Override
    public String[] getPropertyNames() {
        ArrayList<String> list=new ArrayList<>(node.size());
        for(Iterator<String> itr=node.fieldNames();itr.hasNext();)
            list.add(itr.next());
        return list.toArray(new String[list.size()]);
    }

    @Override
    public ConfigItem get(String name) {
        return JsonValue.getItem(node.get(name));
    }
}
