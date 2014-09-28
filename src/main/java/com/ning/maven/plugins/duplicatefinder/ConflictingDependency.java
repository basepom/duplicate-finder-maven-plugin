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

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;
import com.ning.maven.plugins.duplicatefinder.artifact.MavenCoordinates;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;

public class ConflictingDependency
{
    public static final String CURRENT_PROJECT_IDENTIFIER = "<current project>";

    private MavenCoordinates[] conflictingDependencies = new MavenCoordinates[0];
    private boolean currentProject;
    private final Set<String> classes = Sets.newHashSet();
    private final Set<String> packages = Sets.newHashSet();
    private final Set<String> resources = Sets.newHashSet();
    private Pattern[] matchingResources = new Pattern[0];

    // Called by maven
    public void setConflictingDependencies(final Dependency[] conflictingDependencies) throws InvalidVersionSpecificationException
    {
        this.conflictingDependencies = new MavenCoordinates[conflictingDependencies.length];
        for (int idx = 0; idx < conflictingDependencies.length; idx++) {
            this.conflictingDependencies[idx] = new MavenCoordinates(conflictingDependencies[idx]);
        }
    }

    // Called by maven
    public void setResourcePatterns(final String[] resourcePatterns)
    {
        this.matchingResources = new Pattern[resourcePatterns.length];
        for (int i = 0; i < resourcePatterns.length; i++) {
            this.matchingResources[i] = Pattern.compile(resourcePatterns[i], Pattern.CASE_INSENSITIVE);
        }
    }

    public boolean isCurrentProject()
    {
        return currentProject;
    }

    public void setCurrentProject(final boolean currentProject)
    {
        this.currentProject = currentProject;
    }

    public String[] getClasses()
    {
        return classes.toArray(new String[classes.size()]);
    }

    // Called by maven
    public void setClasses(final String[] classes)
    {
        this.classes.addAll(Arrays.asList(classes));
    }

    public String[] getPackages()
    {
        return packages.toArray(new String[packages.size()]);
    }

    // Called by maven
    public void setPackages(final String[] packages)
    {
        this.packages.addAll(Arrays.asList(packages));
    }

    public String[] getResources()
    {
        return resources.toArray(new String[resources.size()]);
    }

    // Called by maven
    public void setResources(final String[] resources)
    {
        this.resources.addAll(Arrays.asList(resources));
    }

    public List<String> getDependencyNames()
    {
        final List<String> result = new ArrayList<String>();

        checkState(conflictingDependencies != null, "conflictingDependencies is null");
        for (int idx = 0; idx < conflictingDependencies.length; idx++) {
            result.add(conflictingDependencies[idx].toString());
        }

        if (isCurrentProject()) {
            result.add(CURRENT_PROJECT_IDENTIFIER);
        }

        Collections.sort(result);
        return result;
    }

    public boolean isForArtifacts(final Collection<Artifact> artifacts, final Artifact projectArtifact) throws OverConstrainedVersionException
    {
        int numMatches = 0;

        for (Artifact artifact : artifacts) {
            if (conflictingDependencies != null) {
                for (int idx = 0; idx < conflictingDependencies.length; idx++) {
                    if (conflictingDependencies[idx].matches(artifact)) {
                        numMatches++;
                    }
                    else if (currentProject && currentProjectDependencyMatches(artifact, projectArtifact)) {
                        numMatches++;
                    }
                }
            }
        }
        return numMatches == artifacts.size();
    }

    private boolean currentProjectDependencyMatches(final Artifact artifact, final Artifact projectArtifact) throws OverConstrainedVersionException
    {
        final MavenCoordinates projectCoordinates = new MavenCoordinates(projectArtifact);

        return projectCoordinates.matches(artifact);
    }

    public boolean containsClass(final String className)
    {
        if (classes.contains(className)) {
            return true;
        }
        else {
            for (String packageName : packages) {
                if (className.startsWith(packageName)) { // TODO - bug here. foo.bar matches foo.barbaz
                    return true;
                }
            }
            return false;
        }
    }

    public boolean containsResource(final String resource)
    {
        final String resourceAsRelative = resource.startsWith("/") || resource.startsWith("\\") ? resource.substring(1) : resource;

        if (resources.contains(resourceAsRelative) ||
            resources.contains("/" + resourceAsRelative) ||
            resources.contains("\\" + resourceAsRelative)) {

            return true;
        }

        for (int i = 0; i < matchingResources.length; i++) {
            if (matchingResources[i].matcher(resourceAsRelative).matches()) {
                return true;
            }
        }

        return false;
    }
}
