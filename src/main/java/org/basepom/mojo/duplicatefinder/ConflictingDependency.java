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

import static com.google.common.base.Preconditions.checkNotNull;

import org.basepom.mojo.duplicatefinder.artifact.MavenCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures the &lt;exceptions&gt; section from the plugin configuration.
 */
public class ConflictingDependency {

    private static final Logger LOG = LoggerFactory.getLogger(ConflictingDependency.class);

    private final Set<MavenCoordinates> conflictingDependencies = new LinkedHashSet<>();
    private final Set<String> classes = new HashSet<>();
    private final Set<String> packages = new HashSet<>();
    private final Set<String> resources = new HashSet<>();
    private Pattern[] matchingResources = new Pattern[0];
    private boolean currentProject = false;
    private boolean currentProjectIncluded = false;

    // Called by maven
    public void setConflictingDependencies(final Dependency... conflictingDependencies) throws InvalidVersionSpecificationException {
        for (Dependency conflictingDependency : conflictingDependencies) {
            this.conflictingDependencies.add(new MavenCoordinates(conflictingDependency));
        }
    }

    // Called by maven
    public void setResourcePatterns(final String... resourcePatterns) {
        this.matchingResources = new Pattern[resourcePatterns.length];
        for (int i = 0; i < resourcePatterns.length; i++) {
            this.matchingResources[i] = Pattern.compile(resourcePatterns[i], Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
    }

    public String[] getClasses() {
        return classes.toArray(new String[0]);
    }

    // Called by maven
    public void setClasses(final String... classes) {
        this.classes.addAll(Arrays.asList(classes));
    }

    public String[] getPackages() {
        return packages.toArray(new String[0]);
    }

    // Called by maven
    public void setPackages(final String... packages) {
        this.packages.addAll(Arrays.asList(packages));
    }

    public String[] getResources() {
        return resources.toArray(new String[0]);
    }

    // Called by maven
    public void setResources(final String... resources) {
        this.resources.addAll(Arrays.asList(resources));
    }

    // Called by maven
    public void setCurrentProject(final boolean currentProject) {
        this.currentProject = currentProject;
    }

    boolean hasCurrentProject() {
        return currentProject;
    }

    // Called by maven

    @Deprecated
    public void setBootClasspath(final boolean bootClasspath) {
        LOG.warn("<bootClasspath> attribute is deprecated and has no function!");
    }

    boolean isCurrentProjectIncluded() {
        return currentProjectIncluded;
    }

    void addProjectMavenCoordinates(final MavenCoordinates projectMavenCoordinates) {
        this.currentProjectIncluded = conflictingDependencies.contains(projectMavenCoordinates);
        if (this.currentProject) {
            // The exclusion should also look at the current project, add the project
            // coordinates to the list of exclusions.
            conflictingDependencies.add(projectMavenCoordinates);
        }
    }

    List<MavenCoordinates> getDependencies() {
        return ImmutableList.copyOf(conflictingDependencies);
    }

    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    Pattern[] getResourcePatterns() {
        return matchingResources;
    }

    public List<String> getDependencyNames() {
        final List<String> result = new ArrayList<>(conflictingDependencies.size());

        for (final MavenCoordinates conflictingDependency : conflictingDependencies) {
            result.add(conflictingDependency.toString());
        }

        Collections.sort(result);
        return result;
    }

    public boolean isForArtifacts(final Set<Artifact> artifacts) throws OverConstrainedVersionException {
        checkNotNull(artifacts, "artifacts is null");

        // An exception can contain more than the actually matching
        // artifacts. In that case, we will check this exception if at least
        // the number of artifacts are present.
        if (artifacts.size() > conflictingDependencies.size()) {
            return false;
        }

        // Every artifact must be matched.
        int numMatches = artifacts.size();

        for (final Artifact artifact : artifacts) {
            for (final MavenCoordinates conflictingDependency : conflictingDependencies) {
                if (conflictingDependency.matches(artifact)) {
                    if (--numMatches == 0) {
                        return true;
                    } else {
                        break; // for (final MavenCoordinates...
                    }
                }
            }
        }
        return false;
    }

    boolean isWildcard() {
        return classes.isEmpty() && packages.isEmpty() && resources.isEmpty() && matchingResources.length == 0;
    }

    public boolean containsClass(final String className) {
        if (isWildcard()) {
            // Nothing given --> match everything.
            return true;
        }

        if (classes.contains(className)) {
            return true;
        } else {
            for (final String packageName : packages) {
                final String pkgName = packageName.endsWith(".") ? packageName : packageName + ".";
                if (className.startsWith(pkgName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean containsResource(final String resource) {
        if (isWildcard()) {
            // Nothing given --> match everything
            return true;
        }

        final String resourceAsRelative = resource.startsWith("/") || resource.startsWith("\\") ? resource.substring(1) : resource;

        if (resources.contains(resourceAsRelative) ||
                resources.contains("/" + resourceAsRelative) ||
                resources.contains("\\" + resourceAsRelative)) {

            return true;
        }

        for (Pattern matchingResource : matchingResources) {
            if (matchingResource.matcher(resourceAsRelative).matches()) {
                return true;
            }
        }

        return false;
    }
}
