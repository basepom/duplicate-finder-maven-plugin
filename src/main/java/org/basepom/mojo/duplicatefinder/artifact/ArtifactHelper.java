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

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

import static com.google.common.base.Strings.nullToEmpty;

public final class ArtifactHelper {

    private ArtifactHelper() {
        throw new AssertionError("do not instantiate");
    }

    public static File getOutputDirectory(final MavenProject project) {
        return new File(project.getBuild().getOutputDirectory());
    }

    public static File getTestOutputDirectory(final MavenProject project) {
        return new File(project.getBuild().getTestOutputDirectory());
    }

    public static boolean isJarArtifact(final Artifact artifact) {
        return nullToEmpty(artifact.getType()).isEmpty() || "jar".equals(artifact.getType()) || "test-jar".equals(artifact.getType());
    }

    public static boolean isTestArtifact(final Artifact artifact) {
        return "test-jar".equals(artifact.getType()) || "tests".equals(artifact.getClassifier());
    }
}
