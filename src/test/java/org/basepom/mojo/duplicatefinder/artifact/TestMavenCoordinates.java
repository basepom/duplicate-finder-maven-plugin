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
package org.basepom.mojo.duplicatefinder.artifact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.basepom.mojo.duplicatefinder.artifact.MavenCoordinates;
import org.junit.Test;

public class TestMavenCoordinates
{
    @Test
    public void testCreateFromDependencyBasic() throws java.lang.Exception
    {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("test.group");
        dependency.setArtifactId("test-artifact");
        dependency.setVersion("1.0");

        final MavenCoordinates coordinates = new MavenCoordinates(dependency);
        assertEquals("test.group", coordinates.getGroupId());
        assertEquals("test-artifact", coordinates.getArtifactId());
        assertEquals("jar", coordinates.getType());

        assertTrue(coordinates.getVersion().isPresent());
        assertTrue(coordinates.getVersionRange().isPresent());
        assertEquals(new DefaultArtifactVersion("1.0"), coordinates.getVersion().get());

        assertFalse(coordinates.getClassifier().isPresent());
    }

    @Test
    public void testCreateFromDependencyWithClassifier() throws java.lang.Exception
    {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("test.group");
        dependency.setArtifactId("test-artifact");
        dependency.setVersion("1.0");
        dependency.setClassifier("jdk17");

        final MavenCoordinates coordinates = new MavenCoordinates(dependency);
        assertEquals("test.group", coordinates.getGroupId());
        assertEquals("test-artifact", coordinates.getArtifactId());
        assertEquals("jar", coordinates.getType());

        assertTrue(coordinates.getVersion().isPresent());
        assertTrue(coordinates.getVersionRange().isPresent());
        assertEquals(new DefaultArtifactVersion("1.0"), coordinates.getVersion().get());

        assertTrue(coordinates.getClassifier().isPresent());
        assertEquals("jdk17", coordinates.getClassifier().get());
    }

    @Test
    public void testCreateFromDependencyWithVersionRange() throws java.lang.Exception
    {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("test.group");
        dependency.setArtifactId("test-artifact");
        dependency.setVersion("[1.0, 2.0)");

        final MavenCoordinates coordinates = new MavenCoordinates(dependency);
        assertEquals("test.group", coordinates.getGroupId());
        assertEquals("test-artifact", coordinates.getArtifactId());
        assertEquals("jar", coordinates.getType());

        assertTrue(coordinates.getVersion().isPresent());
        assertEquals(new DefaultArtifactVersion("[1.0, 2.0)"), coordinates.getVersion().get());

        assertTrue(coordinates.getVersionRange().isPresent());
        assertTrue(coordinates.getVersionRange().get().containsVersion(new DefaultArtifactVersion("1.1")));
        assertFalse(coordinates.getVersionRange().get().containsVersion(new DefaultArtifactVersion("2.0")));
    }

    @Test
    public void testCreateFromArtifactBasic() throws java.lang.Exception
    {
        final Artifact artifact = new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", null, new DefaultArtifactHandler());

        final MavenCoordinates coordinates = new MavenCoordinates(artifact);
        assertEquals("test.group", coordinates.getGroupId());
        assertEquals("test-artifact", coordinates.getArtifactId());
        assertEquals("jar", coordinates.getType());

        assertTrue(coordinates.getVersion().isPresent());
        assertTrue(coordinates.getVersionRange().isPresent());
        assertEquals(new DefaultArtifactVersion("1.0"), coordinates.getVersion().get());

        assertFalse(coordinates.getClassifier().isPresent());
    }

    @Test
    public void testCreateFromArtifactWithClassifier() throws java.lang.Exception
    {
        final Artifact artifact = new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", "jdk17", new DefaultArtifactHandler());

        final MavenCoordinates coordinates = new MavenCoordinates(artifact);
        assertEquals("test.group", coordinates.getGroupId());
        assertEquals("test-artifact", coordinates.getArtifactId());
        assertEquals("jar", coordinates.getType());

        assertTrue(coordinates.getVersion().isPresent());
        assertTrue(coordinates.getVersionRange().isPresent());
        assertEquals(new DefaultArtifactVersion("1.0"), coordinates.getVersion().get());

        assertTrue(coordinates.getClassifier().isPresent());
        assertEquals("jdk17", coordinates.getClassifier().get());
    }

    @Test
    public void testCreateFromArtifactWithVersionRange() throws java.lang.Exception
    {
        final VersionRange versionRange = VersionRange.createFromVersionSpec("[1.0, 2.0)");
        final Artifact artifact = new DefaultArtifact("test.group", "test-artifact", versionRange, "test", "jar", "jdk17", new DefaultArtifactHandler());

        final MavenCoordinates coordinates = new MavenCoordinates(artifact);
        assertEquals("test.group", coordinates.getGroupId());
        assertEquals("test-artifact", coordinates.getArtifactId());
        assertEquals("jar", coordinates.getType());

        assertFalse(coordinates.getVersion().isPresent());
        assertTrue(coordinates.getVersionRange().isPresent());
    }

    @Test
    public void testCreateFromArtifactWithVersionRangeRestricted() throws java.lang.Exception
    {
        final VersionRange versionRange = VersionRange.createFromVersionSpec("[1.0, 2.0)").restrict(VersionRange.createFromVersion("1.2"));
        final Artifact artifact = new DefaultArtifact("test.group", "test-artifact", versionRange, "test", "jar", "jdk17", new DefaultArtifactHandler());

        final MavenCoordinates coordinates = new MavenCoordinates(artifact);
        assertEquals("test.group", coordinates.getGroupId());
        assertEquals("test-artifact", coordinates.getArtifactId());
        assertEquals("jar", coordinates.getType());

        assertTrue(coordinates.getVersion().isPresent());
        assertEquals(new DefaultArtifactVersion("1.2"), coordinates.getVersion().get());

        assertTrue(coordinates.getVersionRange().isPresent());
    }

    @Test
    public void testTestJar() throws java.lang.Exception
    {
        MavenCoordinates coordinates = new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "test-jar", null, new DefaultArtifactHandler()));
        assertEquals("jar", coordinates.getType());
        assertTrue(coordinates.getClassifier().isPresent());
        assertEquals("tests", coordinates.getClassifier().get());

        coordinates = new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", "tests", new DefaultArtifactHandler()));
        assertEquals("jar", coordinates.getType());
        assertTrue(coordinates.getClassifier().isPresent());
        assertEquals("tests", coordinates.getClassifier().get());

        // Existing classifier gets preserved
        coordinates = new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "test-jar", "special", new DefaultArtifactHandler()));
        assertEquals("jar", coordinates.getType());
        assertTrue(coordinates.getClassifier().isPresent());
        assertEquals("special", coordinates.getClassifier().get());
    }

    @Test
    public void testBasicMismatches() throws java.lang.Exception
    {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("test.group");
        dependency.setArtifactId("test-artifact");
        dependency.setVersion("1.0");
        final MavenCoordinates dependencyCoordinates = new MavenCoordinates(dependency);

        // group mismatch
        assertFalse(dependencyCoordinates.matches(new MavenCoordinates(new DefaultArtifact("test.group2", "test-artifact", "1.0", "test", "jar", null, new DefaultArtifactHandler()))));
        // artifact id mismatch
        assertFalse(dependencyCoordinates.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact2", "1.0", "test", "jar", null, new DefaultArtifactHandler()))));
        // type mismatch
        assertFalse(dependencyCoordinates.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "war", null, new DefaultArtifactHandler()))));

        // no classifier matches any classifier
        assertTrue(dependencyCoordinates.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", null, new DefaultArtifactHandler()))));
        assertTrue(dependencyCoordinates.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", "foo", new DefaultArtifactHandler()))));
        assertTrue(dependencyCoordinates.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", "bar", new DefaultArtifactHandler()))));

        dependency.setClassifier("baz");
        final MavenCoordinates dependencyCoordinatesWithClassifier = new MavenCoordinates(dependency);

        // classifier matches same classifier
        assertTrue(dependencyCoordinatesWithClassifier.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", "baz", new DefaultArtifactHandler()))));

        // classifier does not match empty classifier or other classifier
        assertFalse(dependencyCoordinatesWithClassifier.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", null, new DefaultArtifactHandler()))));
        assertFalse(dependencyCoordinatesWithClassifier.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", "bar", new DefaultArtifactHandler()))));
    }

    @Test
    public void testVersionMatches() throws java.lang.Exception
    {
        final Dependency dependency = new Dependency();
        dependency.setGroupId("test.group");
        dependency.setArtifactId("test-artifact");
        final MavenCoordinates dependencyCoordinatesNoVersionNoVersionRange = new MavenCoordinates(dependency);

        // match any version
        assertTrue(dependencyCoordinatesNoVersionNoVersionRange.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", null, new DefaultArtifactHandler()))));
        assertTrue(dependencyCoordinatesNoVersionNoVersionRange.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "2.0", "test", "jar", null, new DefaultArtifactHandler()))));

        dependency.setVersion("1.0");
        final MavenCoordinates dependencyCoordinatesVersion = new MavenCoordinates(dependency);

        // don't match anything without version
        assertFalse(dependencyCoordinatesVersion.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", VersionRange.createFromVersionSpec("[1.0, 2.0)"), "test", "jar", null, new DefaultArtifactHandler()))));

        // But exact versions match (or don't)
        assertFalse(dependencyCoordinatesVersion.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.1", "test", "jar", null, new DefaultArtifactHandler()))));
        assertTrue(dependencyCoordinatesVersion.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", null, new DefaultArtifactHandler()))));

        dependency.setVersion("[2.0, 3.0)");
        final MavenCoordinates dependencyCoordinatesVersionRange = new MavenCoordinates(dependency);

        // Don't match outside version range
        assertFalse(dependencyCoordinatesVersionRange.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "1.0", "test", "jar", null, new DefaultArtifactHandler()))));
        assertFalse(dependencyCoordinatesVersionRange.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "3.0", "test", "jar", null, new DefaultArtifactHandler()))));

        // match inside
        assertTrue(dependencyCoordinatesVersionRange.matches(new MavenCoordinates(new DefaultArtifact("test.group", "test-artifact", "2.2", "test", "jar", null, new DefaultArtifactHandler()))));

    }

}
