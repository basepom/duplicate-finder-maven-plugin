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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.Sets;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;
import org.basepom.mojo.duplicatefinder.artifact.MavenCoordinates;

/**
 * Captures the &lt;exceptions&gt; section from the plugin configuration.
 */
public class ConflictingDependency
{
    private MavenCoordinates[] conflictingDependencies = new MavenCoordinates[0];
    private final Set<String> classes = Sets.newHashSet();
    private final Set<String> packages = Sets.newHashSet();
    private final Set<String> resources = Sets.newHashSet();
    private Pattern[] matchingResources = new Pattern[0];

    // Called by maven
    public void setConflictingDependencies(final Dependency[] conflictingDependencies) throws InvalidVersionSpecificationException
    {
        checkArgument(conflictingDependencies != null && conflictingDependencies.length > 1, "For an exception, at least two dependencies must be given!");

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
            this.matchingResources[i] = Pattern.compile(resourcePatterns[i], Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
        }
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

    MavenCoordinates [] getDependencies()
    {
        return conflictingDependencies;
    }

    Pattern [] getResourcePatterns()
    {
        return matchingResources;
    }

    public List<String> getDependencyNames()
    {
        final List<String> result = new ArrayList<String>();

        checkState(conflictingDependencies != null, "conflictingDependencies is null");
        for (int idx = 0; idx < conflictingDependencies.length; idx++) {
            result.add(conflictingDependencies[idx].toString());
        }

        Collections.sort(result);
        return result;
    }

    public boolean isForArtifacts(final Set<Artifact> artifacts) throws OverConstrainedVersionException
    {
        checkNotNull(artifacts, "artifacts is null");
        checkState(conflictingDependencies != null, "conflictingDependencies is null");
        checkState(conflictingDependencies.length > 1, "conflictingDependencies has not at least two dependencies");

        if (artifacts.size() != conflictingDependencies.length) {
            return false;
        }

        int numMatches = conflictingDependencies.length;

        for (final Artifact artifact : artifacts) {
            for (int idx = 0; idx < conflictingDependencies.length; idx++) {
                if (conflictingDependencies[idx].matches(artifact)) {
                    if (--numMatches == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean currentProjectDependencyMatches(final Artifact artifact, final Artifact projectArtifact) throws OverConstrainedVersionException
    {
        final MavenCoordinates projectCoordinates = new MavenCoordinates(projectArtifact);

        return projectCoordinates.matches(artifact);
    }

    public boolean containsClass(final String className)
    {
        if (classes.isEmpty() && packages.isEmpty()) {
            // Nothing given --> match everything.
            return true;
        }

        if (classes.contains(className)) {
            return true;
        }
        else {
            for (final String packageName : packages) {
                String pkgName = packageName.endsWith(".") ? packageName : packageName + ".";
                if (className.startsWith(pkgName)) {
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
