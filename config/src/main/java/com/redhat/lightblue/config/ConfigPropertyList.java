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

/**
 * A random access property list. Each property is accessed using the
 * property index. It contains a list of ConfigItems, each item can be
 * a ConfigValue, ConfigPropertyList, or ConfigPropertyMap.
 */
public interface ConfigPropertyList extends ConfigItem {

    /**
     * Returns the number of properties in the property list
     */
    int getSize();

    /**
     * Returns the n'th property in the list, n starting from 0.
     */
    ConfigItem get(int n);
}
