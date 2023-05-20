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
package org.basepom.mojo.duplicatefinder.artifact;

import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;

import static com.google.common.base.Preconditions.checkNotNull;

public class MavenCoordinates {

    private final String artifactId;
    private final String groupId;
    private final Optional<? extends ArtifactVersion> version;
    private final Optional<VersionRange> versionRange;
    private final String type;
    private final Optional<String> classifier;

    public MavenCoordinates(final Dependency dependency) throws InvalidVersionSpecificationException {
        checkNotNull(dependency, "dependency is null");

        this.artifactId = checkNotNull(dependency.getArtifactId(), "artifactId for dependency '%s' is null", dependency);
        this.groupId = checkNotNull(dependency.getGroupId(), "groupId for dependency '%s' is null", dependency);

        final String version = dependency.getVersion();
        this.version = version == null ? Optional.<ArtifactVersion>absent() : Optional.of(new DefaultArtifactVersion(version));

        if (this.version.isPresent()) {
            this.versionRange = Optional.of(VersionRange.createFromVersionSpec(version));
        } else {
            this.versionRange = Optional.absent();
        }

        final String type = dependency.getType();
        final String classifier = dependency.getClassifier();
        if ("test-jar".equals(type)) {
            this.classifier = Optional.of(MoreObjects.firstNonNull(classifier, "tests"));
            this.type = "jar";
        } else {
            this.type = MoreObjects.firstNonNull(type, "jar");
            this.classifier = Optional.fromNullable(classifier);
        }
    }

    public MavenCoordinates(final Artifact artifact) throws OverConstrainedVersionException {
        checkNotNull(artifact, "artifact is null");

        this.artifactId = checkNotNull(artifact.getArtifactId(), "artifactId for artifact '%s' is null", artifact);
        this.groupId = checkNotNull(artifact.getGroupId(), "groupId for artifact '%s' is null", artifact);
        this.versionRange = Optional.fromNullable(artifact.getVersionRange());

        if (this.versionRange.isPresent()) {
            this.version = Optional.fromNullable(artifact.getSelectedVersion());
        } else {
            final String version = artifact.getBaseVersion();
            this.version = version == null ? Optional.<ArtifactVersion>absent() : Optional.of(new DefaultArtifactVersion(version));
        }

        final String type = artifact.getType();
        final String classifier = artifact.getClassifier();
        if ("test-jar".equals(type)) {
            this.classifier = Optional.of(MoreObjects.firstNonNull(classifier, "tests"));
            this.type = "jar";
        } else {
            this.type = MoreObjects.firstNonNull(type, "jar");
            this.classifier = Optional.fromNullable(classifier);
        }
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public Optional<? extends ArtifactVersion> getVersion() {
        return version;
    }

    public Optional<VersionRange> getVersionRange() {
        return versionRange;
    }

    public String getType() {
        return type;
    }

    public Optional<String> getClassifier() {
        return classifier;
    }

    public boolean matches(final Artifact artifact) throws OverConstrainedVersionException {
        return matches(new MavenCoordinates(artifact));
    }

    public boolean matches(final MavenCoordinates other) {
        if (!(Objects.equals(getGroupId(), other.getGroupId())
                && Objects.equals(getArtifactId(), other.getArtifactId())
                && Objects.equals(getType(), other.getType()))) {
            return false;
        }

        // If a classifier is present, try to match the other classifier,
        // otherwise, if no classifier is present, it matches all classifiers from the other MavenCoordinates.
        if (getClassifier().isPresent()) {
            if (!Objects.equals(getClassifier().get(), other.getClassifier().orNull())) {
                return false;
            }
        }

        // no version range and no version present, so any other version matches
        if (!getVersionRange().isPresent() && !getVersion().isPresent()) {
            return true;
        }

        // starting here, either a version or a version range is present

        // other has no version. So there can be no match
        if (!other.getVersion().isPresent()) {
            return false;
        }

        // version range local and version in other
        if (getVersionRange().isPresent()) {
            // is there a recommended version?
            final ArtifactVersion recommendedVersion = getVersionRange().get().getRecommendedVersion();
            if (recommendedVersion != null) {
                // Yes, then it must be matched.
                return Objects.equals(recommendedVersion, other.getVersion().orNull());
            }

            // No, see if the other version is in the range
            if (getVersionRange().get().containsVersion(other.getVersion().get())) {
                return true;
            }
        }

        // exact version match.
        return Objects.equals(getVersion().orNull(), other.getVersion().orNull());
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, classifier, type);
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null || other.getClass() != this.getClass()) {
            return false;
        }
        if (other == this) {
            return true;
        }

        MavenCoordinates that = (MavenCoordinates) other;

        return Objects.equals(this.groupId, that.groupId)
                && Objects.equals(this.artifactId, that.artifactId)
                && Objects.equals(this.classifier, that.classifier)
                && Objects.equals(this.type, that.type);
    }

    @Override
    public String toString() {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();

        builder.add(getGroupId());
        builder.add(getArtifactId());

        if (getVersion().isPresent()) {
            builder.add(getVersion().get().toString());
        } else {
            builder.add("<any>");
        }

        builder.add(getType());
        builder.add(getClassifier().or("<any>"));
        return Joiner.on(':').join(builder.build());
    }
}
