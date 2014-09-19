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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.io.Closer;
import com.google.common.io.Files;

import org.apache.maven.plugin.MojoExecutionException;

public class ClasspathDescriptor
{
    private static final Pattern[] DEFAULT_IGNORED_RESOURCES = {
        Pattern.compile("META-INF/.*"),
        Pattern.compile("OSGI-INF/.*"),
        Pattern.compile("README(\\.TXT)?"),
        Pattern.compile(".*PACKAGE\\.HTML"),
        Pattern.compile(".*OVERVIEW\\.HTML")
    };

    private static final ImmutableSet<String> IGNORED_LOCAL_DIRECTORIES;

    static {
        final ImmutableSet.Builder<String> ignoredLocalDirectoryBuilder = ImmutableSet.builder();

        ignoredLocalDirectoryBuilder.add(".GIT");
        ignoredLocalDirectoryBuilder.add(".SVN");
        ignoredLocalDirectoryBuilder.add(".HG");
        ignoredLocalDirectoryBuilder.add(".BZR");

        IGNORED_LOCAL_DIRECTORIES = ignoredLocalDirectoryBuilder.build();
    }

    private final Map<File, Cached> cachedByFile = Maps.newHashMap();
    private final Multimap<String, File> classesWithElements = MultimapBuilder.treeKeys().hashSetValues().build();
    private final Multimap<String, File> resourcesWithElements = MultimapBuilder.treeKeys().hashSetValues().build();

    private boolean useDefaultResourceIgnoreList = true;

    private volatile Optional<Pattern[]> ignoredResourcesPatterns = Optional.absent();

    public boolean isUseDefaultResourceIgnoreList()
    {
        return useDefaultResourceIgnoreList;
    }

    public void setUseDefaultResourceIgnoreList(final boolean useDefaultResourceIgnoreList)
    {
        this.useDefaultResourceIgnoreList = useDefaultResourceIgnoreList;
    }

    public void setIgnoredResources(final String[] ignoredResources) throws MojoExecutionException
    {
        checkNotNull(ignoredResources, "ignoredResources is null");
        Pattern [] patterns = new Pattern[ignoredResources.length];

        try {
            for (int i = 0; i < ignoredResources.length; i++) {
                patterns[i] = Pattern.compile(ignoredResources[i].toUpperCase());
            }
            this.ignoredResourcesPatterns = Optional.of(patterns);
        }
        catch (final PatternSyntaxException pse) {
            throw new MojoExecutionException("Error compiling resourceIgnore pattern: " + pse.getMessage());
        }
    }

    public void add(final File element) throws IOException
    {
        if (!element.exists()) {
            throw new FileNotFoundException(format("Path '%s' does not exist!", element.getAbsolutePath()));
        }

        if (element.isDirectory()) {
            addDirectory(element);
        }
        else {
            addArchive(element);
        }
    }

    public Set<String> getClasss()
    {
        return ImmutableSet.copyOf(classesWithElements.keySet());
    }

    public Set<String> getResources()
    {
        return ImmutableSet.copyOf(resourcesWithElements.keySet());
    }

    public ImmutableSet<File> getElementsHavingClass(final String className)
    {
        final Collection<File> files =  classesWithElements.get(className);
        return files != null ? ImmutableSet.copyOf(files) : ImmutableSet.<File>of();
    }

    public ImmutableSet<File> getElementsHavingResource(final String resource)
    {
        final Collection<File> files =  resourcesWithElements.get(resource);
        return files != null ? ImmutableSet.copyOf(files) : ImmutableSet.<File>of();
    }

    private void addDirectory(final File element)
    {
        addDirectory(element, null, element);
    }

    private void addDirectory(final File element, final String parentPackageName, final File directory)
    {
        if (addCached(element)) {
            return;
        }

        final ImmutableList.Builder<String> classesBuilder = ImmutableList.builder();
        final ImmutableList.Builder<String> resourcesBuilder = ImmutableList.builder();
        final File[] files = directory.listFiles();
        final String pckgName = element.equals(directory) ? null : (parentPackageName == null ? "" : parentPackageName + ".") + directory.getName();

        if (files != null && files.length > 0) {
            for (int idx = 0; idx < files.length; idx++) {
                if (files[idx].isDirectory() && !IGNORED_LOCAL_DIRECTORIES.contains(files[idx].getName().toUpperCase())) {
                    addDirectory(element, pckgName, files[idx]);
                }
                else if (files[idx].isFile()) {
                    if ("class".equals(Files.getFileExtension(files[idx].getName()))) {
                        final String className = (pckgName == null ? "" : pckgName + ".") + Files.getNameWithoutExtension(files[idx].getName());

                        classesBuilder.add(className);
                        addClass(className, element);
                    }
                    else {
                        final String resourcePath = (pckgName == null ? "" : pckgName.replace('.', '/') + "/") + files[idx].getName();

                        resourcesBuilder.add(resourcePath);
                        addResource(resourcePath, element);
                    }
                }
            }
        }

        cachedByFile.put(element, new Cached(classesBuilder.build(), resourcesBuilder.build()));
    }

    private void addArchive(final File element) throws IOException
    {
        if (addCached(element)) {
            return;
        }

        final ImmutableList.Builder<String> classesBuilder = ImmutableList.builder();
        final ImmutableList.Builder<String> resourcesBuilder = ImmutableList.builder();

        Closer closer = Closer.create();

        try {
            InputStream input = closer.register(element.toURI().toURL().openStream());
            ZipInputStream zipInput = closer.register(new ZipInputStream(input));

            ZipEntry entry;

            while ((entry = zipInput.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    final String name = entry.getName();

                    if ("class".equals(Files.getFileExtension(name))) {
                        final File classFile = new File(new File(name).getParent(), Files.getNameWithoutExtension(name));
                        final String className = classFile.getPath().replace('/', '.').replace('\\', '.');

                        classesBuilder.add(className);
                        addClass(className, element);
                    }
                    else {
                        final String resourcePath = name.replace('\\', File.separatorChar);

                        resourcesBuilder.add(resourcePath);
                        addResource(resourcePath, element);
                    }
                }
            }

            cachedByFile.put(element, new Cached(classesBuilder.build(), resourcesBuilder.build()));
        }
        finally {
            closer.close();
        }
    }

    private void addClass(String className, File element)
    {
        if (className.indexOf('$') < 0) {
            classesWithElements.put(className, element);
        }
    }

    private void addResource(String resourcePath, File element)
    {
        if (!ignore(resourcePath)) {
            resourcesWithElements.put(resourcePath, element);
        }
    }

    private boolean ignore(final String path)
    {
        final String uppercasedPath = path.toUpperCase().replace(File.separatorChar, '/');

        // Unless it has been turned off...
        if (useDefaultResourceIgnoreList) {
            //  check whether the path is in the list of default ignores
            for (int idx = 0; idx < DEFAULT_IGNORED_RESOURCES.length; idx++) {
                if (DEFAULT_IGNORED_RESOURCES[idx].matcher(uppercasedPath).matches()) {
                    return true;
                }
            }
        }

        // check whether there is an user supplied ignore pattern.
        if (ignoredResourcesPatterns.isPresent()) {
            Pattern [] patterns = ignoredResourcesPatterns.get();
            for (int idx = 0; idx < patterns.length; idx++) {
                if (patterns[idx].matcher(uppercasedPath).matches()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean addCached(final File element)
    {
        if (!cachedByFile.containsKey(element)) {
            return false;
        }

        final Cached cached = cachedByFile.get(element);

        for (String className : cached.getClasses()) {
            addClass(className, element);
        }

        for (String resourceName : cached.getResources()) {
            addResource(resourceName, element);
        }

        return true;
    }

    private static class Cached
    {
        private final ImmutableList<String> classes;
        private final ImmutableList<String> resources;

        private Cached(final ImmutableList<String> classes, final ImmutableList<String> resources)
        {
            this.classes = classes;
            this.resources = resources;
        }

        public ImmutableList<String> getClasses()
        {
            return classes;
        }

        public ImmutableList<String> getResources()
        {
            return resources;
        }
    }
}
