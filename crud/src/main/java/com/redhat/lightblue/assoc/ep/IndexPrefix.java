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
package com.redhat.lightblue.assoc.ep;

/**
 * A regex pattern over index, with a prefix
 */
class IndexPrefix implements IndexValues {
    private static final String REGEX_CHARS="$[]*.\\^-&|{}()?+:<>!=";
    public final String prefix;
    
    IndexPrefix(String p) {
        prefix=p;
    }
    
    static String getPrefix(String pattern) {
        StringBuilder bld=new StringBuilder();
        int n=pattern.length();
        for(int i=0;i<n;i++) {
            char c=pattern.charAt(i);
            if(i==0) {
                if(c=='^') {
                    // ok
                } else if(pattern.startsWith("\\A")) {
                    // ok
                    i++; // pass A
                } else {
                    break;
                }
            } else {
                if(REGEX_CHARS.indexOf(c)!=-1)
                    bld.append(c);
                else
                    break;
            }
        }
        return bld.toString();
    }
}
