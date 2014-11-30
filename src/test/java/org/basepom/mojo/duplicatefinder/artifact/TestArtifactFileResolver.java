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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.basepom.mojo.duplicatefinder.artifact.ArtifactFileResolver;
import org.junit.Test;

public class TestArtifactFileResolver
{
    private final ArtifactHandler handler = new DefaultArtifactHandler();

    @Test
    public void testCanonicalization()
    {
        final Artifact jarArtifact = new DefaultArtifact("foo.group", "foo-id", "1.0", "compile", "jar", null, handler);

        assertEquals(jarArtifact, ArtifactFileResolver.canonicalizeArtifact(jarArtifact));

        final Artifact testJarArtifact = new DefaultArtifact("foo.group", "foo-id", "1.0", "compile", "jar", "tests", handler);
        assertEquals(testJarArtifact, ArtifactFileResolver.canonicalizeArtifact(testJarArtifact));
        assertEquals(testJarArtifact, ArtifactFileResolver.canonicalizeArtifact(new DefaultArtifact("foo.group", "foo-id", "1.0", "compile", "test-jar", null, handler)));
        assertEquals(testJarArtifact, ArtifactFileResolver.canonicalizeArtifact(new DefaultArtifact("foo.group", "foo-id", "1.0", "compile", "test-jar", "tests", handler)));

        final Artifact testArtifact = new DefaultArtifact("foo.group", "foo-id", "1.0", "compile", "zip", "tests", handler);
        assertEquals(testArtifact, ArtifactFileResolver.canonicalizeArtifact(testArtifact));

        final Artifact testClassifiedJarArtifact = new DefaultArtifact("foo.group", "foo-id", "1.0", "compile", "test-jar", "special", handler);
        assertEquals(testClassifiedJarArtifact, ArtifactFileResolver.canonicalizeArtifact(testClassifiedJarArtifact));
    }
}
