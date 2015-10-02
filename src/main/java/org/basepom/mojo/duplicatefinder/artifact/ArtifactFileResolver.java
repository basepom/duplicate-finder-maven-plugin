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
import java.util.Objects;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;

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

    // A BiMultimap would come in really handy here...
    private final Multimap<File, Artifact> localFileArtifactCache;
    private final Map<Artifact, File> localArtifactFileCache;

    private final BiMap<Artifact, File> repoArtifactCache;
    private final ImmutableSet<File> bootClasspath;

    private final boolean preferLocal;

    public ArtifactFileResolver(final MavenProject project,
        final ImmutableSet<File> bootClasspath,
        final boolean preferLocal) throws DependencyResolutionRequiredException
    {
        checkNotNull(project, "project is null");
        this.preferLocal = preferLocal;

        // This needs to be a multimap, because it is possible by jiggling with classifiers that a local project
        // (with local folders) does map to multiple artifacts and therefore the file <-> artifact relation is not
        // 1:1 but 1:n. As found in https://github.com/basepom/duplicate-finder-maven-plugin/issues/10
        ImmutableMultimap.Builder<File, Artifact> localFileArtifactCacheBuilder = ImmutableMultimap.builder();

        // This can not be an immutable map builder, because the map is used for looking up while it is built up.
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

            // This can happen if another sub-project in the reactor is e.g. used as a compiler plugin dependency.
            // In that case, the dependency will show up as a referenced project but not in the artifacts list from the project.
            // Fix up straight from the referenced project.
            if (repoArtifacts.isEmpty()) {
                LOG.debug("Found project reference to %s but no repo reference, probably used in a plugin dependency.", referencedProject.getArtifact());
            }

            for (final Artifact artifact : repoArtifacts) {

                final File outputDir = isTestArtifact(artifact) ? getTestOutputDirectory(referencedProject) : getOutputDirectory(referencedProject);

                if (outputDir != null && outputDir.exists()) {
                    localFileArtifactCacheBuilder.put(outputDir, artifact);
                }
            }
        }

        this.localFileArtifactCache = localFileArtifactCacheBuilder.build();

        // Flip the File -> Artifact multimap. This also acts as a sanity check because no artifact
        // must be present more than one and the Map.Builder will choke if a key is around more than
        // once.
        ImmutableMap.Builder<Artifact, File> localArtifactFileCacheBuilder = ImmutableMap.builder();
        for (Map.Entry<File, Artifact> entry : localFileArtifactCache.entries()) {
            localArtifactFileCacheBuilder.put(entry.getValue(), entry.getKey());
        }

        this.localArtifactFileCache = localArtifactFileCacheBuilder.build();
    }

    public ImmutableMultimap<File, Artifact> resolveArtifactsForScopes(final Set<String> scopes) throws InvalidVersionSpecificationException, DependencyResolutionRequiredException
    {
        checkNotNull(scopes, "scopes is null");

        final ImmutableMultimap.Builder<File, Artifact> inScopeBuilder = ImmutableMultimap.builder();
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
            resolveClasspathElementsForFile(element, builder);
        }
        return builder.build();
    }

    private void resolveClasspathElementsForFile(final File file, ImmutableSet.Builder<ClasspathElement> builder)
    {
        checkNotNull(file, "file is null");

        if (preferLocal && localFileArtifactCache.containsKey(file)) {
            for (Artifact artifact : localFileArtifactCache.get(file)) {
                builder.add(new ClasspathArtifact(artifact));
            }
            return;
        }

        if (repoArtifactCache.inverse().containsKey(file)) {
            builder.add(new ClasspathArtifact(repoArtifactCache.inverse().get(file)));
            return;
        }

        if (bootClasspath.contains(file)) {
            builder.add(new ClasspathBootClasspathElement(file));
            return;
        }

        if (localFileArtifactCache.containsKey(file)) {
            for (Artifact artifact : localFileArtifactCache.get(file)) {
                builder.add(new ClasspathArtifact(artifact));
            }
            return;
        }

        builder.add(new ClasspathLocalFolder(file));
    }

    private File resolveFileForArtifact(final Artifact artifact)
    {
        checkNotNull(artifact, "artifact is null");

        if (preferLocal && localArtifactFileCache.containsKey(artifact)) {
            return localArtifactFileCache.get(artifact);
        }

        if (repoArtifactCache.containsKey(artifact)) {
            return repoArtifactCache.get(artifact);
        }

        return localArtifactFileCache.get(artifact);
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
        return ImmutableSet.<Artifact>builder().addAll(localArtifactFileCache.keySet()).addAll(repoArtifactCache.keySet()).build();
    }

    private static Set<Artifact> findRepoArtifacts(final MavenProject project, final Map<Artifact, File> repoArtifactCache)
    {
        final ImmutableSet.Builder<Artifact> builder = ImmutableSet.builder();

        for (final Artifact artifact : repoArtifactCache.keySet()) {
            if (Objects.equals(project.getArtifact().getGroupId(), artifact.getGroupId())
                && Objects.equals(project.getArtifact().getArtifactId(), artifact.getArtifactId())
                && Objects.equals(project.getArtifact().getBaseVersion(), artifact.getBaseVersion())) {
                builder.add(artifact);
            }
        }
        return builder.build();
    }
}
