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
package org.basepom.mojo.duplicatefinder;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder.SetMultimapBuilder;
import com.google.common.collect.Multimaps;

import org.apache.maven.artifact.Artifact;

public class ResultCollector
{
    private final EnumSet<ConflictState> printState;
    private final EnumSet<ConflictState> failState;

    private final EnumMap<ConflictType, EnumSet<ConflictState>> seenResults = Maps.newEnumMap(ConflictType.class);

    private final ListMultimap<String, ConflictResult> results = SetMultimapBuilder.linkedHashKeys().arrayListValues().build();

    private ConflictState conflictState = ConflictState.NO_CONFLICT;

    ResultCollector(final EnumSet<ConflictState> printState, final EnumSet<ConflictState> failState)
    {
        this.printState = printState;
        this.failState = failState;

        for (ConflictType conflictType : ConflictType.values()) {
            seenResults.put(conflictType, EnumSet.noneOf(ConflictState.class));
        }
    }

    public ConflictState getConflictState()
    {
        return conflictState;
    }

    public boolean isFailed()
    {
        return failState.contains(conflictState);
    }

    public boolean hasConflictsFor(ConflictType type, ConflictState state)
    {
        return seenResults.get(type).contains(state);
    }

    public void addConflict(ConflictType type, String name, Map<String, Optional<Artifact>> conflictArtifactNames, boolean excepted, final ConflictState state)
    {
        if (!excepted) {
            this.conflictState = ConflictState.max(this.conflictState, state);

            // Record the type of conflicts seen.
            seenResults.get(type).add(state);
        }

        ConflictResult conflictResult = new ConflictResult(type, name, conflictArtifactNames, excepted, state);

        results.put(conflictResult.getConflictArtifactsName(), conflictResult);
    }

    public Map<String, Collection<ConflictResult>> getResults(final ConflictType type, final ConflictState state)
    {
        Multimap<String, ConflictResult> result = Multimaps.filterValues(results, new Predicate<ConflictResult>() {

            @Override
            public boolean apply(@Nonnull ConflictResult conflictResult)
            {
                checkNotNull(conflictResult, "conflictResult is null");
                return conflictResult.getConflictState() == state
                    && conflictResult.getType() == type
                    && !conflictResult.isExcepted();
            }
        });

        return ImmutableMap.copyOf(result.asMap());
    }

    Map<String, Collection<ConflictResult>> getAllResults()
    {
        return ImmutableMap.copyOf(results.asMap());
    }

    private static String buildConflictArtifactsName(final Map<String, Optional<Artifact>> conflictArtifactNames)
    {
        return Joiner.on(", ").join(conflictArtifactNames.keySet());
    }

    public class ConflictResult
    {
        private final ConflictType type;
        private final String name;
        private final Map<String, Optional<Artifact>> conflictArtifactNames;
        private final boolean excepted;
        private final ConflictState conflictState;
        private final String conflictArtifactsName;

        ConflictResult(final ConflictType type,
                       final String name,
                       final Map<String, Optional<Artifact>> conflictArtifactNames,
                       final boolean excepted,
                       final ConflictState conflictState)
        {
            this.type = type;
            this.name = name;
            this.conflictArtifactNames = conflictArtifactNames;
            this.excepted = excepted;
            this.conflictState = conflictState;
            this.conflictArtifactsName = ResultCollector.buildConflictArtifactsName(conflictArtifactNames);
        }

        public String getName()
        {
            return name;
        }

        public ConflictType getType()
        {
            return type;
        }

        public Map<String, Optional<Artifact>> getConflictArtifactNames()
        {
            return conflictArtifactNames;
        }

        public boolean isExcepted()
        {
            return excepted;
        }

        public boolean isPrinted()
        {
            return !excepted && printState.contains(conflictState);
        }

        public boolean isFailed()
        {
            return !excepted && failState.contains(conflictState);
        }

        public ConflictState getConflictState()
        {
            return conflictState;
        }

        private String getConflictArtifactsName()
        {
            return conflictArtifactsName;
        }
    }
}
