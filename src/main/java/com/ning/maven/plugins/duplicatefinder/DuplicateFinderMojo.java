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

import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_PROVIDED;
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_SYSTEM;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.collect.ImmutableSet;
import com.pyx4j.log4j.MavenLogAppender;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds duplicate classes/resources.
 */
@Mojo(name = "check",
                requiresProject = true,
                threadSafe = true,
                defaultPhase = LifecyclePhase.VERIFY,
                requiresDependencyResolution = ResolutionScope.TEST)
public class DuplicateFinderMojo extends AbstractMojo
{
    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    // the constants for conflicts
    private static final int NO_CONFLICT = 0;
    private static final int CONFLICT_CONTENT_EQUAL = 1;
    private static final int CONFLICT_CONTENT_DIFFERENT = 2;

    private static final Set<String> COMPILE_SCOPE = ImmutableSet.of(SCOPE_COMPILE, SCOPE_PROVIDED, SCOPE_SYSTEM);
    private static final Set<String> RUNTIME_SCOPE = ImmutableSet.of(SCOPE_COMPILE, SCOPE_RUNTIME);
    private static final Set<String> TEST_SCOPE = ImmutableSet.of("test");

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
    protected String[] ignoredResources;

    /**
     * Artifacts with expected and resolved versions that are checked.
     */
    @Parameter
    protected Exception[] exceptions;

    /**
     * Dependencies that should not be checked at all.
     */
    @Parameter(property = "ignoredDependencies")
    protected DependencyWrapper[] ignoredDependencies;

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

    public void setIgnoredDependencies(final Dependency[] ignoredDependencies) throws InvalidVersionSpecificationException
    {
        this.ignoredDependencies = new DependencyWrapper[ignoredDependencies.length];
        for (int idx = 0; idx < ignoredDependencies.length; idx++) {
            this.ignoredDependencies[idx] = new DependencyWrapper(ignoredDependencies[idx]);
        }
    }

    @Override
    public void execute() throws MojoExecutionException
    {
        MavenLogAppender.startPluginLog(this);

        try {
            if (skip) {
                LOG.debug("Skipping execution!");
            }
            else {
                if (checkCompileClasspath) {
                    checkCompileClasspath();
                }
                if (checkRuntimeClasspath) {
                    checkRuntimeClasspath();
                }
                if (checkTestClasspath) {
                    checkTestClasspath();
                }
            }
        }
        finally {
            MavenLogAppender.endPluginLog(this);
        }
    }

    private void checkCompileClasspath() throws MojoExecutionException
    {
        try {
            LOG.info("Checking compile classpath");

            final Set<Artifact> allArtifacts = project.getArtifacts();
            final ImmutableSet.Builder<Artifact> inScopeBuilder = ImmutableSet.builder();
            for (final Artifact artifact : allArtifacts) {
                if (artifact.getArtifactHandler().isAddedToClasspath() && COMPILE_SCOPE.contains(artifact.getScope())) {
                    inScopeBuilder.add(artifact);
                }
            }

            final Map<File, Artifact> artifactsByFile = createArtifactsByFileMap(inScopeBuilder.build());

            addOutputDirectory(artifactsByFile);
            checkClasspath(project.getCompileClasspathElements(), artifactsByFile);
        }
        catch (final DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Could not resolve dependencies", ex);
        }
    }

    private void checkRuntimeClasspath() throws MojoExecutionException
    {
        try {
            LOG.info("Checking runtime classpath");

            final Set<Artifact> allArtifacts = project.getArtifacts();
            final ImmutableSet.Builder<Artifact> inScopeBuilder = ImmutableSet.builder();
            for (final Artifact artifact : allArtifacts) {
                if (artifact.getArtifactHandler().isAddedToClasspath() && RUNTIME_SCOPE.contains(artifact.getScope())) {
                    inScopeBuilder.add(artifact);
                }
            }

            final Map<File, Artifact> artifactsByFile = createArtifactsByFileMap(inScopeBuilder.build());

            addOutputDirectory(artifactsByFile);
            checkClasspath(project.getRuntimeClasspathElements(), artifactsByFile);
        }
        catch (final DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Could not resolve dependencies", ex);
        }
    }

    private void checkTestClasspath() throws MojoExecutionException
    {
        try {
            LOG.info("Checking test classpath");

            final Set<Artifact> allArtifacts = project.getArtifacts();
            final ImmutableSet.Builder<Artifact> inScopeBuilder = ImmutableSet.builder();
            for (final Artifact artifact : allArtifacts) {
                if (artifact.getArtifactHandler().isAddedToClasspath()) {
                    inScopeBuilder.add(artifact);
                }
            }

            final Map<File, Artifact> artifactsByFile = createArtifactsByFileMap(inScopeBuilder.build());

            addOutputDirectory(artifactsByFile);
            addTestOutputDirectory(artifactsByFile);
            checkClasspath(project.getTestClasspathElements(), artifactsByFile);
        }
        catch (final DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Could not resolve dependencies", ex);
        }
    }

    private void checkClasspath(final List<String> classpathElements, final Map<File, Artifact> artifactsByFile) throws MojoExecutionException
    {
        final ClasspathDescriptor classpathDesc = createClasspathDescriptor(classpathElements);

        final int foundDuplicateClassesConflict = checkForDuplicateClasses(classpathDesc, artifactsByFile);
        final int foundDuplicateResourcesConflict = checkForDuplicateResources(classpathDesc, artifactsByFile);
        final int maxConflict = Math.max(foundDuplicateClassesConflict, foundDuplicateResourcesConflict);

        if (failBuildInCaseOfConflict && maxConflict > NO_CONFLICT ||
            failBuildInCaseOfDifferentContentConflict && maxConflict == CONFLICT_CONTENT_DIFFERENT ||
            failBuildInCaseOfEqualContentConflict && maxConflict >= CONFLICT_CONTENT_EQUAL) {
            throw new MojoExecutionException("Found duplicate classes/resources");
        }
    }

    private int checkForDuplicateClasses(final ClasspathDescriptor classpathDesc, final Map<File, Artifact> artifactsByFile) throws MojoExecutionException
    {
        final Map<String, List<String>> classDifferentConflictsByArtifactNames = new TreeMap<String, List<String>>(new ToStringComparator());
        final Map<String, List<String>> classEqualConflictsByArtifactNames = new TreeMap<String, List<String>>(new ToStringComparator());

        for (final String className : classpathDesc.getClasss()) {
            final Set<File> elements = classpathDesc.getElementsHavingClass(className);

            if (elements.size() > 1) {
                final Set<Artifact> artifacts = getArtifactsForElements(elements, artifactsByFile);

                filterIgnoredDependencies(artifacts);
                if (artifacts.size() < 2 || isExceptedClass(className, artifacts)) {
                    continue;
                }

                Map<String, List<String>> conflictsByArtifactNames;

                if (isAllElementsAreEqual(elements, className.replace('.', '/') + ".class"))
                {
                    conflictsByArtifactNames = classEqualConflictsByArtifactNames;
                }
                else {
                    conflictsByArtifactNames = classDifferentConflictsByArtifactNames;
                }

                final String artifactNames = getArtifactsToString(artifacts);
                List<String> classNames = conflictsByArtifactNames.get(artifactNames);

                if (classNames == null) {
                    classNames = new ArrayList<String>();
                    conflictsByArtifactNames.put(artifactNames, classNames);
                }
                classNames.add(className);
            }
        }

        int conflict = NO_CONFLICT;

        if (!classEqualConflictsByArtifactNames.isEmpty()) {
            if (printEqualFiles ||
                failBuildInCaseOfConflict ||
                failBuildInCaseOfEqualContentConflict) {

                printWarningMessage(classEqualConflictsByArtifactNames, "(but equal)", "classes");
            }

            conflict = CONFLICT_CONTENT_EQUAL;
        }

        if (!classDifferentConflictsByArtifactNames.isEmpty()) {
            printWarningMessage(classDifferentConflictsByArtifactNames, "and different", "classes");

            conflict = CONFLICT_CONTENT_DIFFERENT;
        }

        return conflict;
    }

    private int checkForDuplicateResources(final ClasspathDescriptor classpathDesc, final Map<File, Artifact> artifactsByFile) throws MojoExecutionException
    {
        final Map<String, List<String>> resourceDifferentConflictsByArtifactNames = new TreeMap<String, List<String>>(new ToStringComparator());
        final Map<String, List<String>> resourceEqualConflictsByArtifactNames = new TreeMap<String, List<String>>(new ToStringComparator());

        for (final String resource : classpathDesc.getResources()) {
            final Set<File> elements = classpathDesc.getElementsHavingResource(resource);

            if (elements.size() > 1) {
                final Set<Artifact> artifacts = getArtifactsForElements(elements, artifactsByFile);

                filterIgnoredDependencies(artifacts);
                if (artifacts.size() < 2 || isExceptedResource(resource, artifacts)) {
                    continue;
                }

                Map<String, List<String>> conflictsByArtifactNames;

                if (isAllElementsAreEqual(elements, resource)) {
                    conflictsByArtifactNames = resourceEqualConflictsByArtifactNames;
                }
                else {
                    conflictsByArtifactNames = resourceDifferentConflictsByArtifactNames;
                }

                final String artifactNames = getArtifactsToString(artifacts);
                List<String> resources = conflictsByArtifactNames.get(artifactNames);

                if (resources == null) {
                    resources = new ArrayList<String>();
                    conflictsByArtifactNames.put(artifactNames, resources);
                }
                resources.add(resource);
            }
        }

        int conflict = NO_CONFLICT;

        if (!resourceEqualConflictsByArtifactNames.isEmpty()) {
            if (printEqualFiles ||
                failBuildInCaseOfConflict ||
                failBuildInCaseOfEqualContentConflict) {

                printWarningMessage(resourceEqualConflictsByArtifactNames, "(but equal)", "resources");
            }

            conflict = CONFLICT_CONTENT_EQUAL;
        }

        if (!resourceDifferentConflictsByArtifactNames.isEmpty()) {
            printWarningMessage(resourceDifferentConflictsByArtifactNames, "and different", "resources");

            conflict = CONFLICT_CONTENT_DIFFERENT;
        }

        return conflict;
    }

    /**
     * Prints the conflict messages.
     *
     * @param conflictsByArtifactNames the Map of conflicts (Artifactnames, List of classes)
     * @param hint hint with the type of the conflict ("all equal" or "content different")
     * @param type type of conflict (class or resource)
     */
    private void printWarningMessage(final Map<String, List<String>> conflictsByArtifactNames, final String hint, final String type)
    {
        for (final Map.Entry<String, List<String>> entry : conflictsByArtifactNames.entrySet()) {
            final String artifactNames = entry.getKey();
            final List<String> classNames = entry.getValue();

            LOG.warn("Found duplicate " + hint + " " + type + " in " + artifactNames + " :");
            for (String className : classNames) {
                LOG.warn("  " + className);
            }
        }
    }

    /**
     * Detects class/resource differences via SHA256 hash comparsion.
     *
     * @param resourcePath the class or resource path that has duplicates in classpath
     * @param elements the files contains the duplicates
     * @return true if all classes are "byte equal" and false if any class differ
     */
    private boolean isAllElementsAreEqual(final Set<File> elements, final String resourcePath)
    {
        File firstFile = null;
        String firstSHA256 = null;

        for (File element : elements)
        {
            try {
                final String newSHA256 = getSHA256HexOfElement(element, resourcePath);

                if (firstSHA256 == null) {
                    // save sha256 hash from the first element
                    firstSHA256 = newSHA256;
                    firstFile = element;
                }
                else if (!newSHA256.equals(firstSHA256)) {
                    LOG.debug("Found different SHA256 hashs for elements " + resourcePath + " in file " + firstFile + " and " + element);
                    return false;
                }
            }
            catch (final IOException ex) {
                LOG.warn("Could not read content from file " + element + "!", ex);
            }
        }

        return true;
    }

    /**
     * Calculates the SHA256 Hash of a class in a file.
     *
     * @param file the archive contains the class
     * @param resourcePath the name of the class
     * @return the MD% Hash as Hex-Value
     * @throws IOException if any error occurs on reading class in archive
     */
    private String getSHA256HexOfElement(final File file, final String resourcePath) throws IOException
    {
        ZipFile zip = null;
        InputStream in;

        if (file.isDirectory()) {
            final File resourceFile = new File(file, resourcePath);
            in = new BufferedInputStream(new FileInputStream(resourceFile));
        }
        else {
            zip = new ZipFile(file);
            final ZipEntry zipEntry = zip.getEntry(resourcePath);

            if (zipEntry == null) {
                throw new IOException("Could not find " + resourcePath + " in archive " + file);
            }
            in = zip.getInputStream(zipEntry);
        }

        try {
            return DigestUtils.sha256Hex(in);
        }
        finally {
            IOUtils.closeQuietly(in);
            if (zip != null) {
                try {
                    zip.close();
                }
                catch (final IOException ioe) {
                    // swallow exception
                }
            }
        }
    }

    private void filterIgnoredDependencies(final Set<Artifact> artifacts)
    {
        if (ignoredDependencies != null) {
            for (int idx = 0; idx < ignoredDependencies.length; idx++) {
                for (final Iterator artifactIt = artifacts.iterator(); artifactIt.hasNext();) {
                    final Artifact artifact = (Artifact) artifactIt.next();

                    if (ignoredDependencies[idx].matches(artifact)) {
                        artifactIt.remove();
                    }
                }
            }
        }
    }

    private boolean isExceptedClass(final String className, final Collection<Artifact> artifacts)
    {
        final List exceptions = getExceptionsFor(artifacts);

        for (final Iterator it = exceptions.iterator(); it.hasNext();) {
            final Exception exception = (Exception) it.next();

            if (exception.containsClass(className)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExceptedResource(final String resource, final Collection<Artifact> artifacts)
    {
        final List<Exception> exceptions = getExceptionsFor(artifacts);

        for (Exception exception : exceptions) {
            if (exception.containsResource(resource)) {
                return true;
            }
        }
        return false;
    }

    private List<Exception> getExceptionsFor(final Collection<Artifact> artifacts)
    {
        final List<Exception> result = new ArrayList<Exception>();

        if (exceptions != null) {
            for (int idx = 0; idx < exceptions.length; idx++) {
                if (exceptions[idx].isForArtifacts(artifacts, project.getArtifact())) {
                    result.add(exceptions[idx]);
                }
            }
        }
        return result;
    }

    private Set<Artifact> getArtifactsForElements(final Collection<File> elements, final Map<File, Artifact> artifactsByFile)
    {
        final Set<Artifact> artifacts = new TreeSet<Artifact>();

        for (final File element : elements) {
            Artifact artifact = artifactsByFile.get(element);

            if (artifact == null) {
                artifact = project.getArtifact();
            }
            artifacts.add(artifact);
        }
        return artifacts;
    }

    private String getArtifactsToString(final Collection<Artifact> artifacts)
    {
        final StringBuffer result = new StringBuffer();

        result.append("[");
        for (final Iterator<Artifact> it = artifacts.iterator(); it.hasNext();) {
            if (result.length() > 1) {
                result.append(",");
            }
            result.append(getQualifiedName(it.next()));
        }
        result.append("]");
        return result.toString();
    }

    private ClasspathDescriptor createClasspathDescriptor(final List<String> classpathElements) throws MojoExecutionException
    {
        final ClasspathDescriptor classpathDesc = new ClasspathDescriptor();

        classpathDesc.setUseDefaultResourceIgnoreList(useDefaultResourceIgnoreList);
        classpathDesc.setIgnoredResources(ignoredResources);

        for (final String element : classpathElements) {

            try {
                classpathDesc.add(new File(element));
            }
            catch (final FileNotFoundException ex) {
                LOG.debug("Could not access classpath element " + element);
            }
            catch (final IOException ex) {
                throw new MojoExecutionException("Error trying to access element " + element, ex);
            }
        }
        return classpathDesc;
    }

    private Map<File, Artifact> createArtifactsByFileMap(final Collection<Artifact> artifacts) throws DependencyResolutionRequiredException
    {
        final Map<File, Artifact> artifactsByFile = new HashMap<File, Artifact>(artifacts.size());

        for (final Artifact artifact : artifacts) {
            final File localPath = getLocalProjectPath(artifact);
            final File repoPath = artifact.getFile();

            if (localPath == null && repoPath == null) {
                throw new DependencyResolutionRequiredException(artifact);
            }
            if (localPath != null) {
                artifactsByFile.put(localPath, artifact);
            }
            if (repoPath != null) {
                artifactsByFile.put(repoPath, artifact);
            }
        }
        return artifactsByFile;
    }

    private File getLocalProjectPath(final Artifact artifact) throws DependencyResolutionRequiredException
    {
        final String refId = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
        final MavenProject owningProject = project.getProjectReferences().get(refId);

        if (owningProject != null) {
            if (artifact.getType().equals("test-jar")) {
                final File testOutputDir = new File(owningProject.getBuild().getTestOutputDirectory());

                if (testOutputDir.exists()) {
                    return testOutputDir;
                }
            }
            else {
                return new File(project.getBuild().getOutputDirectory());
            }
        }
        return null;
    }

    private void addOutputDirectory(final Map<File, Artifact> artifactsByFile)
    {
        final File outputDir = new File(project.getBuild().getOutputDirectory());

        if (outputDir.exists()) {
            artifactsByFile.put(outputDir, null);
        }
    }

    private void addTestOutputDirectory(final Map<File, Artifact> artifactsByFile)
    {
        final File outputDir = new File(project.getBuild().getOutputDirectory());

        if (outputDir.exists()) {
            artifactsByFile.put(outputDir, null);
        }
    }

    private String getQualifiedName(final Artifact artifact)
    {
        String result = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();

        if (artifact.getType() != null && !"jar".equals(artifact.getType())) {
            result = result + ":" + artifact.getType();
        }
        if (artifact.getClassifier() != null && (!"tests".equals(artifact.getClassifier()) || !"test-jar".equals(artifact.getType()))) {
            result = result + ":" + artifact.getClassifier();
        }
        return result;
    }
}
