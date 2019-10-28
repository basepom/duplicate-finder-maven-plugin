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
package org.basepom.mojo.duplicatefinder.classpath;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class TestClasspathDescriptor
{
    @Test
    public void testValidPackageNames()
    {
        String[] validNames = {
                "_.class",
                "hello/world.class",
                "test.class",
                "__auto_generated/from/some/tool.class",
                "some/inner$thing.class",
                "some/package/module-info.class", // module info in sub package
                "module-info.class" // module info in root package
        };

        for (String test : validNames) {
            Optional<List<String>> result = ClasspathDescriptor.validateClassName(test);
            assertTrue("Failure for '" + test + "'", result.isPresent());
        }
    }

    @Test
    public void testInvalidPackageNames()
    {
        String[] invalidNames = {
                null, // null value
                "", // empty string
                "/", // only slash
                "hello.world", // not a class file
                "foo/hello.world", // not a class file in subfolder
                "META-INF/test.class", // package name invalid
                "2.0/foo.class", // package name invalid
                "foo/bar/baz-blo/tst.class", // package name is invalid
                "META-INF/versions/9/foo/module-info.class", // module info in version sub package
        };


        for (String test : invalidNames) {
            Optional<List<String>> result = ClasspathDescriptor.validateClassName(test);
            assertFalse("Failure for '" + test + "'", result.isPresent());
        }
    }
}
