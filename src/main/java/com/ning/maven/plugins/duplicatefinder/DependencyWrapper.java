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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;

public class DependencyWrapper
{
    private final Dependency   dependency;
    private final VersionRange versionRange;

    public DependencyWrapper(Dependency dependency) throws InvalidVersionSpecificationException
    {
        this.dependency   = dependency;
        this.versionRange = VersionRange.createFromVersionSpec(dependency.getVersion());
    }

    public String toString()
    {
        String result = dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion();

        if ((dependency.getType() != null) && !"jar".equals(dependency.getType())) {
            result = result +  ":" + dependency.getType();
        }
        if ((dependency.getClassifier() != null) && (!"tests".equals(dependency.getClassifier()) || !"test-jar".equals(dependency.getType()))) {
            result = result +  ":" + dependency.getClassifier();
        }
        return result;
    }

    public boolean matches(Artifact artifact)
    {
        ArtifactVersion version;

        try {
            if (artifact.getVersionRange() != null) {
                version = artifact.getSelectedVersion();
            }
            else {
                version = new DefaultArtifactVersion(artifact.getVersion());
            }
        }
        catch (OverConstrainedVersionException ex) {
            return false;
        }                

        return StringUtils.equals(dependency.getGroupId(), artifact.getGroupId()) &&
               StringUtils.equals(dependency.getArtifactId(), artifact.getArtifactId()) &&
               StringUtils.equals(StringUtils.defaultIfEmpty(dependency.getType(), "jar"), StringUtils.defaultIfEmpty(artifact.getType(), "jar")) &&
               StringUtils.equals(dependency.getClassifier(), artifact.getClassifier()) &&
               (versionRange == null || versionRange.containsVersion(version) || StringUtils.equals(artifact.getVersion(), dependency.getVersion()));
    }
}
