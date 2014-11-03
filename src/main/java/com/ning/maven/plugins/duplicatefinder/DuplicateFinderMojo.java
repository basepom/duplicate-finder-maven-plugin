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

import static java.lang.String.format;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.ning.maven.plugins.duplicatefinder.ConflictType.CLASS;
import static com.ning.maven.plugins.duplicatefinder.ConflictType.RESOURCE;
import static com.ning.maven.plugins.duplicatefinder.artifact.ArtifactHelper.getOutputDirectory;
import static com.ning.maven.plugins.duplicatefinder.artifact.ArtifactHelper.getTestOutputDirectory;

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_SYSTEM;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nonnull;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.ning.maven.plugins.duplicatefinder.ResultCollector.ConflictResult;
import com.ning.maven.plugins.duplicatefinder.artifact.ArtifactFileResolver;
import com.ning.maven.plugins.duplicatefinder.classpath.ClasspathDescriptor;
import com.pyx4j.log4j.MavenLogAppender;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Finds duplicate classes/resources on the classpath.
 */
@Mojo(name = "check",
                requiresProject = true,
                threadSafe = true,
                defaultPhase = LifecyclePhase.VERIFY,
                requiresDependencyResolution = ResolutionScope.TEST)
public final class DuplicateFinderMojo extends AbstractMojo
{
    private static final PluginLog LOG = new PluginLog(DuplicateFinderMojo.class);

    private static final HashFunction SHA_256 = Hashing.sha256();

    private static final Set<String> COMPILE_SCOPE = ImmutableSet.of(SCOPE_COMPILE, SCOPE_PROVIDED, SCOPE_SYSTEM);
    private static final Set<String> RUNTIME_SCOPE = ImmutableSet.of(SCOPE_COMPILE, SCOPE_RUNTIME);
    private static final Set<String> TEST_SCOPE = ImmutableSet.<String>of(); // Empty == all scopes

    /**
     * The maven project (effective pom).
     */
    @Component
    private MavenProject project;

    /**
     * Report files that have the same sha256 has value.
     *
     * @since 1.0.6
     */
    @Parameter(defaultValue = "false")
    protected boolean printEqualFiles = false;

    /**
     * Fail the build if files with the same name but different content are detected.
     *
     * @since 1.0.3
     */
    @Parameter(defaultValue = "false")
    protected boolean failBuildInCaseOfDifferentContentConflict;

    /**
     * Fail the build if files with the same name and the same content are detected.
     * @since 1.0.3
     */
    @Parameter(defaultValue = "false")
    protected boolean failBuildInCaseOfEqualContentConflict;

    /**
     * Fail the build if any files with the same name are found.
     */
    @Parameter(defaultValue = "false")
    protected boolean failBuildInCaseOfConflict;

    /**
     * Use the default resource ignore list.
     */
    @Parameter(defaultValue = "true")
    protected boolean useDefaultResourceIgnoreList = true;

    /**
     * Ignored resources, which are not checked for multiple occurences.
     */
    @Parameter
    protected String[] ignoredResources = new String[0];

    /**
     * Artifacts with expected and resolved versions that are checked.
     */
    @Parameter(alias = "exceptions", property = "exceptions")
    protected ConflictingDependency[] conflictingDependencies = new ConflictingDependency[0];

    /**
     * Dependencies that should not be checked at all.
     */
    @Parameter(property = "ignoredDependencies")
    protected Dependency[] ignoredDependencies = new Dependency[0];

    /**
     * Check resources and classes on the compile class path.
     */
    @Parameter(defaultValue = "true")
    protected boolean checkCompileClasspath = true;

    /**
     * Check resources and classes on the runtime class path.
     */
    @Parameter(defaultValue = "true")
    protected boolean checkRuntimeClasspath = true;

    /**
     * Check resources and classes on the test class path.
     */
    @Parameter(defaultValue = "true")
    protected boolean checkTestClasspath = true;

    /**
     * Skips the plugin execution.
     */
    @Parameter(defaultValue = "false")
    protected boolean skip = false;

    /**
     * Quiets the plugin (report only errors).
     *
     * @since 1.1.0
     */
    @Parameter(defaultValue = "false")
    protected boolean quiet = false;

    /**
     * Whether existing local directories with classes or existing artifacts are preferred.
     *
     * @since 1.1.0
     */
    @Parameter(defaultValue = "true")
    protected boolean preferLocal = true;

    /**
     * Output file for the result of the plugin.
     *
     * @since 1.1.0
     */
    @Parameter
    protected File resultFile = null;

    @Override
    public void setLog(final Log log)
    {
        super.setLog(log);
        MavenLogAppender.startPluginLog(this);
    }

    @Override
    public void execute() throws MojoExecutionException
    {
        try {
            if (skip) {
                LOG.debug("Skipping execution!");
            }
            else if ("pom".equals(project.getArtifact().getType())) {
                LOG.debug("Ignoring POM project");
            }
            else {
                ResultCollector resultCollector = new ResultCollector();

                // For any of the build failures being set, the equal files should also be print out.
                printEqualFiles |= failBuildInCaseOfConflict || failBuildInCaseOfEqualContentConflict;

                try {
                    final ArtifactFileResolver artifactFileResolver = new ArtifactFileResolver(project, preferLocal);

                    if (checkCompileClasspath) {
                        LOG.report(quiet, "Checking compile classpath");
                        checkClasspath(resultCollector, artifactFileResolver, COMPILE_SCOPE, getOutputDirectory(project));
                    }

                    if (checkRuntimeClasspath) {
                        LOG.report(quiet, "Checking runtime classpath");
                        checkClasspath(resultCollector, artifactFileResolver, RUNTIME_SCOPE, getOutputDirectory(project));
                    }

                    if (checkTestClasspath) {
                        LOG.report(quiet, "Checking test classpath");
                        checkClasspath(resultCollector, artifactFileResolver, TEST_SCOPE, getOutputDirectory(project), getTestOutputDirectory(project));
                    }
                }
                catch (final DependencyResolutionRequiredException e) {
                    throw new MojoExecutionException("Could not resolve dependencies", e);
                }
                catch (final InvalidVersionSpecificationException e) {
                    throw new MojoExecutionException("Invalid version specified", e);
                }
                catch (final OverConstrainedVersionException e) {
                    throw new MojoExecutionException("Version too constrained", e);
                }
            }
        }
        finally {
            MavenLogAppender.endPluginLog(this);
        }
    }

    /**
     * Checks the maven classpath for a given set of scopes whether it contains duplicates. In addition to the
     * artifacts on the classpath, one or more additional project folders are added.
     */
    private void checkClasspath(final ResultCollector resultCollector, final ArtifactFileResolver artifactFileResolver, final Set<String> scopes, final File ... projectFolders)
        throws MojoExecutionException,
        InvalidVersionSpecificationException,
        OverConstrainedVersionException,
        DependencyResolutionRequiredException
    {
        // Map of files to artifacts. Depending on the type of build, referenced projects in a multi-module build
        // may be local folders in the project instead of repo jar references.
        final Map<File, Artifact> fileToArtifactMap = artifactFileResolver.resolveArtifactsForScopes(scopes);

        final ClasspathDescriptor classpathDescriptor = ClasspathDescriptor.createClasspathDescriptor(project,
            fileToArtifactMap,
            Arrays.asList(ignoredResources),
            Arrays.asList(ignoredDependencies),
            useDefaultResourceIgnoreList,
            projectFolders);

        // Now a scope specific classpath descriptor (scope relevant artifacts and project folders) and the global artifact resolver
        // are primed. Run conflict resolution for classes and resources.
        checkForDuplicates(CLASS, resultCollector, classpathDescriptor, artifactFileResolver);
        checkForDuplicates(RESOURCE, resultCollector, classpathDescriptor, artifactFileResolver);

        switch (resultCollector.getConflictState()) {
            case CONFLICT_CONTENT_DIFFERENT:
                printConflicts(resultCollector);
                if (failBuildInCaseOfConflict || failBuildInCaseOfDifferentContentConflict || failBuildInCaseOfEqualContentConflict) {
                    throw new MojoExecutionException("Found duplicate classes/resources");
                }
                break;
            case CONFLICT_CONTENT_EQUAL:
                printConflicts(resultCollector);
                if (failBuildInCaseOfConflict || failBuildInCaseOfEqualContentConflict) {
                    throw new MojoExecutionException("Found duplicate classes/resources");
                }
                break;
            default:
                break;
        }
    }

    private void checkForDuplicates(final ConflictType type, final ResultCollector resultCollector, final ClasspathDescriptor classpathDescriptor, final ArtifactFileResolver artifactFileResolver) throws MojoExecutionException, OverConstrainedVersionException
    {
        // only look at entries with a size > 1.
        final Map<String, Collection<File>> filteredMap = ImmutableMap.copyOf(Maps.filterEntries(classpathDescriptor.getClasspathElementLocations(type), new Predicate<Entry<String, Collection<File>>>() {

            @Override
            public boolean apply(@Nonnull final Entry<String, Collection<File>> entry)
            {
                checkNotNull(entry, "entry is null");
                checkState(entry.getValue() != null, "Entry '%s' is invalid", entry);

                return entry.getValue().size() > 1;
            }

        }));

        for (final Map.Entry<String, Collection<File>> entry : filteredMap.entrySet()) {
            final String name = entry.getKey();
            final Collection<File> elements = entry.getValue();

            // Map which contains a printable name for the conflicting entry (which is either the printable name for an artifact or
            // a folder name for a project folder) as keys and an optional artifact as value.
            final Map<String, Optional<Artifact>> conflictArtifactNames = artifactFileResolver.getArtifactNamesForElements(elements);

            ImmutableSet.Builder<Artifact> artifactBuilder = ImmutableSet.<Artifact>builder().addAll(Optional.presentInstances(conflictArtifactNames.values()));

            if (artifactBuilder.build().size() < conflictArtifactNames.size()) {
                // One or more of the project folders are involved. Add the project artifact
                artifactBuilder.add(project.getArtifact()).build();
            }

            Set<Artifact> artifacts = artifactBuilder.build();

            boolean excepted = isExcepted(type, name, artifacts);
            ConflictState conflictState = DuplicateFinderMojo.determineConflictState(type, name, elements);

            resultCollector.addConflict(type, name, conflictArtifactNames, excepted, conflictState);
        }
    }

    public void printConflicts(ResultCollector collector)
    {
        // Only print equal files if the print flag is set.
        EnumSet<ConflictState> conflictSet = printEqualFiles
                        ? EnumSet.of(ConflictState.CONFLICT_CONTENT_EQUAL, ConflictState.CONFLICT_CONTENT_DIFFERENT)
                        : EnumSet.of(ConflictState.CONFLICT_CONTENT_DIFFERENT);

        for (ConflictState state : conflictSet) {
            for (ConflictType type : ConflictType.values()) {
                if (collector.hasConflictsFor(type, state)) {
                    Map<String, Collection<ConflictResult>> results = collector.getResults(type, state);
                    for (final Map.Entry<String, Collection<ConflictResult>> entry : results.entrySet()) {
                        final String artifactNames = entry.getKey();
                        final Collection<ConflictResult> conflictResults = entry.getValue();

                        LOG.warn("Found duplicate %s %s in [%s] :", state.getHint(), type.getType(), artifactNames);
                        for (final ConflictResult conflictResult : conflictResults) {
                            LOG.warn("  %s", conflictResult.getName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Detects class/resource differences via SHA256 hash comparison.
     */
    private static ConflictState determineConflictState(final ConflictType type, final String name, final Iterable<File> elements)
    {
        File firstFile = null;
        String firstSHA256 = null;

        final String resourcePath = type == ConflictType.CLASS ? name.replace('.', '/') + ".class" : name;

        for (final File element : elements)
        {
            try {
                final String newSHA256 = getSHA256HexOfElement(element, resourcePath);

                if (firstSHA256 == null) {
                    // save sha256 hash from the first element
                    firstSHA256 = newSHA256;
                    firstFile = element;
                }
                else if (!newSHA256.equals(firstSHA256)) {
                    LOG.debug("Found different SHA256 hashes for elements %s in file %s and %s", resourcePath, firstFile, element);
                    return ConflictState.CONFLICT_CONTENT_DIFFERENT;
                }
            }
            catch (final IOException ex) {
                LOG.warn(ex, "Could not read content from file %s!", element);
            }
        }

        return ConflictState.CONFLICT_CONTENT_EQUAL;
    }

    /**
     * Calculates the SHA256 Hash of a class in a file.
     *
     * @param file the archive contains the class
     * @param resourcePath the name of the class
     * @return the MD% Hash as Hex-Value
     * @throws IOException if any error occurs on reading class in archive
     */
    private static String getSHA256HexOfElement(final File file, final String resourcePath) throws IOException
    {
        final Closer closer = Closer.create();
        InputStream in;

        try {
            if (file.isDirectory()) {
                final File resourceFile = new File(file, resourcePath);
                in = closer.register(new BufferedInputStream(new FileInputStream(resourceFile)));
            }
            else {
                final ZipFile zip = new ZipFile(file);

                closer.register(new Closeable() {
                    @Override
                    public void close() throws IOException
                    {
                        zip.close();
                    }
                });

                final ZipEntry zipEntry = zip.getEntry(resourcePath);

                if (zipEntry == null) {
                    throw new IOException(format("Could not find %s in archive %s", resourcePath, file));
                }

                in = zip.getInputStream(zipEntry);
            }

            return SHA_256.newHasher().putBytes(ByteStreams.toByteArray(in)).hash().toString();
        }
        finally {
            closer.close();
        }
    }

    private boolean isExcepted(final ConflictType type, final String name, final Set<Artifact> artifacts) throws OverConstrainedVersionException
    {
        final ImmutableSet.Builder<ConflictingDependency> conflictBuilder = ImmutableSet.builder();
        checkState(conflictingDependencies != null, "conflictingDependencies is null");
        for (final ConflictingDependency conflictingDependency : Arrays.asList(conflictingDependencies)) {
            if (conflictingDependency.isForArtifacts(artifacts)) {
                conflictBuilder.add(conflictingDependency);
            }
        }

        for (final ConflictingDependency conflictingDependency : conflictBuilder.build()) {
            if (type == ConflictType.CLASS && conflictingDependency.containsClass(name)) {
                return true;
            }
            else if (type == ConflictType.RESOURCE && conflictingDependency.containsResource(name)) {
                return true;
            }
        }
        return false;
    }
}
