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

import static java.lang.String.format;

import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PluginLog
{
    private final Logger logger;

    public PluginLog(Class<?> clazz)
    {
        checkNotNull(clazz, "clazz is null");
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public void debug(String fmt, Object ... args)
    {
        checkNotNull(fmt, "fmt is null");
        logger.debug(format(fmt, args));
    }

    public void debug(Throwable t, String fmt, Object ... args)
    {
        checkNotNull(fmt, "fmt is null");
        checkNotNull(t, "t is null");
        logger.debug(format(fmt, args), t);
    }

    public void info(String fmt, Object ... args)
    {
        checkNotNull(fmt, "fmt is null");
        logger.debug(format(fmt, args));
    }

    public void info(Throwable t, String fmt, Object ... args)
    {
        checkNotNull(fmt, "fmt is null");
        checkNotNull(t, "t is null");
        logger.debug(format(fmt, args), t);
    }

    public void warn(String fmt, Object ... args)
    {
        checkNotNull(fmt, "fmt is null");
        logger.debug(format(fmt, args));
    }

    public void warn(Throwable t, String fmt, Object ... args)
    {
        checkNotNull(fmt, "fmt is null");
        checkNotNull(t, "t is null");
        logger.debug(format(fmt, args), t);
    }

    public void error(String fmt, Object ... args)
    {
        checkNotNull(fmt, "fmt is null");
        logger.debug(format(fmt, args));
    }

    public void error(Throwable t, String fmt, Object ... args)
    {
        checkNotNull(fmt, "fmt is null");
        checkNotNull(t, "t is null");
        logger.debug(format(fmt, args), t);
    }

    public void report(boolean quiet, String fmt, Object ... args)
    {
        if (quiet) {
            debug(fmt, args);
        }
        else {
            info(fmt, args);
        }
    }
}

