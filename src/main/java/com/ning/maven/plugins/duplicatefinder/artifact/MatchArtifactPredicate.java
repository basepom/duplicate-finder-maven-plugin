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
package com.ning.maven.plugins.duplicatefinder.artifact;

import static java.lang.String.format;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchArtifactPredicate implements Predicate<Artifact>
{
    private static final Logger LOG = LoggerFactory.getLogger(MatchArtifactPredicate.class);

    private final List<MavenCoordinates> mavenCoordinates;

    public MatchArtifactPredicate(Dependency ... dependencies)
        throws InvalidVersionSpecificationException
    {
        ImmutableList.Builder<MavenCoordinates> builder = ImmutableList.builder();
        for (Dependency dependency : dependencies) {
            builder.add(new MavenCoordinates(dependency));
        }
        this.mavenCoordinates = builder.build();
    }

    @Override
    public boolean apply(@Nonnull Artifact artifact)
    {
        for (MavenCoordinates mavenCoordinate : mavenCoordinates) {
            try {
                if (mavenCoordinate.matches(artifact)) {
                    LOG.debug(format("Ignoring artifact '%s' (matches %s)", artifact, mavenCoordinate));
                    return true;
                }
            }
            catch (OverConstrainedVersionException e) {
                LOG.warn(format("Caught '%s' while comparing '%s' to '%s'", e.getMessage(), mavenCoordinate, artifact));
            }
        }

        return false;
    }
}
