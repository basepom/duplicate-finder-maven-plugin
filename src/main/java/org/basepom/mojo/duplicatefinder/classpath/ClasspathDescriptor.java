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

package org.basepom.mojo.duplicatefinder.classpath;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

import org.basepom.mojo.duplicatefinder.ConflictType;
import org.basepom.mojo.duplicatefinder.artifact.MavenCoordinates;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.lang.model.SourceVersion;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.Files;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public final class ClasspathDescriptor {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathDescriptor.class);

    private static final MatchPatternPredicate DEFAULT_IGNORED_RESOURCES_PREDICATE = new MatchPatternPredicate(Arrays.asList(
            // Standard jar folders
            "^META-INF/.*",
            "^OSGI-INF/.*",
            // directory name that shows up all the time
            "^licenses/.*",
            // file names that show up all the time
            ".*license(\\.txt)?$",
            ".*notice(\\.txt)?$",
            ".*readme(\\.txt)?$",
            ".*changelog(\\.txt)?$",
            ".*third-party(\\.txt)?$",
            // HTML stuff from javadocs.
            ".*package\\.html$",
            ".*overview\\.html$"));

    @VisibleForTesting
    static final MatchPatternPredicate DEFAULT_IGNORED_CLASS_PREDICATE = new MatchPatternPredicate(Arrays.asList(

            "^(.*\\.)?.*\\$.*$",      // matches inner classes in any package
            "^(.*\\.)?package-info$", // matches package-info in any package
            "^(.*\\.)?module-info$"   // matches module-info in any package
    ));

    private static final MatchPatternPredicate DEFAULT_IGNORED_LOCAL_DIRECTORIES = new MatchPatternPredicate(Arrays.asList(
            "^.git$",
            "^.svn$",
            "^.hg$",
            "^.bzr$"));

    /**
     * This is a global, static cache which can be reused through multiple runs of the plugin in the same VM, e.g. for a multi-module build.
     */
    private static final ConcurrentMap<File, ClasspathCacheElement> CACHED_BY_FILE = new ConcurrentHashMap<>();

    private final Multimap<String, File> classesWithElements = MultimapBuilder.treeKeys().hashSetValues().build();
    private final Multimap<String, File> resourcesWithElements = MultimapBuilder.treeKeys().hashSetValues().build();

    private final Predicate<String> resourcesPredicate;
    private final Predicate<String> classPredicate;

    private final ImmutableList<Pattern> ignoredResourcePatterns;
    private final ImmutableList<Pattern> ignoredClassPatterns;

    public static ClasspathDescriptor createClasspathDescriptor(final MavenProject project,
            final Multimap<File, Artifact> fileToArtifactMap,
            final Collection<String> ignoredResourcePatterns,
            final Collection<String> ignoredClassPatterns,
            final Collection<MavenCoordinates> ignoredDependencies,
            final boolean useDefaultResourceIgnoreList,
            final boolean useDefaultClassIgnoreList,
            final File... projectFolders) throws MojoExecutionException {
        checkNotNull(project, "project is null");
        checkNotNull(fileToArtifactMap, "fileToArtifactMap is null");
        checkNotNull(ignoredResourcePatterns, "ignoredResourcePatterns is null");
        checkNotNull(ignoredClassPatterns, "ignoredClassPatterns is null");
        checkNotNull(ignoredDependencies, "ignoredDependencies is null");
        checkNotNull(projectFolders, "projectFolders is null");

        final ClasspathDescriptor classpathDescriptor = new ClasspathDescriptor(useDefaultResourceIgnoreList, ignoredResourcePatterns,
                useDefaultClassIgnoreList, ignoredClassPatterns);

        File file = null;

        final MatchArtifactPredicate matchArtifactPredicate = new MatchArtifactPredicate(ignoredDependencies);

        Artifact artifact = null;

        try {
            // any entry is either a jar in the repo or a folder in the target folder of a referenced
            // project. Add the elements that are not ignored by the ignoredDependencies predicate to
            // the classpath descriptor.
            for (final Entry<File, Artifact> entry : fileToArtifactMap.entries()) {
                artifact = entry.getValue();
                file = entry.getKey();

                if (file.exists()) {
                    // Add to the classpath if the artifact predicate does not apply (then it is not in the ignoredDependencies list).
                    if (!matchArtifactPredicate.apply(artifact)) {
                        classpathDescriptor.addClasspathElement(file);
                    }
                } else {
                    // e.g. when running the goal explicitly on a cleaned multi-module project, referenced
                    // projects will try to use the output folders of a referenced project but these do not
                    // exist. Obviously, in this case the plugin might return incorrect results (unfortunately
                    // false negatives, but there is not much it can do here (besides fail the build here with a
                    // cryptic error message. Maybe add a flag?).
                    LOG.debug(format("Classpath element '%s' does not exist.", file.getAbsolutePath()));
                }
            }
        } catch (final IOException ex) {
            throw new MojoExecutionException(format("Error trying to access file '%s' for artifact '%s'", file, artifact), ex);
        }

        try {
            // Add project folders unconditionally.
            for (final File projectFile : projectFolders) {
                file = projectFile;
                if (projectFile.exists()) {
                    classpathDescriptor.addClasspathElement(file);
                } else {
                    // See above. This may happen in the project has been cleaned before running the goal directly.
                    LOG.debug(format("Project folder '%s' does not exist.", file.getAbsolutePath()));
                }
            }
        } catch (final IOException ex) {
            throw new MojoExecutionException(format("Error trying to access project folder '%s'", file), ex);
        }

        return classpathDescriptor;
    }

    private ClasspathDescriptor(final boolean useDefaultResourceIgnoreList,
            final Collection<String> ignoredResourcePatterns,
            final boolean useDefaultClassIgnoreList,
            final Collection<String> ignoredClassPatterns)
            throws MojoExecutionException {
        final Builder<Pattern> ignoredResourcePatternsBuilder = ImmutableList.builder();

        // build resource predicate...
        Predicate<String> resourcesPredicate = s -> false;

        // predicate matching the default ignores
        if (useDefaultResourceIgnoreList) {
            resourcesPredicate = resourcesPredicate.or(DEFAULT_IGNORED_RESOURCES_PREDICATE);
            ignoredResourcePatternsBuilder.addAll(DEFAULT_IGNORED_RESOURCES_PREDICATE.getPatterns());
        }

        if (!ignoredResourcePatterns.isEmpty()) {
            try {
                // predicate matching the user ignores
                MatchPatternPredicate ignoredResourcesPredicate = new MatchPatternPredicate(ignoredResourcePatterns);
                resourcesPredicate = resourcesPredicate.or(ignoredResourcesPredicate);
                ignoredResourcePatternsBuilder.addAll(ignoredResourcesPredicate.getPatterns());
            } catch (final PatternSyntaxException pse) {
                throw new MojoExecutionException("Error compiling resourceIgnore pattern: " + pse.getMessage());
            }
        }

        this.resourcesPredicate = resourcesPredicate;
        this.ignoredResourcePatterns = ignoredResourcePatternsBuilder.build();

        final Builder<Pattern> ignoredClassPatternsBuilder = ImmutableList.builder();

        // build class predicate.
        Predicate<String> classPredicate = s -> false;

        // predicate matching the default ignores
        if (useDefaultClassIgnoreList) {
            classPredicate = classPredicate.or(DEFAULT_IGNORED_CLASS_PREDICATE);
            ignoredClassPatternsBuilder.addAll(DEFAULT_IGNORED_CLASS_PREDICATE.getPatterns());
        }

        if (!ignoredClassPatterns.isEmpty()) {
            try {
                // predicate matching the user ignores
                MatchPatternPredicate ignoredPackagePredicate = new MatchPatternPredicate(ignoredClassPatterns);
                classPredicate = classPredicate.or(ignoredPackagePredicate);
                ignoredClassPatternsBuilder.addAll(ignoredPackagePredicate.getPatterns());
            } catch (final PatternSyntaxException pse) {
                throw new MojoExecutionException("Error compiling classIgnore pattern: " + pse.getMessage());
            }
        }

        this.classPredicate = classPredicate;
        this.ignoredClassPatterns = ignoredClassPatternsBuilder.build();
    }

    public ImmutableMap<String, Collection<File>> getClasspathElementLocations(final ConflictType type) {
        checkNotNull(type, "type is null");
        switch (type) {
            case CLASS:
                return ImmutableMultimap.copyOf(classesWithElements).asMap();
            case RESOURCE:
                return ImmutableMultimap.copyOf(resourcesWithElements).asMap();
            default:
                throw new IllegalStateException("Type '" + type + "' unknown!");
        }
    }

    public ImmutableList<Pattern> getIgnoredResourcePatterns() {
        return ignoredResourcePatterns;
    }

    public ImmutableList<Pattern> getIgnoredClassPatterns() {
        return ignoredClassPatterns;
    }

    public ImmutableList<Pattern> getIgnoredDirectoryPatterns() {
        return DEFAULT_IGNORED_LOCAL_DIRECTORIES.getPatterns();
    }


    private void addClasspathElement(final File element) throws IOException {
        checkState(element.exists(), "Path '%s' does not exist!", element.getAbsolutePath());

        ClasspathCacheElement cached = CACHED_BY_FILE.get(element);

        if (cached == null) {
            final ClasspathCacheElement.Builder cacheBuilder = ClasspathCacheElement.builder(element);
            if (element.isDirectory()) {
                addDirectory(cacheBuilder, element, new PackageNameHolder());
            } else {
                addArchive(cacheBuilder, element);
            }
            final ClasspathCacheElement newCached = cacheBuilder.build();
            final ClasspathCacheElement oldCached = CACHED_BY_FILE.putIfAbsent(element, newCached);
            cached = MoreObjects.firstNonNull(oldCached, newCached);
        } else {
            LOG.debug(format("Cache hit for '%s'", element.getAbsolutePath()));
        }

        cached.putResources(resourcesWithElements, resourcesPredicate);
        cached.putClasses(classesWithElements, classPredicate);

    }

    private void addDirectory(final ClasspathCacheElement.Builder cacheBuilder, final File workDir, final PackageNameHolder packageName) {
        final File[] files = workDir.listFiles();

        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    if (DEFAULT_IGNORED_LOCAL_DIRECTORIES.apply(file.getName())) {
                        LOG.debug(format("Ignoring local directory '%s'", file.getAbsolutePath()));
                    } else {
                        addDirectory(cacheBuilder, file, packageName.getChildPackage(file.getName()));
                    }

                } else if (file.isFile()) {
                    if ("class".equals(Files.getFileExtension(file.getName()))) {
                        final String className = packageName.getQualifiedName(Files.getNameWithoutExtension(file.getName()));
                        cacheBuilder.addClass(className);
                    } else {
                        final String resourcePath = packageName.getQualifiedPath(file.getName());
                        cacheBuilder.addResource(resourcePath);
                    }
                } else {
                    LOG.warn(format("Ignoring unknown file type for '%s'", file.getAbsolutePath()));
                }
            }
        }
    }

    private void addArchive(final ClasspathCacheElement.Builder cacheBuilder, final File element) throws IOException {

        try (
                InputStream input = element.toURI().toURL().openStream();
                ZipInputStream zipInput = new ZipInputStream(input)) {

            ZipEntry entry;

            while ((entry = zipInput.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    final String name = entry.getName();
                    Optional<List<String>> validatedElements = validateClassName(name);
                    if (validatedElements.isPresent()) {
                        List<String> nameElements = validatedElements.get();
                        final PackageNameHolder packageName = new PackageNameHolder(nameElements.subList(0, nameElements.size() - 1));
                        final String className = packageName.getQualifiedName(Files.getNameWithoutExtension(name));
                        cacheBuilder.addClass(className);
                    } else {
                        final String resourcePath = name.replace('\\', File.separatorChar);
                        cacheBuilder.addResource(resourcePath);
                    }
                }
            }
        }
    }

    @VisibleForTesting
    static Optional<List<String>> validateClassName(String fullClassPath) {
        if (fullClassPath == null) {
            return Optional.empty();
        }

        // ZIP/Jars always use "/" as separators
        final List<String> nameElements = ImmutableList.copyOf(Splitter.on("/").splitToList(fullClassPath));
        if (nameElements.isEmpty()) {
            LOG.warn(format("ZIP entry '%s' split into empty list!", fullClassPath));
            return Optional.empty();
        }
        String classFileName = nameElements.get(nameElements.size() - 1);

        if (!"class".equals(Files.getFileExtension(classFileName))) {
            LOG.debug(format("Ignoring %s, %s is not a class file", fullClassPath, classFileName));
            return Optional.empty();
        }
        for (int i = 0; i < nameElements.size() - 1; i++) {
            if (!SourceVersion.isIdentifier(nameElements.get(i))) {
                LOG.debug(format("Ignoring %s, %s is not a valid package element", fullClassPath, nameElements.get(i)));
                return Optional.empty();
            }
        }

        String className = Files.getNameWithoutExtension(classFileName);
        if (!(SourceVersion.isIdentifier(className)
                || "module-info".equals(className)
                || "package-info".equals(className))) {
            LOG.debug(format("Ignoring %s, %s is not a valid class identifier", fullClassPath, className));
            return Optional.empty();
        }

        return Optional.of(nameElements);
    }
}
