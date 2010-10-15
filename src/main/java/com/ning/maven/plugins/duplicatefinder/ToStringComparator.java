/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.maven.plugins.duplicatefinder;

import java.util.Comparator;

public class ToStringComparator implements Comparator
{
    public int compare(Object objA, Object objB)
    {
        if (objA == null) {
            return objB == null ? 0 : -1;
        }
        else if (objB == null) {
            return 1;
        }
        else {
            return objA.toString().compareTo(objB.toString());
        }
    }
}