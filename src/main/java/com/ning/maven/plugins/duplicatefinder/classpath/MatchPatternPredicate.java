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
package com.ning.maven.plugins.duplicatefinder.classpath;

import static java.lang.String.format;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MatchPatternPredicate implements Predicate<String>
{
    private static final Logger LOG = LoggerFactory.getLogger(MatchPatternPredicate.class);

    private final List<Pattern>patterns;

    MatchPatternPredicate(Collection<String> patternStrings)
    {
        checkNotNull(patternStrings, "patternStrings is null");

        final ImmutableList.Builder<Pattern> builder = ImmutableList.builder();
        for (String patternString : patternStrings) {
            builder.add(Pattern.compile(patternString.toUpperCase(Locale.ENGLISH)));
        }

        this.patterns = builder.build();
    }

    @Override
    public boolean apply(@Nonnull String input)
    {
        String value = input.toUpperCase(Locale.ENGLISH);
        for (Pattern pattern: patterns) {
            if (pattern.matcher(value).matches()) {
                LOG.debug(format("Ignoring '%s' (matches %s)", input, pattern.pattern()));
                return true;
            }
        }
        return false;
    }
}
