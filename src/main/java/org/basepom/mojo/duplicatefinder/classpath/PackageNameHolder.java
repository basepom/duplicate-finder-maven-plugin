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

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the package in which resources and classes reside. Keeps track of the current package when
 * traversing local folders for classes and resources.
 */
class PackageNameHolder {

    private final ImmutableList<String> packages;
    private final String packageName;
    private final String path;

    PackageNameHolder(final List<String> packages) {
        this.packages = ImmutableList.copyOf(checkNotNull(packages, "packages is null"));
        this.packageName = Joiner.on('.').join(packages);
        this.path = Joiner.on('/').join(packages);
    }

    PackageNameHolder() {
        this.packages = ImmutableList.of();
        this.packageName = "";
        this.path = "";
    }

    PackageNameHolder getChildPackage(final String packageName) {
        checkNotNull(packageName, "packageName is null");
        checkArgument(packageName.length() > 0, "package name must have at least one character");

        return new PackageNameHolder(ImmutableList.<String>builder().addAll(packages).add(packageName).build());
    }

    String getQualifiedName(final String className) {
        checkNotNull(className, "className is null");
        return packages.isEmpty() ? className : packageName + "." + className;
    }

    String getQualifiedPath(final String className) {
        checkNotNull(className, "className is null");
        return packages.isEmpty() ? className : path + "/" + className;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + packageName + ")";
    }
}
