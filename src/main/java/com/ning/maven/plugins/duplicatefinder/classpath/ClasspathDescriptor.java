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
package com.ning.maven.plugins.duplicatefinder.classpath;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import com.ning.maven.plugins.duplicatefinder.PluginLog;

import org.apache.maven.plugin.MojoExecutionException;

public class ClasspathDescriptor
{
    private static final PluginLog LOG = new PluginLog(ClasspathDescriptor.class);

    private static final Predicate<String> DEFAULT_IGNORED_RESOURCES_PREDICATE = new MatchPatternPredicate(Arrays.asList(
        // Standard jar folders
        "^META-INF/.*",
        "^OSGI-INF/.*",
        // directory name that shows up all the time
        "^licenses/.*",
        // file names that show up all the time
        ".*license(\\.txt)?$",
        ".*notice(\\.txt)?$",
        ".*readme(\\.txt)?$",
        ".*third-party(\\.txt)?$",
        // HTML stuff from javadocs.
        ".*package\\.html$",
        ".*overview\\.html$"
    ));

    private static final Predicate<String> DEFAULT_IGNORED_LOCAL_DIRECTORIES = new MatchPatternPredicate(Arrays.asList(
        "^.git$",
        "^.svn$",
        "^.hg$",
        "^.bzr$"
    ));

    /**
     * This is a global, static cache which can be reused through multiple runs of the plugin in the same VM,
     * e.g. for a multi-module build.
     */
    private static final ConcurrentMap<File, ClasspathCacheElement> CACHED_BY_FILE = Maps.newConcurrentMap();

    private final Multimap<String, File> classesWithElements = MultimapBuilder.treeKeys().hashSetValues().build();
    private final Multimap<String, File> resourcesWithElements = MultimapBuilder.treeKeys().hashSetValues().build();

    private final Predicate<String> resourcesPredicate;
    private final Predicate<String> classPredicate;

    public ClasspathDescriptor(boolean useDefaultResourceIgnoreList, Collection<String> ignoredResources)
        throws MojoExecutionException
    {
        this.classPredicate = new MatchInnerClassesPredicate();
        Predicate<String> resourcesPredicate = Predicates.alwaysFalse();

        // predicate matching the default ignores
        if (useDefaultResourceIgnoreList) {
            resourcesPredicate = Predicates.or(resourcesPredicate, DEFAULT_IGNORED_RESOURCES_PREDICATE);
        }

        if (!ignoredResources.isEmpty()) {
            try {
                // predicate matching the user ignores
                resourcesPredicate = Predicates.or(resourcesPredicate, new MatchPatternPredicate(ignoredResources));
            }
            catch (final PatternSyntaxException pse) {
                throw new MojoExecutionException("Error compiling resourceIgnore pattern: " + pse.getMessage());
            }
        }

        this.resourcesPredicate = resourcesPredicate;
    }

    public void add(final File element) throws IOException
    {
        checkState(element.exists(), "Path '%s' does not exist!", element.getAbsolutePath());

        ClasspathCacheElement cached = CACHED_BY_FILE.get(element);

        if (cached == null) {
            final ClasspathCacheElement.Builder cacheBuilder = ClasspathCacheElement.builder(element);
            if (element.isDirectory()) {
                addDirectory(cacheBuilder, element, new PackageNameHolder());
            }
            else {
                addArchive(cacheBuilder, element);
            }
            ClasspathCacheElement newCached = cacheBuilder.build();
            ClasspathCacheElement oldCached = CACHED_BY_FILE.putIfAbsent(element, newCached);
            cached = MoreObjects.firstNonNull(oldCached, newCached);
        }
        else {
            LOG.debug("Cache hit for '%s'", element.getAbsolutePath());
        }

        cached.putResources(resourcesWithElements, resourcesPredicate);
        cached.putClasses(classesWithElements, classPredicate);

    }

    public ImmutableMap<String, Collection<File>> getClasses()
    {
        return ImmutableMultimap.copyOf(classesWithElements).asMap();
    }

    public ImmutableMap<String, Collection<File>> getResources()
    {
        return ImmutableMultimap.copyOf(resourcesWithElements).asMap();
    }

    private void addDirectory(final ClasspathCacheElement.Builder cacheBuilder, final File workDir, final PackageNameHolder packageName)
    {
        final File[] files = workDir.listFiles();

        if (files != null) {
            for (File file : Arrays.asList(files)) {
                if (file.isDirectory()) {
                    if (DEFAULT_IGNORED_LOCAL_DIRECTORIES.apply(file.getName())) {
                        LOG.debug("Ignoring local directory '%s'", file.getAbsolutePath());
                    }
                    else {
                        addDirectory(cacheBuilder, file, packageName.getChildPackage(file.getName()));
                    }

                }
                else if (file.isFile()) {
                    if ("class".equals(Files.getFileExtension(file.getName()))) {
                        final String className = packageName.getQualifiedName(Files.getNameWithoutExtension(file.getName()));
                        cacheBuilder.addClass(className);
                    }
                    else {
                        final String resourcePath = packageName.getQualifiedPath(file.getName());
                        cacheBuilder.addResource(resourcePath);
                    }
                }
                else {
                    LOG.warn("Ignoring unknown file type for '%s'", file.getAbsolutePath());
                }
            }
        }
    }

    private void addArchive(final ClasspathCacheElement.Builder cacheBuilder, final File element) throws IOException
    {
        Closer closer = Closer.create();

        try {
            InputStream input = closer.register(element.toURI().toURL().openStream());
            ZipInputStream zipInput = closer.register(new ZipInputStream(input));

            ZipEntry entry;

            while ((entry = zipInput.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    final String name = entry.getName();
                    if ("class".equals(Files.getFileExtension(name))) {
                        final List<String> nameElements = Splitter.on("/").splitToList(name); // ZIP/Jars always use "/" as separators
                        if (nameElements.isEmpty()) {
                            LOG.warn("ZIP entry '%s' split into empty list!", entry);
                        }
                        else {
                            final PackageNameHolder packageName = new PackageNameHolder(nameElements.subList(0, nameElements.size() - 1));
                            final String className = packageName.getQualifiedName(Files.getNameWithoutExtension(name));
                            cacheBuilder.addClass(className);
                        }
                    }
                    else {
                        final String resourcePath = name.replace('\\', File.separatorChar);
                        cacheBuilder.addResource(resourcePath);
                    }
                }
            }
        }
        finally {
            closer.close();
        }
    }
}
