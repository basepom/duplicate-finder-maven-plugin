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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;

public class ClasspathDescriptor
{
    private static final Pattern[] DEFAULT_IGNORED_RESOURCES = { Pattern.compile("(META-INF/)?ASL2\\.0(\\.TXT)?"),
                    Pattern.compile("META-INF/DEPENDENCIES(\\.TXT)?"),
                    Pattern.compile("META-INF/DISCLAIMER(\\.TXT)?"),
                    Pattern.compile("(META-INF/)?[A-Z_-]*LICENSE.*"),
                    Pattern.compile("META-INF/MANIFEST\\.MF"),
                    Pattern.compile("META-INF/INDEX\\.LIST"),
                    Pattern.compile("META-INF/MAVEN/.*"),
                    Pattern.compile("META-INF/PLEXUS/.*"),
                    Pattern.compile("META-INF/SERVICES/.*"),
                    Pattern.compile("(META-INF/)?NOTICE(\\.TXT)?"),
                    Pattern.compile("META-INF/README"),
                    Pattern.compile("OSGI-INF/.*"),
                    Pattern.compile("README(\\.TXT)?"),
                    Pattern.compile(".*PACKAGE\\.HTML"),
                    Pattern.compile(".*OVERVIEW\\.HTML"),
                    Pattern.compile("META-INF/SPRING\\.HANDLERS"),
                    Pattern.compile("META-INF/SPRING\\.SCHEMAS"),
                    Pattern.compile("META-INF/SPRING\\.TOOLING") };

    private static final Set<String> IGNORED_LOCAL_DIRECTORIES = new HashSet<String>();

    private static final Map<File, Cached> CACHED_BY_ELEMENT = new HashMap<File, Cached>();

    static {
        IGNORED_LOCAL_DIRECTORIES.add(".GIT");
        IGNORED_LOCAL_DIRECTORIES.add(".SVN");
        IGNORED_LOCAL_DIRECTORIES.add(".HG");
        IGNORED_LOCAL_DIRECTORIES.add(".BZR");
    }

    private final Map<String, Set<File>> classesWithElements = new TreeMap<String, Set<File>>();

    private final Map<String, Set<File>> resourcesWithElements = new TreeMap<String, Set<File>>();

    private boolean useDefaultResourceIgnoreList = true;

    private Pattern[] ignoredResourcesPatterns = null;

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
        if (ignoredResources != null) {
            ignoredResourcesPatterns = new Pattern[ignoredResources.length];

            try {
                for (int i = 0; i < ignoredResources.length; i++) {
                    ignoredResourcesPatterns[i] = Pattern.compile(ignoredResources[i].toUpperCase());
                }
            }
            catch (final PatternSyntaxException pse) {
                throw new MojoExecutionException("Error compiling resourceIgnore pattern: " + pse.getMessage());
            }
        }
    }

    public void add(final File element) throws IOException
    {
        if (!element.exists()) {
            throw new FileNotFoundException("Path " + element + " doesn't exist");
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
        return Collections.unmodifiableSet(classesWithElements.keySet());
    }

    public Set<String> getResources()
    {
        return Collections.unmodifiableSet(resourcesWithElements.keySet());
    }

    public Set<File> getElementsHavingClass(final String className)
    {
        final Set<File> elements = classesWithElements.get(className);

        return elements == null ? null : Collections.unmodifiableSet(elements);
    }

    public Set<File> getElementsHavingResource(final String resource)
    {
        final Set<File> elements = resourcesWithElements.get(resource);

        return elements == null ? null : Collections.unmodifiableSet(elements);
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

        final List<String> classes = new ArrayList<String>();
        final List<String> resources = new ArrayList<String>();
        final File[] files = directory.listFiles();
        final String pckgName = element.equals(directory) ? null : (parentPackageName == null ? "" : parentPackageName + ".") + directory.getName();

        if (files != null && files.length > 0) {
            for (int idx = 0; idx < files.length; idx++) {
                if (files[idx].isDirectory() && !IGNORED_LOCAL_DIRECTORIES.contains(files[idx].getName().toUpperCase())) {
                    addDirectory(element, pckgName, files[idx]);
                }
                else if (files[idx].isFile()) {
                    if ("class".equals(FilenameUtils.getExtension(files[idx].getName()))) {
                        final String className = (pckgName == null ? "" : pckgName + ".") + FilenameUtils.getBaseName(files[idx].getName());

                        classes.add(className);
                        addClass(className, element);
                    }
                    else {
                        final String resourcePath = (pckgName == null ? "" : pckgName.replace('.', '/') + "/") + files[idx].getName();

                        resources.add(resourcePath);
                        addResource(resourcePath, element);
                    }
                }
            }
        }

        CACHED_BY_ELEMENT.put(element, new Cached(classes, resources));
    }

    private void addArchive(final File element) throws IOException
    {
        if (addCached(element)) {
            return;
        }

        final List<String> classes = new ArrayList<String>();
        final List<String> resources = new ArrayList<String>();
        InputStream input = null;
        ZipInputStream zipInput = null;

        try {
            input = element.toURI().toURL().openStream();
            zipInput = new ZipInputStream(input);

            ZipEntry entry;

            while ((entry = zipInput.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    final String name = entry.getName();

                    if ("class".equals(FilenameUtils.getExtension(name))) {
                        final String className = FilenameUtils.removeExtension(name).replace('/', '.').replace('\\', '.');

                        classes.add(className);
                        addClass(className, element);
                    }
                    else {
                        final String resourcePath = name.replace('\\', File.separatorChar);

                        resources.add(resourcePath);
                        addResource(resourcePath, element);
                    }
                }
            }

            CACHED_BY_ELEMENT.put(element, new Cached(classes, resources));
        }
        finally {
            if (zipInput != null) {
                // this will also close the wrapped stream
                IOUtils.closeQuietly(zipInput);
            }
            else if (input != null) {
                IOUtils.closeQuietly(input);
            }
        }
    }

    private void addClass(final String className, final File element)
    {
        if (className.indexOf('$') < 0) {
            Set<File> elements = classesWithElements.get(className);

            if (elements == null) {
                elements = new HashSet<File>();
                classesWithElements.put(className, elements);
            }
            elements.add(element);
        }
    }

    private void addResource(final String path, final File element)
    {
        if (!ignore(path)) {
            Set<File> elements = resourcesWithElements.get(path);

            if (elements == null) {
                elements = new HashSet<File>();
                resourcesWithElements.put(path, elements);
            }
            elements.add(element);
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
        if (ignoredResourcesPatterns != null) {
            for (int idx = 0; idx < ignoredResourcesPatterns.length; idx++) {
                if (ignoredResourcesPatterns[idx].matcher(uppercasedPath).matches()) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean addCached(final File element)
    {
        final Cached cached = CACHED_BY_ELEMENT.get(element);

        if (cached == null) {
            return false;
        }

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
        private final List<String> classes;
        private final List<String> resources;

        private Cached(final List<String> classes, final List<String> resources)
        {
            this.classes = classes;
            this.resources = resources;
        }

        public List<String> getClasses()
        {
            return classes;
        }

        public List<String> getResources()
        {
            return resources;
        }
    }
}
