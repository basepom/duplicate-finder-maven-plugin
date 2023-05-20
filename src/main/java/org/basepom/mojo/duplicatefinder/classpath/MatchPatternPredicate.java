/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.basepom.mojo.duplicatefinder.classpath;

import java.util.Collection;
import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.basepom.mojo.duplicatefinder.PluginLog;

import static com.google.common.base.Preconditions.checkNotNull;

class MatchPatternPredicate implements Predicate<String> {

    private static final PluginLog LOG = new PluginLog(MatchPatternPredicate.class);

    private final ImmutableList<Pattern> patterns;

    MatchPatternPredicate(final Collection<String> patternStrings) {
        checkNotNull(patternStrings, "patternStrings is null");

        final ImmutableList.Builder<Pattern> builder = ImmutableList.builder();
        for (final String patternString : patternStrings) {
            builder.add(Pattern.compile(patternString, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }

        this.patterns = builder.build();
    }

    public ImmutableList<Pattern> getPatterns() {
        return patterns;
    }

    @Override
    public boolean apply(final String input) {
        if (input != null) {
            for (final Pattern pattern : patterns) {
                if (pattern.matcher(input).matches()) {
                    LOG.debug("Ignoring '%s' (matches %s)", input, pattern.pattern());
                    return true;
                }
            }
        }
        return false;
    }
}
