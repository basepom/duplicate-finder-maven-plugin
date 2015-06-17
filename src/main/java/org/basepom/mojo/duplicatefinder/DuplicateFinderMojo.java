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

import static java.lang.String.format;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_SYSTEM;
import static org.basepom.mojo.duplicatefinder.ConflictState.CONFLICT_CONTENT_DIFFERENT;
import static org.basepom.mojo.duplicatefinder.ConflictState.CONFLICT_CONTENT_EQUAL;
import static org.basepom.mojo.duplicatefinder.ConflictType.CLASS;
import static org.basepom.mojo.duplicatefinder.ConflictType.RESOURCE;
import static org.basepom.mojo.duplicatefinder.artifact.ArtifactHelper.getOutputDirectory;
import static org.basepom.mojo.duplicatefinder.artifact.ArtifactHelper.getTestOutputDirectory;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamException;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.pyx4j.log4j.MavenLogAppender;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.basepom.mojo.duplicatefinder.ResultCollector.ConflictResult;
import org.basepom.mojo.duplicatefinder.artifact.ArtifactFileResolver;
import org.basepom.mojo.duplicatefinder.artifact.MavenCoordinates;
import org.basepom.mojo.duplicatefinder.classpath.ClasspathDescriptor;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.staxmate.SMOutputFactory;
import org.codehaus.staxmate.out.SMOutputDocument;
import org.codehaus.staxmate.out.SMOutputElement;

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

    private static final int SAVE_FILE_VERSION = 1;

    private static final HashFunction SHA_256 = Hashing.sha256();

    private static final Set<String> COMPILE_SCOPE = ImmutableSet.of(SCOPE_COMPILE, SCOPE_PROVIDED, SCOPE_SYSTEM);
    private static final Set<String> RUNTIME_SCOPE = ImmutableSet.of(SCOPE_COMPILE, SCOPE_RUNTIME);
    private static final Set<String> TEST_SCOPE = ImmutableSet.<String>of(); // Empty == all scopes

    /**
     * The maven project (effective pom).
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * Report files that have the same sha256 has value.
     *
     * @since 1.0.6
     */
    @Parameter(defaultValue = "false", property = "duplicate-finder.printEqualFiles")
    protected boolean printEqualFiles = false;

    /**
     * Fail the build if files with the same name but different content are detected.
     *
     * @since 1.0.3
     */
    @Parameter(defaultValue = "false", property = "duplicate-finder.failBuildInCaseOfDifferentContentConflict")
    protected boolean failBuildInCaseOfDifferentContentConflict = false;

    /**
     * Fail the build if files with the same name and the same content are detected.
     * @since 1.0.3
     */
    @Parameter(defaultValue = "false", property = "duplicate-finder.failBuildInCaseOfEqualContentConflict")
    protected boolean failBuildInCaseOfEqualContentConflict = false;

    /**
     * Fail the build if any files with the same name are found.
     */
    @Parameter(defaultValue = "false", property = "duplicate-finder.failBuildInCaseOfConflict")
    protected boolean failBuildInCaseOfConflict = false;

    /**
     * Use the default resource ignore list.
     */
    @Parameter(defaultValue = "true", property = "duplicate-finder.useDefaultResourceIgnoreList")
    protected boolean useDefaultResourceIgnoreList = true;

    /**
     * Ignored resources, which are not checked for multiple occurences.
     *
     * @deprecated Use ignoredResourcePatterns.
     */
    @Deprecated
    @Parameter
    protected String[] ignoredResources = new String[0];

    /**
     * Ignored resources, which are not checked for multiple occurences.
     */
    @Parameter
    protected String[] ignoredResourcePatterns = new String[0];

    /**
     * Artifacts with expected and resolved versions that are checked.
     */
    @Parameter(alias = "exceptions")
    protected ConflictingDependency[] conflictingDependencies = new ConflictingDependency[0];

    /**
     * Dependencies that should not be checked at all.
     */
    @Parameter(alias = "ignoredDependencies")
    protected MavenCoordinates[] ignoredDependencies = new MavenCoordinates[0];

    /**
     * Check resources and classes on the compile class path.
     */
    @Parameter(defaultValue = "true", property = "duplicate-finder.checkCompileClasspath")
    protected boolean checkCompileClasspath = true;

    /**
     * Check resources and classes on the runtime class path.
     */
    @Parameter(defaultValue = "true", property = "duplicate-finder.checkRuntimeClasspath")
    protected boolean checkRuntimeClasspath = true;

    /**
     * Check resources and classes on the test class path.
     */
    @Parameter(defaultValue = "true", property = "duplicate-finder.checkTestClasspath")
    protected boolean checkTestClasspath = true;

    /**
     * Skips the plugin execution.
     */
    @Parameter(defaultValue = "false", property = "duplicate-finder.skip")
    protected boolean skip = false;

    /**
     * Quiets the plugin (report only errors).
     *
     * @since 1.1.0
     */
    @Parameter(defaultValue = "false", property = "duplicate-finder.quiet")
    protected boolean quiet = false;

    /**
     * Whether existing local directories with classes or existing artifacts are preferred.
     *
     * @since 1.1.0
     */
    @Parameter(defaultValue = "true", property = "duplicate-finder.preferLocal")
    protected boolean preferLocal = true;

    /**
     * Output file for the result of the plugin.
     *
     * @since 1.1.0
     */
    @Parameter(defaultValue = "${project.build.directory}/duplicate-finder-result.xml", property = "duplicate-finder.resultFile")
    protected File resultFile;

    /**
     * Write result to output file.
     *
     * @since 1.1.0
     */
    @Parameter(defaultValue = "true", property = "duplicate-finder.useResultFile")
    protected boolean useResultFile = true;

    /**
     * Minimum occurences on the class path to be listed in the result file.
     *
     * @since 1.1.0
     */
    @Parameter(defaultValue="2", property = "duplicate-finder.resultFileMinClasspathCount")
    protected int resultFileMinClasspathCount = 2;

    /**
     * Include the boot class path in duplicate detection. This will find duplicates with the JDK
     * internal classes (e.g. the classes in rt.jar).
     *
     * @since 1.1.1
     */
    @Parameter(defaultValue="false", property = "duplicate-finder.includeBootClasspath")
    protected boolean includeBootClasspath = false;

    /**
     * System property that contains the boot class path.
     *
     * @since 1.1.1
     */
    @Parameter(defaultValue="sun.boot.class.path", property = "duplicate-finder.bootClasspathProperty")
    protected String bootClasspathProperty = "sun.boot.class.path";

    /**
     * Include POM projects in validation.
     *
     * @since 1.2.0
     */
    @Parameter(defaultValue="false", property = "duplicate-finder.includePomProjects")
    protected boolean includePomProjects = false;

    private final EnumSet<ConflictState> printState = EnumSet.of(CONFLICT_CONTENT_DIFFERENT);
    private final EnumSet<ConflictState> failState = EnumSet.noneOf(ConflictState.class);

    @Override
    public void setLog(final Log log)
    {
        super.setLog(log);
        MavenLogAppender.startPluginLog(this);
    }

    // called by maven
    public void setIgnoredDependencies(final Dependency [] dependencies) throws InvalidVersionSpecificationException
    {
        checkArgument(dependencies != null);

        this.ignoredDependencies = new MavenCoordinates[dependencies.length];
        for (int idx = 0; idx < dependencies.length; idx++) {
            this.ignoredDependencies[idx] = new MavenCoordinates(dependencies[idx]);
        }
    }

    @Override
    public void execute() throws MojoExecutionException
    {
        try {
            if (skip) {
                LOG.report(quiet, "Skipping duplicate-finder execution!");
            }
            else if (!includePomProjects && "pom".equals(project.getArtifact().getType())) {
                LOG.report(quiet, "Ignoring POM project!");
            }
            else {
                if (printEqualFiles) {
                    printState.add(CONFLICT_CONTENT_EQUAL);
                }

                if (failBuildInCaseOfConflict || failBuildInCaseOfEqualContentConflict) {
                    printState.add(CONFLICT_CONTENT_EQUAL);

                    failState.add(CONFLICT_CONTENT_EQUAL);
                    failState.add(CONFLICT_CONTENT_DIFFERENT);
                }

                if (failBuildInCaseOfDifferentContentConflict) {
                    failState.add(CONFLICT_CONTENT_DIFFERENT);
                }

                // Deprecation warning for configuration values

                if (ignoredResources.length > 0) {
                    LOG.warn("<ignoreResources> has been deprecated and replaced with <ignoreResourcePatterns>. It will go away in version 1.2.0. Please update your POM accordingly!");
                }

                try {
                    // Prep conflicting dependencies
                    MavenCoordinates projectCoordinates = new MavenCoordinates(project.getArtifact());

                    for (ConflictingDependency conflictingDependency : conflictingDependencies) {
                        conflictingDependency.addProjectMavenCoordinates(projectCoordinates);
                    }

                    // Find boot classpath information

                    ImmutableSet.Builder<File> bootClasspathBuilder = ImmutableSet.builder();

                    if (includeBootClasspath) {
                        String value = System.getProperty(bootClasspathProperty);
                        if (value != null) {
                            for (String entry : Splitter.on(':').split(value)) {
                                File file = new File(entry);
                                if (file.exists()) {
                                    LOG.debug("Adding '%s' as a boot classpath element", entry);
                                    bootClasspathBuilder.add(file);
                                }
                                else {
                                    LOG.debug("Ignoring '%s', does not exist.", entry);
                                }
                            }
                        }
                    }

                    final ImmutableSet<File> bootClasspath = bootClasspathBuilder.build();

                    final ArtifactFileResolver artifactFileResolver = new ArtifactFileResolver(project, bootClasspath, preferLocal);
                    final ImmutableMap.Builder<String, Entry<ResultCollector, ClasspathDescriptor>> classpathResultBuilder = ImmutableMap.builder();

                    if (checkCompileClasspath) {
                        LOG.report(quiet, "Checking compile classpath");
                        final ResultCollector resultCollector = new ResultCollector(printState, failState);
                        final ClasspathDescriptor classpathDescriptor = checkClasspath(resultCollector, artifactFileResolver, COMPILE_SCOPE, bootClasspath, getOutputDirectory(project));
                        classpathResultBuilder.put("compile", new SimpleImmutableEntry<ResultCollector, ClasspathDescriptor>(resultCollector, classpathDescriptor));
                    }

                    if (checkRuntimeClasspath) {
                        LOG.report(quiet, "Checking runtime classpath");
                        final ResultCollector resultCollector = new ResultCollector(printState, failState);
                        final ClasspathDescriptor classpathDescriptor = checkClasspath(resultCollector, artifactFileResolver, RUNTIME_SCOPE, bootClasspath, getOutputDirectory(project));
                        classpathResultBuilder.put("runtime", new SimpleImmutableEntry<ResultCollector, ClasspathDescriptor>(resultCollector, classpathDescriptor));
                    }

                    if (checkTestClasspath) {
                        LOG.report(quiet, "Checking test classpath");
                        final ResultCollector resultCollector = new ResultCollector(printState, failState);
                        final ClasspathDescriptor classpathDescriptor = checkClasspath(resultCollector, artifactFileResolver, TEST_SCOPE, bootClasspath, getOutputDirectory(project), getTestOutputDirectory(project));
                        classpathResultBuilder.put("test", new SimpleImmutableEntry<ResultCollector, ClasspathDescriptor>(resultCollector, classpathDescriptor));
                    }

                    final ImmutableMap<String, Entry<ResultCollector, ClasspathDescriptor>> classpathResults = classpathResultBuilder.build();

                    if (useResultFile) {
                        checkState(resultFile != null, "resultFile must be set if useResultFile is true");
                        writeResultFile(resultFile, classpathResults);
                    }

                    boolean failed = false;

                    for (Map.Entry<String, Entry<ResultCollector, ClasspathDescriptor>> classpathEntry : classpathResults.entrySet()) {
                        String classpathName = classpathEntry.getKey();
                        ResultCollector resultCollector = classpathEntry.getValue().getKey();

                        for (final ConflictState state : printState) {
                            for (final ConflictType type : ConflictType.values()) {
                                if (resultCollector.hasConflictsFor(type, state)) {
                                    final Map<String, Collection<ConflictResult>> results = resultCollector.getResults(type, state);
                                    for (final Map.Entry<String, Collection<ConflictResult>> entry : results.entrySet()) {
                                        final String artifactNames = entry.getKey();
                                        final Collection<ConflictResult> conflictResults = entry.getValue();

                                        LOG.warn("Found duplicate %s %s in [%s]:", state.getHint(), type.getType(), artifactNames);
                                        for (final ConflictResult conflictResult : conflictResults) {
                                            LOG.warn("  %s", conflictResult.getName());
                                        }
                                    }
                                }
                            }
                        }

                        failed |= resultCollector.isFailed();

                        if (resultCollector.isFailed()) {
                            LOG.warn("Found duplicate classes/resources in %s classpath.", classpathName);
                        }
                    }

                    if (failed) {
                        throw new MojoExecutionException("Found duplicate classes/resources!");
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

    private ImmutableSet<String> getIgnoredResourcePatterns()
    {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        builder.add(ignoredResourcePatterns);
        builder.add(ignoredResources);

        return builder.build();
    }

    /**
     * Checks the maven classpath for a given set of scopes whether it contains duplicates. In addition to the
     * artifacts on the classpath, one or more additional project folders are added.
     */
    private ClasspathDescriptor checkClasspath(final ResultCollector resultCollector,
                                               final ArtifactFileResolver artifactFileResolver,
                                               final Set<String> scopes,
                                               final Set<File> bootClasspath,
                                               final File ... projectFolders)
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
            getIgnoredResourcePatterns(),
            Arrays.asList(ignoredDependencies),
            useDefaultResourceIgnoreList,
            bootClasspath,
            projectFolders);

        // Now a scope specific classpath descriptor (scope relevant artifacts and project folders) and the global artifact resolver
        // are primed. Run conflict resolution for classes and resources.
        checkForDuplicates(CLASS, resultCollector, classpathDescriptor, artifactFileResolver);
        checkForDuplicates(RESOURCE, resultCollector, classpathDescriptor, artifactFileResolver);
        return classpathDescriptor;
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
            // a folder name for a project folder) as keys and a classpath element as value.
            final SortedSet<ClasspathElement> conflictingClasspathElements = artifactFileResolver.getClasspathElementsForElements(elements);

            ImmutableSet.Builder<Artifact> artifactBuilder = ImmutableSet.builder();

            boolean bootClasspathConflict = false;
            for (ClasspathElement conflictingClasspathElement : conflictingClasspathElements) {
                bootClasspathConflict |= conflictingClasspathElement.isBootClasspathElement();

                if (conflictingClasspathElement.hasArtifact()) {
                    artifactBuilder.add(conflictingClasspathElement.getArtifact());
                }
                else if (conflictingClasspathElement.isLocalFolder()) {
                    artifactBuilder.add(project.getArtifact());
                }
            }

            final boolean excepted = isExcepted(type, name, bootClasspathConflict, artifactBuilder.build());
            final ConflictState conflictState = DuplicateFinderMojo.determineConflictState(type, name, elements);

            resultCollector.addConflict(type, name, conflictingClasspathElements, excepted, conflictState);
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

    private boolean isExcepted(final ConflictType type, final String name, final boolean bootClasspathConflict, final Set<Artifact> artifacts) throws OverConstrainedVersionException
    {
        final ImmutableSet.Builder<ConflictingDependency> conflictBuilder = ImmutableSet.builder();
        checkState(conflictingDependencies != null, "conflictingDependencies is null");

        // Find all exception definitions from the configuration that match these artifacts.
        for (final ConflictingDependency conflictingDependency : conflictingDependencies) {
            if (conflictingDependency.isForArtifacts(bootClasspathConflict, artifacts)) {
                conflictBuilder.add(conflictingDependency);
            }
        }

        // If any of the possible candidates covers this class or resource, then the conflict is excepted.
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

    private void writeResultFile(File resultFile, ImmutableMap<String, Entry<ResultCollector, ClasspathDescriptor>> results)
        throws MojoExecutionException, InvalidVersionSpecificationException, OverConstrainedVersionException
    {
        File parent = resultFile.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs()) {
                throw new MojoExecutionException("Could not create parent folders for " + parent.getAbsolutePath());
            }
        }
        if (!parent.isDirectory() || !parent.canWrite()) {
            throw new MojoExecutionException("Can not create result file in " + parent.getAbsolutePath());
        }

        try {
            SMOutputFactory factory = new SMOutputFactory(XMLOutputFactory2.newFactory());
            SMOutputDocument resultDocument = factory.createOutputDocument(resultFile);
            resultDocument.setIndentation("\n" + Strings.repeat(" ", 64), 1, 4);

            SMOutputElement rootElement = resultDocument.addElement("duplicate-finder-result");
            XMLWriterUtils.addAttribute(rootElement, "version", SAVE_FILE_VERSION);

            XMLWriterUtils.addProjectInformation(rootElement, project);

            addConfiguration(rootElement);

            SMOutputElement resultsElement = rootElement.addElement("results");
            for (Map.Entry<String, Entry<ResultCollector, ClasspathDescriptor>> entry : results.entrySet()) {
                SMOutputElement resultElement = resultsElement.addElement("result");
                XMLWriterUtils.addAttribute(resultElement, "name", entry.getKey());
                XMLWriterUtils.addResultCollector(resultElement, entry.getValue().getKey());
                XMLWriterUtils.addClasspathDescriptor(resultElement, resultFileMinClasspathCount, entry.getValue().getValue());
            }

            resultDocument.closeRootAndWriter();
        }
        catch (XMLStreamException e) {
            throw new MojoExecutionException("While writing result file", e);
        }
    }

    private void addConfiguration(SMOutputElement rootElement)
        throws XMLStreamException, InvalidVersionSpecificationException
    {
        SMOutputElement prefs = XMLWriterUtils.addElement(rootElement, "configuration", null);
        // Simple configuration options
        XMLWriterUtils.addAttribute(prefs, "skip", skip);
        XMLWriterUtils.addAttribute(prefs, "quiet", quiet);
        XMLWriterUtils.addAttribute(prefs, "checkCompileClasspath", checkCompileClasspath);
        XMLWriterUtils.addAttribute(prefs, "checkRuntimeClasspath", checkRuntimeClasspath);
        XMLWriterUtils.addAttribute(prefs, "checkTestClasspath", checkTestClasspath);
        XMLWriterUtils.addAttribute(prefs, "failBuildInCaseOfDifferentContentConflict", failBuildInCaseOfDifferentContentConflict);
        XMLWriterUtils.addAttribute(prefs, "failBuildInCaseOfEqualContentConflict", failBuildInCaseOfEqualContentConflict);
        XMLWriterUtils.addAttribute(prefs, "failBuildInCaseOfConflict", failBuildInCaseOfConflict);
        XMLWriterUtils.addAttribute(prefs, "printEqualFiles", printEqualFiles);
        XMLWriterUtils.addAttribute(prefs, "preferLocal", preferLocal);
        XMLWriterUtils.addAttribute(prefs, "includeBootClasspath", includeBootClasspath);
        XMLWriterUtils.addAttribute(prefs, "bootClasspathProperty", bootClasspathProperty);
        // Ignoring Dependencies and resources
        XMLWriterUtils.addAttribute(prefs, "useDefaultResourceIgnoreList", useDefaultResourceIgnoreList);
        // Result file options
        XMLWriterUtils.addAttribute(prefs, "useResultFile", useResultFile);
        XMLWriterUtils.addAttribute(prefs, "resultFileMinClasspathCount", resultFileMinClasspathCount);
        XMLWriterUtils.addAttribute(prefs, "resultFile", resultFile.getAbsolutePath());

        SMOutputElement ignoredResourcesElement = prefs.addElement("ignoredResourcePatterns");
        for (String ignoredResource : getIgnoredResourcePatterns()) {
            XMLWriterUtils.addElement(ignoredResourcesElement, "ignoredResourcePattern", ignoredResource);
        }

        SMOutputElement conflictingDependenciesElement = prefs.addElement("conflictingDependencies");
        for (ConflictingDependency conflictingDependency : conflictingDependencies) {
            XMLWriterUtils.addConflictingDependency(conflictingDependenciesElement, "conflictingDependency", conflictingDependency);
        }

        SMOutputElement ignoredDependenciesElement = prefs.addElement("ignoredDependencies");
        for (MavenCoordinates ignoredDependency : ignoredDependencies) {
            XMLWriterUtils.addMavenCoordinate(ignoredDependenciesElement, "dependency", ignoredDependency);
        }
    }
}

