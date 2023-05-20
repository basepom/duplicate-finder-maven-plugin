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

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestPackageNameHolder {

    @Test
    public void testEmpty() {
        final PackageNameHolder p1 = new PackageNameHolder();
        assertEquals("foo", p1.getQualifiedName("foo"));
        assertEquals("foo", p1.getQualifiedPath("foo"));

        final PackageNameHolder p2 = p1.getChildPackage("bar");
        assertEquals("bar.foo", p2.getQualifiedName("foo"));
        assertEquals("bar/foo", p2.getQualifiedPath("foo"));
    }

    @Test
    public void testPrefilled() {
        final PackageNameHolder p1 = new PackageNameHolder(ImmutableList.of("hello", "world"));
        assertEquals("hello.world.foo", p1.getQualifiedName("foo"));
        assertEquals("hello/world/foo", p1.getQualifiedPath("foo"));

        final PackageNameHolder p2 = p1.getChildPackage("bar");
        assertEquals("hello.world.bar.foo", p2.getQualifiedName("foo"));
        assertEquals("hello/world/bar/foo", p2.getQualifiedPath("foo"));
    }
}
