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

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import com.google.common.base.Optional;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.project.MavenProject;
import org.basepom.mojo.duplicatefinder.ResultCollector.ConflictResult;
import org.basepom.mojo.duplicatefinder.artifact.MavenCoordinates;
import org.basepom.mojo.duplicatefinder.classpath.ClasspathDescriptor;
import org.codehaus.staxmate.out.SMOutputElement;

public final class XMLWriterUtils
{
    private XMLWriterUtils()
    {
        throw new AssertionError("do not instantiate");
    }

    public static SMOutputElement addElement(SMOutputElement document, String name, Object value)
        throws XMLStreamException
    {
        SMOutputElement element = document.addElement(name);
        if (value != null) {
            element.addCharacters(value.toString());
        }
        return element;
    }

    public static void addAttribute(SMOutputElement document, String name, Object value)
        throws XMLStreamException
    {
        if (value != null) {
            document.addAttribute(name, value.toString());
        }
    }

    public static void addProjectInformation(SMOutputElement rootElement, MavenProject project)
        throws XMLStreamException
    {
        SMOutputElement projectElement = rootElement.addElement("project");
        addAttribute(projectElement, "artifactId", project.getArtifact().getArtifactId());
        addAttribute(projectElement, "groupId", project.getArtifact().getGroupId());
        addAttribute(projectElement, "version", project.getArtifact().getVersion());
        addAttribute(projectElement, "classifier", project.getArtifact().getClassifier());
        addAttribute(projectElement, "type", project.getArtifact().getType());
    }

    public static void addConflictingDependency(SMOutputElement conflictingDependenciesElement, String name, ConflictingDependency conflictingDependency)
        throws XMLStreamException
    {
        SMOutputElement conflictingDependencyElement = conflictingDependenciesElement.addElement(name);

        addAttribute(conflictingDependencyElement, "currentProject", conflictingDependency.hasCurrentProject());
        addAttribute(conflictingDependencyElement, "currentProjectIncluded", conflictingDependency.isCurrentProjectIncluded());

        SMOutputElement dependenciesElement = conflictingDependencyElement.addElement("dependencies");
        for (MavenCoordinates dependency : conflictingDependency.getDependencies()) {
            addMavenCoordinate(dependenciesElement, "dependency", dependency);
        }

        SMOutputElement packagesElement = conflictingDependencyElement.addElement("packages");
        for (String packageName : conflictingDependency.getPackages()) {
            addElement(packagesElement, "package", packageName);
        }

        SMOutputElement classesElement = conflictingDependencyElement.addElement("classes");
        for (String className : conflictingDependency.getClasses()) {
            addElement(classesElement, "class", className);
        }

        SMOutputElement resourcesElement = conflictingDependencyElement.addElement("resources");
        for (String resourceName : conflictingDependency.getResources()) {
            addElement(resourcesElement, "resource", resourceName);
        }

        SMOutputElement resourcePatternsElement = conflictingDependencyElement.addElement("resourcePatterns");
        for (Pattern resourcePattern : conflictingDependency.getResourcePatterns()) {
            addElement(resourcePatternsElement, "resourcePattern", resourcePattern.toString());
        }
    }

    public static void addMavenCoordinate(SMOutputElement dependenciesElement, String name, MavenCoordinates dependency)
        throws XMLStreamException
    {
        SMOutputElement dependencyElement = dependenciesElement.addElement(name);
        addAttribute(dependencyElement, "artifactId", dependency.getArtifactId());
        addAttribute(dependencyElement, "groupId", dependency.getGroupId());

        if (dependency.getVersion().isPresent()) {
            addAttribute(dependencyElement, "version", dependency.getVersion().get());
        }

        if (dependency.getVersionRange().isPresent()) {
            addAttribute(dependencyElement, "versionRange", dependency.getVersionRange().get());
        }

        if (dependency.getClassifier().isPresent()) {
            addAttribute(dependencyElement, "classifier", dependency.getClassifier().get());
        }

        addAttribute(dependencyElement, "type", dependency.getType());
    }

    public static void addArtifact(SMOutputElement artifactElement, String name, Artifact artifact)
        throws XMLStreamException, OverConstrainedVersionException
    {
        // lazy. Replace with a real dependency writer if that somehow loses or mangles information.
        MavenCoordinates coordinates = new MavenCoordinates(artifact);
        addMavenCoordinate(artifactElement, name, coordinates);
    }

    public static void addResultCollector(SMOutputElement resultElement, ResultCollector resultCollector)
        throws XMLStreamException, OverConstrainedVersionException
    {
        addAttribute(resultElement, "conflictState", resultCollector.getConflictState());
        addAttribute(resultElement, "failed", resultCollector.isFailed());
        SMOutputElement conflictsElement = resultElement.addElement("conflicts");

        for (Map.Entry<String, Collection<ConflictResult>> entry : resultCollector.getAllResults().entrySet()) {
            SMOutputElement conflictElement = conflictsElement.addElement("conflict");
            addAttribute(conflictElement, "name", entry.getKey());
            SMOutputElement conflictResultsElement = conflictElement.addElement("conflictResults");
            for (ConflictResult conflictResult : entry.getValue()) {
                addConflictResult(conflictResultsElement, conflictResult);
            }
        }
    }

    private static void addConflictResult(SMOutputElement conflictResultsElement, ConflictResult conflictResult)
        throws XMLStreamException, OverConstrainedVersionException
    {
        SMOutputElement conflictResultElement = conflictResultsElement.addElement("conflictResult");
        addAttribute(conflictResultElement, "name", conflictResult.getName());
        addAttribute(conflictResultElement, "type", conflictResult.getType());
        addAttribute(conflictResultElement, "excepted", conflictResult.isExcepted());
        addAttribute(conflictResultElement, "failed", conflictResult.isFailed());
        addAttribute(conflictResultElement, "printed", conflictResult.isPrinted());
        addAttribute(conflictResultElement, "conflictState", conflictResult.getConflictState());
        SMOutputElement conflictNames = conflictResultElement.addElement("conflictNames");
        for (Map.Entry<String, Optional<Artifact>> entry : conflictResult.getConflictArtifactNames().entrySet()) {
            SMOutputElement conflictName = conflictNames.addElement("conflictName");
            addAttribute(conflictName, "name", entry.getKey());
            if (entry.getValue().isPresent()) {
                addArtifact(conflictName, "artifact", entry.getValue().get());
            }
        }
    }

    public static void addClasspathDescriptor(SMOutputElement resultElement, int resultFileMinClasspathCount, ClasspathDescriptor classpathDescriptor)
        throws XMLStreamException
    {
        SMOutputElement resourceExclusionPatternsElement = resultElement.addElement("ignoredResourcePatterns");
        for (Pattern resourceExclusionPattern : classpathDescriptor.getIgnoredResourcePatterns()) {
            addElement(resourceExclusionPatternsElement, "ignoredResourcePattern", resourceExclusionPattern.toString());
        }

        SMOutputElement ignoredDirectoriesPatternsElement = resultElement.addElement("ignoredDirectoryPatterns");
        for (Pattern ignoredDirectoriesPattern : classpathDescriptor.getIgnoredDirectoryPatterns()) {
            addElement(ignoredDirectoriesPatternsElement, "ignoredDirectoryPattern", ignoredDirectoriesPattern.toString());
        }

        for (ConflictType type : ConflictType.values()) {
            SMOutputElement classpathElementsElement = resultElement.addElement("classpathElements");
            addAttribute(classpathElementsElement, "type", type);
            for (Map.Entry<String, Collection<File>> entry : classpathDescriptor.getClasspathElementLocations(type).entrySet()) {
                if (entry.getValue().size() >= resultFileMinClasspathCount) {
                    SMOutputElement classpathElementElement = classpathElementsElement.addElement("classpathElement");
                    addAttribute(classpathElementElement, "name", entry.getKey());
                    for (File file : entry.getValue()) {
                        addElement(classpathElementElement, "file", file.getPath());
                    }
                }
            }
        }
    }
}
