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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import org.basepom.mojo.duplicatefinder.artifact.MavenCoordinates;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchArtifactPredicate implements Predicate<Artifact> {

    private static final Logger LOG = LoggerFactory.getLogger(MatchArtifactPredicate.class);

    private final List<MavenCoordinates> mavenCoordinates;

    MatchArtifactPredicate(final Collection<MavenCoordinates> dependencies) {
        this.mavenCoordinates = ImmutableList.copyOf(checkNotNull(dependencies, "dependencies is null"));
    }

    @Override
    public boolean apply(final Artifact artifact) {
        if (artifact != null) {
            for (final MavenCoordinates mavenCoordinate : mavenCoordinates) {
                try {
                    if (mavenCoordinate.matches(artifact)) {
                        LOG.debug(format("Ignoring artifact '%s' (matches %s)", artifact, mavenCoordinate));
                        return true;
                    }
                } catch (final OverConstrainedVersionException e) {
                    LOG.warn(format("Caught '%s' while comparing '%s' to '%s'", e.getMessage(), mavenCoordinate, artifact));
                }
            }
        }
        return false;
    }
}
