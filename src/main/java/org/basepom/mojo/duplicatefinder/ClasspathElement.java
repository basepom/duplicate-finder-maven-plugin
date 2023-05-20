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

import java.io.File;
import java.util.Objects;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Ordering;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.artifact.Artifact;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.nullToEmpty;
import static org.basepom.mojo.duplicatefinder.artifact.ArtifactHelper.isJarArtifact;
import static org.basepom.mojo.duplicatefinder.artifact.ArtifactHelper.isTestArtifact;

/**
 * Describes any of the possible elements on the classpath. These can be elements from the boot classpath, artifacts and local
 * folder from the current project. For each of the possible types exists a subclass which holds additional information.
 */
public abstract class ClasspathElement implements Comparable<ClasspathElement> {

    public abstract String getName();

    public File getFile() {
        throw new UnsupportedOperationException();
    }

    public Artifact getArtifact() {
        throw new UnsupportedOperationException();
    }

    public boolean isBootClasspathElement() {
        return false;
    }

    public boolean isLocalFolder() {
        return false;
    }

    public boolean hasArtifact() {
        return false;
    }

    @Override
    public int compareTo(ClasspathElement element) {
        return Ordering.natural().compare(this.getName(), element.getName());
    }

    public static final class ClasspathArtifact extends ClasspathElement {

        private final Artifact artifact;

        public ClasspathArtifact(final Artifact artifact) {
            this.artifact = checkNotNull(artifact, "artifact is null");
        }

        @Override
        public String getName() {
            return Joiner.on(':').skipNulls().join(artifact.getGroupId(),
                    artifact.getArtifactId(),
                    artifact.getVersion(),
                    getType(artifact),
                    getClassifier(artifact));
        }

        private String getType(final Artifact artifact) {
            if (isJarArtifact(artifact)) {
                // when the classifier is null but the type is jar, return null
                // so that in the Joiner expression both the type and classifier
                // are null and none is printed.
                return nullToEmpty(artifact.getClassifier()).isEmpty() ? null : "jar";
            }

            return artifact.getType();
        }

        private String getClassifier(final Artifact artifact) {
            if (nullToEmpty(artifact.getClassifier()).isEmpty()) {
                return null;
            } else if (isTestArtifact(artifact)) {
                return "tests";
            }

            return artifact.getClassifier();
        }

        @Override
        public boolean hasArtifact() {
            return true;
        }

        @Override
        @SuppressFBWarnings("EI_EXPOSE_REP")
        public Artifact getArtifact() {
            return artifact;
        }

        @Override
        public int hashCode() {
            return Objects.hash(artifact);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }

            if (o == this) {
                return true;
            }

            ClasspathArtifact that = (ClasspathArtifact) o;

            return Objects.equals(this.artifact, that.artifact);
        }
    }

    public static final class ClasspathLocalFolder extends ClasspathElement {

        private final File localFolder;

        public ClasspathLocalFolder(final File localFolder) {
            this.localFolder = checkNotNull(localFolder, "localFolder is null");
            checkState(localFolder.isDirectory(), "localFolder must be a directory");
        }

        @Override
        public String getName() {
            return localFolder.getAbsolutePath();
        }

        @Override
        public boolean isLocalFolder() {
            return true;
        }

        @Override
        public File getFile() {
            return localFolder;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(localFolder);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }

            if (o == this) {
                return true;
            }

            ClasspathLocalFolder that = (ClasspathLocalFolder) o;

            return Objects.equals(this.localFolder, that.localFolder);
        }
    }

    public static Function<ClasspathElement, String> getNameFunction() {
        return ClasspathElement::getName;
    }
}
