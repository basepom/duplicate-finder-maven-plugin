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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pyx4j.log4j.MavenLogAppender;

/**
 * Finds duplicate classes/resources.
 *
 * @goal check
 * @phase verify
 * @requiresDependencyResolution test
 * @see <a href="http://docs.codehaus.org/display/MAVENUSER/Mojo+Developer+Cookbook">Mojo Developer Cookbook</a>
 * @author Ning, Inc.
 * @author kreyssel
 */
public class DuplicateFinderMojo extends AbstractMojo
{
    protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

    // the constants for conflicts
    private final static int NO_CONFLICT = 0;
    private final static int CONFLICT_CONTENT_EQUAL = 1;
    private final static int CONFLICT_CONTENT_DIFFERENT = 2;
    
    /**
     * The maven project (effective pom).
     * @parameter expression="${project}"
     * @required
     * @readonly
    */
    private MavenProject project;

    /**
     * Whether the mojo should fail the build if a conflict with content different elements was found.
     * @parameter default-value="false"
     * @since 1.0.3
     */
    private boolean failBuildInCaseOfDifferentContentConflict;
    
    /**
     * Whether the mojo should fail the build if a conflict with content equal elements was found.
     * @parameter default-value="false"
     * @since 1.0.3
     */
    private boolean failBuildInCaseOfEqualContentConflict;
    
    /**
     * Whether the mojo should fail the build if a conflict was found. 
     * @parameter default-value="false"
     */
    private boolean failBuildInCaseOfConflict;

    /**
     * Whether the mojo should use the default resource ignore list.
     * @parameter default-value="true"
     */
    private boolean useDefaultResourceIgnoreList = true;

    /**
     * Additional resources that should be ignored.
     * @parameter alias="ignoredResources"
     */
    private String [] ignoredResources;

    /**
     * A set of artifacts with expected and resolved versions that are to be except from the check.
     * @parameter alias="exceptions"
     */
    private Exception[] exceptions;

    /**
     * A set of dependecies that should be completely ignored in the check.
     * @parameter property="ignoredDependencies"
     */
    private DependencyWrapper[] ignoredDependencies;

    /**
     * Skip the plugin execution.
     *
     * <pre>
     *   <configuration>
     *     <skip>true</skip>
     *   </configuration>
     * </pre>
     *
     * @parameter default-value="false"
     */
    protected boolean skip = false;

    public void setIgnoredDependencies(Dependency[] ignoredDependencies) throws InvalidVersionSpecificationException
    {
        this.ignoredDependencies = new DependencyWrapper[ignoredDependencies.length];
        for (int idx = 0; idx < ignoredDependencies.length; idx++) {
            this.ignoredDependencies[idx] = new DependencyWrapper(ignoredDependencies[idx]);
        }
    }

    public void execute() throws MojoExecutionException
    {
        MavenLogAppender.startPluginLog(this);

        try {
            if (skip) {
                LOG.debug("Skipping execution!");
            }
            else {
                checkCompileClasspath();
                checkRuntimeClasspath();
                checkTestClasspath();
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

            Map artifactsByFile = createArtifactsByFileMap(project.getCompileArtifacts());

            addOutputDirectory(artifactsByFile);
            checkClasspath(project.getCompileClasspathElements(), artifactsByFile);
        }
        catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Could not resolve dependencies", ex);
        }
    }
    
    private void checkRuntimeClasspath() throws MojoExecutionException
    {
        try {
            LOG.info("Checking runtime classpath");

            Map artifactsByFile = createArtifactsByFileMap(project.getRuntimeArtifacts());

            addOutputDirectory(artifactsByFile);
            checkClasspath(project.getRuntimeClasspathElements(), artifactsByFile);
        }
        catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Could not resolve dependencies", ex);
        }
    }

    private void checkTestClasspath() throws MojoExecutionException
    {
        try {
            LOG.info("Checking test classpath");

            Map artifactsByFile = createArtifactsByFileMap(project.getTestArtifacts());

            addOutputDirectory(artifactsByFile);
            addTestOutputDirectory(artifactsByFile);
            checkClasspath(project.getTestClasspathElements(), artifactsByFile);
        }
        catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException("Could not resolve dependencies", ex);
        }
    }

    private void checkClasspath(List classpathElements, Map artifactsByFile) throws MojoExecutionException
    {
        ClasspathDescriptor classpathDesc = createClasspathDescriptor(classpathElements);

        int foundDuplicateClassesConflict   = checkForDuplicateClasses(classpathDesc, artifactsByFile);
        int foundDuplicateResourcesConflict = checkForDuplicateResources(classpathDesc, artifactsByFile);
        int maxConflict = Math.max(foundDuplicateClassesConflict, foundDuplicateResourcesConflict);
        
        if ( (failBuildInCaseOfConflict && maxConflict > NO_CONFLICT) ||        		
        	 (failBuildInCaseOfDifferentContentConflict && maxConflict == CONFLICT_CONTENT_DIFFERENT) ||
        	 (failBuildInCaseOfEqualContentConflict && maxConflict >= CONFLICT_CONTENT_EQUAL)	) {
            throw new MojoExecutionException("Found duplicate classes/resources");
        }
    }

    private int checkForDuplicateClasses(ClasspathDescriptor classpathDesc, Map artifactsByFile) throws MojoExecutionException
    {
        Map classDifferentConflictsByArtifactNames = new TreeMap(new ToStringComparator());
        Map classEqualConflictsByArtifactNames = new TreeMap(new ToStringComparator());

        for (Iterator classNameIt = classpathDesc.getClasss().iterator(); classNameIt.hasNext();) {
            String    className = (String)classNameIt.next();
            Set       elements  = classpathDesc.getElementsHavingClass(className);

            if (elements.size() > 1) {
                Set artifacts = getArtifactsForElements(elements, artifactsByFile);

                filterIgnoredDependencies(artifacts);
                if ((artifacts.size() < 2) || isExceptedClass(className, artifacts)) {
                    continue;
                }
                
                Map conflictsByArtifactNames;
                
                if(isAllElementsAreEqual(elements, className.replace('.', '/') + ".class"))
                {
                	conflictsByArtifactNames = classEqualConflictsByArtifactNames;
                } else
                {
                	conflictsByArtifactNames = classDifferentConflictsByArtifactNames;	
                }
                
                String artifactNames = getArtifactsToString(artifacts);
                List classNames = (List)conflictsByArtifactNames.get(artifactNames); 

                if (classNames == null) {
                    classNames = new ArrayList();
                    conflictsByArtifactNames.put(artifactNames, classNames);
                }
                classNames.add(className);
            }
        }

        int conflict = NO_CONFLICT;
        
        if(!classEqualConflictsByArtifactNames.isEmpty())
        {
        	printWarningMessage(classEqualConflictsByArtifactNames, "(but equal)", "classes");
        	
        	conflict = CONFLICT_CONTENT_EQUAL;
        }
        
        if (!classDifferentConflictsByArtifactNames.isEmpty())
        {
            printWarningMessage(classDifferentConflictsByArtifactNames, "and different", "classes");
            
            conflict = CONFLICT_CONTENT_DIFFERENT;
        }
        
        return conflict;
    }
    
    private int checkForDuplicateResources(ClasspathDescriptor classpathDesc, Map artifactsByFile) throws MojoExecutionException
    {
    	Map resourceDifferentConflictsByArtifactNames = new TreeMap(new ToStringComparator());
        Map resourceEqualConflictsByArtifactNames = new TreeMap(new ToStringComparator());

        for (Iterator resourceIt = classpathDesc.getResources().iterator(); resourceIt.hasNext();) {
            String    resource = (String)resourceIt.next();
            Set       elements = classpathDesc.getElementsHavingResource(resource);

            if (elements.size() > 1) {
                Set artifacts = getArtifactsForElements(elements, artifactsByFile);

                filterIgnoredDependencies(artifacts);
                if ((artifacts.size() < 2) || isExceptedResource(resource, artifacts)) {
                    continue;
                }
                
                Map conflictsByArtifactNames;
                
                if(isAllElementsAreEqual(elements, resource))
                {
                	conflictsByArtifactNames = resourceEqualConflictsByArtifactNames;
                } else
                {
                	conflictsByArtifactNames = resourceDifferentConflictsByArtifactNames;	
                }

                String artifactNames = getArtifactsToString(artifacts);
                List resources = (List)conflictsByArtifactNames.get(artifactNames); 

                if (resources == null) {
                    resources = new ArrayList();
                    conflictsByArtifactNames.put(artifactNames, resources);
                }
                resources.add(resource);
            }
        }

        int conflict = NO_CONFLICT;
                
        if(!resourceEqualConflictsByArtifactNames.isEmpty()) {
        	printWarningMessage(resourceEqualConflictsByArtifactNames, "(but equal)", "resources");
        	
        	return CONFLICT_CONTENT_EQUAL;
        }
        
        if (!resourceDifferentConflictsByArtifactNames.isEmpty())
        {
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
    private void printWarningMessage(Map conflictsByArtifactNames, String hint, String type)
    {
    	for (Iterator conflictIt = conflictsByArtifactNames.entrySet().iterator(); conflictIt.hasNext();) {
            Map.Entry entry         = (Map.Entry)conflictIt.next();
            String    artifactNames = (String)entry.getKey();
            List      classNames    = (List)entry.getValue();

            LOG.warn("Found duplicate " + hint + " " + type + " in " + artifactNames + " :");
            for (Iterator classNameIt = classNames.iterator(); classNameIt.hasNext();) {
                LOG.warn("  " + classNameIt.next());
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
    private boolean isAllElementsAreEqual(final Set elements, final String resourcePath) 
    {
    	File firstFile = null;
		String firstSHA256 = null;
		
		for(Iterator it=elements.iterator(); it.hasNext();)
		{
			File file = (File) it.next();
			try {
				String newSHA256 = getSHA256HexOfElement(file, resourcePath);
				
				if(firstSHA256 == null)
				{
					// save sha256 hash from the first element
					firstSHA256 = newSHA256;
					firstFile = file;
				}
				else if(!newSHA256.equals(firstSHA256)) {
					LOG.debug("Found different SHA256-Hashs for element " + resourcePath + " in file " + firstFile + " and " + file);
					return false;
				}
			} catch (IOException ex)
			{
				LOG.warn("Could not read content from file " + file + "!", ex);
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
    	ZipFile zip = new ZipFile(file);
    	ZipEntry zipEntry = zip.getEntry(resourcePath);
    	if(zipEntry == null)
    	{
    		throw new IOException("Could not found Zip-Entry for " + resourcePath + " in file " + file);
    	}
    	
    	String sha256;
    	
		InputStream in = zip.getInputStream(zipEntry);
		try
		{
			sha256 = DigestUtils.sha256Hex(in);
		} finally {
			IOUtils.closeQuietly(in);
		}
    		
    	return sha256;
    }

    private void filterIgnoredDependencies(final Set artifacts)
    {
        if (ignoredDependencies != null) {
            for (int idx = 0; idx < ignoredDependencies.length; idx++) {
                for (Iterator artifactIt = artifacts.iterator(); artifactIt.hasNext();) {
                    Artifact artifact = (Artifact)artifactIt.next();
    
                    if (ignoredDependencies[idx].matches(artifact)) {
                        artifactIt.remove();
                    }
                }
            }
        }
    }
    
    private boolean isExceptedClass(final String className, final Collection artifacts)
    {
        List exceptions = getExceptionsFor(artifacts);

        for (Iterator it = exceptions.iterator(); it.hasNext();) {
            Exception exception = (Exception)it.next();

            if (exception.containsClass(className)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExceptedResource(String resource, Collection artifacts)
    {
        List exceptions = getExceptionsFor(artifacts);

        for (Iterator it = exceptions.iterator(); it.hasNext();) {
            Exception exception = (Exception)it.next();

            if (exception.containsResource(resource)) {
                return true;
            }
        }
        return false;
    }
    
    private List getExceptionsFor(Collection artifacts)
    {
        List result = new ArrayList();

        if (exceptions != null) {
            for (int idx = 0; idx < exceptions.length; idx++) {
                if (exceptions[idx].isForArtifacts(artifacts, project.getArtifact())) {
                    result.add(exceptions[idx]);
                }
            }
        }
        return result;
    }

    private Set getArtifactsForElements(Collection elements, Map artifactsByFile)
    {
        Set artifacts = new TreeSet();

        for (Iterator elementUrlIt = elements.iterator(); elementUrlIt.hasNext();) {
            File     element  = (File)elementUrlIt.next();
            Artifact artifact = (Artifact)artifactsByFile.get(element);

            if (artifact == null) {
                artifact = project.getArtifact();
            }
            artifacts.add(artifact);
        }
        return artifacts;
    }

    private String getArtifactsToString(Collection artifacts)
    {
        StringBuffer result = new StringBuffer();

        result.append("[");
        for (Iterator it = artifacts.iterator(); it.hasNext();) {
            if (result.length() > 1) {
                result.append(",");
            }
            result.append(getQualifiedName((Artifact)it.next()));
        }
        result.append("]");
        return result.toString();
    }
    
    private ClasspathDescriptor createClasspathDescriptor(List classpathElements) throws MojoExecutionException
    {
        ClasspathDescriptor classpathDesc = new ClasspathDescriptor();

        classpathDesc.setUseDefaultResourceIgnoreList(useDefaultResourceIgnoreList);
        classpathDesc.setIgnoredResources(ignoredResources);

        for (Iterator elementIt = classpathElements.iterator(); elementIt.hasNext();) {
            String element = (String)elementIt.next();

            try {
                classpathDesc.add(new File(element));
            }
            catch (FileNotFoundException ex) {
                LOG.debug("Could not access classpath element " + element);
            }
            catch (IOException ex) {
                throw new MojoExecutionException("Error trying to access element " + element, ex);
            }
        }
        return classpathDesc;
    }

    private Map createArtifactsByFileMap(List artifacts) throws DependencyResolutionRequiredException
    {
        Map artifactsByFile = new HashMap(artifacts.size());

        for (Iterator artifactIt = artifacts.iterator(); artifactIt.hasNext();) {
            Artifact artifact  = (Artifact)artifactIt.next();
            File     localPath = getLocalProjectPath(artifact);
            File     repoPath  = artifact.getFile();

            if ((localPath == null) && (repoPath == null))
            {
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

    private File getLocalProjectPath(Artifact artifact) throws DependencyResolutionRequiredException
    {
        String       refId         = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
        MavenProject owningProject = (MavenProject)project.getProjectReferences().get(refId);

        if (owningProject != null)
        {
            if (artifact.getType().equals("test-jar"))
            {
                File testOutputDir = new File(owningProject.getBuild().getTestOutputDirectory());

                if (testOutputDir.exists())
                {
                    return testOutputDir;
                }
            }
            else
            {
                return new File(project.getBuild().getOutputDirectory());
            }
        }
        return null;
    }

    private void addOutputDirectory(Map artifactsByFile)
    {
        File outputDir = new File(project.getBuild().getOutputDirectory());

        if (outputDir.exists()) {
            artifactsByFile.put(outputDir, null);
        }
    }
    
    private void addTestOutputDirectory(Map artifactsByFile)
    {
        File outputDir = new File(project.getBuild().getOutputDirectory());

        if (outputDir.exists()) {
            artifactsByFile.put(outputDir, null);
        }
    }

    private String getQualifiedName(Artifact artifact)
    {
        String result = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();

        if ((artifact.getType() != null) && !"jar".equals(artifact.getType())) {
            result = result +  ":" + artifact.getType();
        }
        if ((artifact.getClassifier() != null) && (!"tests".equals(artifact.getClassifier()) || !"test-jar".equals(artifact.getType()))) {
            result = result +  ":" + artifact.getClassifier();
        }
        return result;
    }
}
