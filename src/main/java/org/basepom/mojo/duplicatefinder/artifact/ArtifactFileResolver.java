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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import static org.basepom.mojo.duplicatefinder.artifact.ArtifactHelper.getOutputDirectory;
import static org.basepom.mojo.duplicatefinder.artifact.ArtifactHelper.getTestOutputDirectory;
import static org.basepom.mojo.duplicatefinder.artifact.ArtifactHelper.isTestArtifact;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.project.MavenProject;
import org.basepom.mojo.duplicatefinder.ClasspathElement;
import org.basepom.mojo.duplicatefinder.PluginLog;
import org.basepom.mojo.duplicatefinder.ClasspathElement.ClasspathArtifact;
import org.basepom.mojo.duplicatefinder.ClasspathElement.ClasspathBootClasspathElement;
import org.basepom.mojo.duplicatefinder.ClasspathElement.ClasspathLocalFolder;

/**
 * Resolves artifact references from the project into local and repository files and folders.
 *
 * Only manages the dependencies because the main project can have multiple (two) folders
 * for the project. This is not supported by this resolver.
 */
public class ArtifactFileResolver
{
    private static final PluginLog LOG = new PluginLog(ArtifactFileResolver.class);

    private final BiMap<Artifact, File> localArtifactCache;
    private final BiMap<Artifact, File> repoArtifactCache;
    private final ImmutableSet<File> bootClasspath;

    private final boolean preferLocal;

    public ArtifactFileResolver(final MavenProject project,
                                final ImmutableSet<File> bootClasspath,
                                final boolean preferLocal) throws DependencyResolutionRequiredException
    {
        checkNotNull(project, "project is null");
        this.preferLocal = preferLocal;

        this.localArtifactCache = HashBiMap.create(project.getArtifacts().size());
        this.repoArtifactCache = HashBiMap.create(project.getArtifacts().size());
        this.bootClasspath = bootClasspath;

        for (final Artifact artifact : project.getArtifacts()) {
            final File repoPath = artifact.getFile();
            final Artifact canonicalizedArtifact = ArtifactFileResolver.canonicalizeArtifact(artifact);

            checkState(repoPath != null && repoPath.exists(), "Repository Path '%s' does not exist.", repoPath);
            final File oldFile = repoArtifactCache.put(canonicalizedArtifact, repoPath);
            checkState(oldFile == null || oldFile.equals(repoPath), "Already encountered a file for %s: %s", canonicalizedArtifact, oldFile);
        }

        for (final MavenProject referencedProject : project.getProjectReferences().values()) {
            // referenced projects only have GAV coordinates but no scope.
            final Set<Artifact> repoArtifacts = findRepoArtifacts(referencedProject, repoArtifactCache);
            checkState(!repoArtifacts.isEmpty(), "Found project reference to %s but no repo reference!", referencedProject.getArtifact());

            for (final Artifact artifact : repoArtifacts) {

                final File outputDir = isTestArtifact(artifact) ? getTestOutputDirectory(referencedProject) : getOutputDirectory(referencedProject);

                if (outputDir != null && outputDir.exists()) {
                    final File oldFile = localArtifactCache.put(artifact, outputDir);
                    checkState(oldFile == null || oldFile.equals(outputDir), "Already encountered a file for %s: %s", artifact, oldFile);
                }
            }
        }
    }

    /**
     * Creates a list of
     * @param scopes
     * @return
     * @throws InvalidVersionSpecificationException
     * @throws DependencyResolutionRequiredException
     */
    public ImmutableMap<File, Artifact> resolveArtifactsForScopes(final Set<String> scopes) throws InvalidVersionSpecificationException, DependencyResolutionRequiredException
    {
        checkNotNull(scopes, "scopes is null");

        final ImmutableMap.Builder<File, Artifact> inScopeBuilder = ImmutableMap.builder();
        for (final Artifact artifact : listArtifacts()) {
            if (artifact.getArtifactHandler().isAddedToClasspath()) {
                if (scopes.isEmpty() || scopes.contains(artifact.getScope())) {
                    final File file = resolveFileForArtifact(artifact);
                    checkState(file != null, "No file for artifact '%s' found!", artifact);
                    inScopeBuilder.put(file, artifact);
                }
            }
        }

        return inScopeBuilder.build();
    }

    public ImmutableSortedSet<ClasspathElement> getClasspathElementsForElements(final Collection<File> elements)
    {
        final ImmutableSortedSet.Builder<ClasspathElement> builder = ImmutableSortedSet.naturalOrder();

        for (final File element : elements) {
            builder.add(resolveClasspathElementForFile(element));
        }
        return builder.build();
    }

    private ClasspathElement resolveClasspathElementForFile(final File file)
    {
        checkNotNull(file, "file is null");

        if (preferLocal && localArtifactCache.inverse().containsKey(file)) {
            return new ClasspathArtifact(localArtifactCache.inverse().get(file));
        }

        if (repoArtifactCache.inverse().containsKey(file)) {
            return new ClasspathArtifact(repoArtifactCache.inverse().get(file));
        }

        if (bootClasspath.contains(file)) {
            return new ClasspathBootClasspathElement(file);
        }

        if (localArtifactCache.inverse().containsKey(file)) {
            return new ClasspathArtifact(localArtifactCache.inverse().get(file));
        }

        return new ClasspathLocalFolder(file);
    }

    private File resolveFileForArtifact(final Artifact artifact)
    {
        checkNotNull(artifact, "artifact is null");

        if (preferLocal && localArtifactCache.containsKey(artifact)) {
            return localArtifactCache.get(artifact);
        }

        if (repoArtifactCache.containsKey(artifact)) {
            return repoArtifactCache.get(artifact);
        }

        return localArtifactCache.get(artifact);
    }

    @VisibleForTesting
    static DefaultArtifact canonicalizeArtifact(final Artifact artifact)
    {
        final VersionRange versionRange = artifact.getVersionRange() == null ? VersionRange.createFromVersion(artifact.getVersion()) : artifact.getVersionRange();
        String type = MoreObjects.firstNonNull(artifact.getType(), "jar");
        String classifier = artifact.getClassifier();

        if ("test-jar".equals(type) && (classifier == null || "tests".equals(classifier))) {
            type = "jar";
            classifier = "tests";
        }

        final DefaultArtifact canonicalizedArtifact = new DefaultArtifact(artifact.getGroupId(),
                                                                          artifact.getArtifactId(),
                                                                          versionRange,
                                                                          artifact.getScope(),
                                                                          type,
                                                                          classifier,
                                                                          artifact.getArtifactHandler(),
                                                                          artifact.isOptional());

        return canonicalizedArtifact;
    }

    private Set<Artifact> listArtifacts()
    {
        return ImmutableSet.<Artifact>builder().addAll(localArtifactCache.keySet()).addAll(repoArtifactCache.keySet()).build();
    }

    private static Set<Artifact> findRepoArtifacts(final MavenProject project, final Map<Artifact, File> repoArtifactCache)
    {
        final ImmutableSet.Builder<Artifact> builder = ImmutableSet.builder();

        for (final Artifact artifact : repoArtifactCache.keySet()) {
            if (Objects.equal(project.getArtifact().getGroupId(), artifact.getGroupId())
                && Objects.equal(project.getArtifact().getArtifactId(), artifact.getArtifactId())
                && Objects.equal(project.getArtifact().getBaseVersion(), artifact.getBaseVersion())) {
                builder.add(artifact);
            }
        }
        return builder.build();
    }

    private static File getLocalProjectPath(final MavenProject project, final Artifact artifact) throws DependencyResolutionRequiredException
    {
        final String refId = Joiner.on(':').join(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        final MavenProject owningProject = project.getProjectReferences().get(refId);

        if (owningProject != null) {
            final File outputDir = isTestArtifact(artifact) ? getTestOutputDirectory(owningProject) : getOutputDirectory(owningProject);

            if (outputDir.exists()) {
                return outputDir;
            }
        }

        return null;
    }
}
