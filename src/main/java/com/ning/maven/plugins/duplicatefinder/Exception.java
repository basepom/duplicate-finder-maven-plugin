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

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;

public class Exception
{
    public static final String CURRENT_PROJECT_IDENTIFIER = "<current project>";

    private DependencyWrapper[] conflictingDependencies;
    private boolean currentProject;
    private Set classes = new HashSet();
    private Set packages = new HashSet();
    private Set resources = new HashSet();
    private Pattern [] matchingResources;

    public void setConflictingDependencies(Dependency[] conflictingDependencies) throws InvalidVersionSpecificationException
    {
        this.conflictingDependencies = new DependencyWrapper[conflictingDependencies.length];
        for (int idx = 0; idx < conflictingDependencies.length; idx++) {
            this.conflictingDependencies[idx] = new DependencyWrapper(conflictingDependencies[idx]);
        }
    }

    public void setResourcePatterns(String[] resourcePatterns)
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

    public void setCurrentProject(boolean currentProject)
    {
        this.currentProject = currentProject;
    }

    public String[] getClasses()
    {
        return (String[])classes.toArray(new String[classes.size()]);
    }

    public void setClasses(String[] classes)
    {
        this.classes.addAll(Arrays.asList(classes));
    }

    public String[] getPackages()
    {
        return (String[])packages.toArray(new String[packages.size()]);
    }

    public void setPackages(String[] packages)
    {
        this.packages.addAll(Arrays.asList(packages));
    }

    public String[] getResources()
    {
        return (String[])resources.toArray(new String[resources.size()]);
    }

    public void setResources(String[] resources)
    {
        this.resources.addAll(Arrays.asList(resources));
    }

    public List getDependencyNames()
    {
        List result = new ArrayList();

        if (conflictingDependencies != null) {
            for (int idx = 0; idx < conflictingDependencies.length; idx++) {
                result.add(conflictingDependencies[idx].toString());
            }
        }
        if (currentProject) {
            result.add(CURRENT_PROJECT_IDENTIFIER);
        }
        Collections.sort(result);
        return result;
    }

    public boolean isForArtifacts(Collection artifacts, Artifact projectArtifact)
    {
        int numMatches = 0;

        for (Iterator artifactIt = artifacts.iterator(); artifactIt.hasNext();) {
            Artifact artifact = (Artifact)artifactIt.next();

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

    private boolean currentProjectDependencyMatches(Artifact artifact, Artifact projectArtifact)
    {
        VersionRange versionRange = projectArtifact.getVersionRange();
        ArtifactVersion version;

        try {
            if (artifact.getVersionRange() != null) {
                version = artifact.getSelectedVersion();
            }
            else {
                version = new DefaultArtifactVersion(artifact.getVersion());
            }
        }
        catch (OverConstrainedVersionException ex) {
            return false;
        }

        return StringUtils.equals(projectArtifact.getGroupId(), artifact.getGroupId()) &&
            StringUtils.equals(projectArtifact.getArtifactId(), artifact.getArtifactId()) &&
            StringUtils.equals(StringUtils.defaultIfEmpty(projectArtifact.getType(), "jar"), StringUtils.defaultIfEmpty(artifact.getType(), "jar")) &&
            StringUtils.equals(projectArtifact.getClassifier(), artifact.getClassifier()) &&
            ((versionRange != null && versionRange.containsVersion(version)) || artifact.getVersion().equals(projectArtifact.getVersion()));
    }

    public boolean containsClass(String className)
    {
        if (classes.contains(className)) {
            return true;
        }
        else {
            for (Iterator pckgNameIt = packages.iterator(); pckgNameIt.hasNext();) {
                if (className.startsWith((String)pckgNameIt.next())) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean containsResource(String resource)
    {
        String resourceAsRelative = (resource.startsWith("/") || resource.startsWith("\\") ? resource.substring(1) : resource);

        if(resources.contains(resourceAsRelative) ||
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
