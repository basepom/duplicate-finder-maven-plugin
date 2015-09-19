The result file is in XML format. This reference describes version **`1`** of the result file format.

Any reader of a result file **MUST** expect and be able to deal with the documented elements and attributes. A reader **MUST** ignore any element and attribute that it encounters and that is not documented here.

The result file version will increment when there is a backwards incompatible change (e.g. a mandatory field gets removed or a new mandatory field gets added). Backwards compatible changes (e.g. adding a new attribute or element that is optional or removing an attribute that is optional) will not change the version number of the file.

```xml
<?xml version='1.0' encoding='UTF-8'?>
<duplicate-finder-result version="1">
    <project artifactId="..."
             groupId="..."
             version="..."
             classifier="..."
             type="..."/>
    <configuration skip="..."
                   quiet="..."
                   checkCompileClasspath="..."
                   checkRuntimeClasspath="..."
                   checkTestClasspath="..."
                   failBuildInCaseOfDifferentContentConflict="..."
                   failBuildInCaseOfEqualContentConflict="..."
                   failBuildInCaseOfConflict="..."
                   printEqualFiles="..."
                   preferLocal="..."
                   useDefaultResourceIgnoreList="..."
                   useDefaultClassIgnoreList="..."
                   useResultFile="..."
                   resultFileMinClasspathCount="..."
                   resultFile="..."
                   includeBootClasspath="..."
                   bootClasspathProperty="...>
        <ignoredResourcePatterns>
            <ignoredResourcePattern>...</ignoredResourcePattern>
            ...
        </ignoredResourcePatterns>
        <ignoredClassPatterns>
            <ignoredClassPattern>...</ignoredClassPattern>
            ...
        </ignoredClassPatterns>
        <conflictingDependencies>
            <conflictingDependency currentProject="..."
                                   currentProjectIncluded="..."
                                   wildcard="...">
                <dependencies>
                    <dependency artifactId="..."
                                groupId="..."
                                version="..."
                                versionRange="..."
                                classifier="..."
                                type="..."/>
                    ...
                </dependencies>
                <packages>
                    <package>...</package>
                    ...
                </packages>
                <classes>
                    <class>...</class>
                    ...
                </classes>
                <resources>
                    <resource>...</resource>
                    ...
                </resources>
                <resourcePatterns>
                    <resourcePattern>...</resourcePattern>
                    ...
                </resourcePatterns>
            </conflictingDependency>
            ...
        </conflictingDependencies>
        <ignoredDependencies>
            <dependency artifactId="..."
                        groupId="..."
                        version="..."
                        versionRange="..."
                        classifier="..."
                        type="..."/>
            ...
        </ignoredDependencies>
    </configuration>
    <results>
        <result name=".."
                conflictState="..."
                failed="...">
            <conflicts>
                <conflict name="...">
                    <conflictResults>
                        <conflictResult name="..."
                                        type="..."
                                        excepted="..."
                                        failed="..."
                                        printed="..."
                                        conflictState="...">
                            <conflictNames>
                                <conflictName name="...">
                                    <artifact artifactId="..."
                                              groupId="..."
                                              version="..."
                                              versionRange="..."
                                              classifier="..."
                                              type="..."/>
                                </conflictName>
                                <conflictName name="...">
                                    <directory>...</directory>
                                </conflictName>
                                ...
                            </conflictNames>
                        </conflictResult>
                        ...
                    </conflictResults>
                </conflict>
                ...
            </conflicts>
            <ignoredResourcePatterns>
                <ignoredResourcePattern>...</ignoredResourcePattern>
                ...
            </ignoredResourcePatterns>
            <ignoredClassPatterns>
                <ignoredClassPattern>...</ignoredClassPattern>
                ...
            </ignoredClassPatterns>
            <ignoredDirectoryPatterns>
                <ignoredDirectoryPattern>...</ignoredDirectoryPattern>
                ...
            </ignoredDirectoryPatterns>
            <classpathElements type="...">
                <classpathElement name="...">
                    <file>...</file>
                    ...
                </classpathElement>
                ...
            </classpathElements>
            ...
        </result>
    </results>
</duplicate-finder-result>
```

### Conventions

#### Lists of elements

In a number of places, lists of elements are written. These lists follow the maven convention of an enclosing element in plural and a list of elements in singular:

```xml
<packages>
    <package>...</package>
    <package>...</package>
    <package>...</package>
    <package>...</package>
</packages>
```

```xml
<ignoredResourcePatterns>
    <ignoredResourcePattern>...</ignoredResourcePattern>
    <ignoredResourcePattern>...</ignoredResourcePattern>
    <ignoredResourcePattern>...</ignoredResourcePattern>
    <ignoredResourcePattern>...</ignoredResourcePattern>
</ignoredResourcePatterns>
```

A list can be empty, so only the enclosing element is present.

#### Dependency definitions

A dependency is written as a single `dependency` element with attributes:

| Attribute name | Function | Always present |
| -------------- | -------- | -------------- |
| `artifactId`   | artifact id for the dependency | yes |
| `groupId`      | group id for the dependency | yes |
| `version`      | version for the dependency | no |
| `versionRange`      | version for the dependency | no |
| `classifier`   | classifier for the dependency | no |
| `type`         | type for the dependency | yes |

```xml
<dependency artifactId="first-class-jar" groupId="testjar" type="jar"/>
```

`dependency` elements may be grouped as a list, in which case the enclosing element may have a different name.


### `duplicate-finder-result` element

This is the root element.

| Attribute name | Function | Always present |
| -------------- | -------- | -------------- |
| `version`      | result file format, currently `1` | yes |

| Child element name | Function |
| ------------------ | -------- |
| `project`          | Information about the current project |
| `configuration`    | Plugin configuration information |
| `results`          | A list of `result` elements which contain the plugin execution results |

### `project` element

Full name: `duplicate-finder-result.project`

This element contains basic information about the project.

| Attribute name | Function | Always present |
| -------------- | -------- | -------------- |
| `artifactId`   | artifact id from the POM | yes |
| `groupId`      | group id from the POM | yes |
| `version`      | version from the POM | yes |
| `classifier`   | classifier from the POM | yes |
| `type`         | type from the POM | yes |

### `configuration` element

Full name: `duplicate-finder-result.configuration`

Contains the plugin configuration as attributes. All available configuration options are present as attributes. All attributes are always present.

| Attribute name | Type | Notes |
| -------------- | -----| ----- |
| `skip` | boolean | always `false` |
| `quiet` | boolean | |
| `checkCompileClasspath` | boolean | |
| `checkRuntimeClasspath` | boolean | |
| `checkTestClasspath` | boolean | |
| `failBuildInCaseOfDifferentContentConflict` | boolean | |
| `failBuildInCaseOfEqualContentConflict` | boolean | |
| `failBuildInCaseOfConflict` | boolean | |
| `printEqualFiles` | boolean | |
| `preferLocal` | boolean | |
| `useDefaultResourceIgnoreList` | boolean | |
| `useDefaultClassIgnoreList` | boolean | ** Plugin version 1.2.1 + ** |
| `useResultFile` | boolean | always `true` |
| `resultFileMinClasspathCount` | integer | |
| `resultFile` | string | |
| `includeBootClasspath` | boolean | ** Plugin version 1.1.1 + ** |
| `bootClasspathProperty` | string | ** Plugin version 1.1.1 + ** |

| Child element name | Function | Notes |
| ------------------ | -------- | ----- |
| `ignoredResourcePatterns` | A list of `ignoredResourcePattern` elements from the plugin configuration | |
| `ignoredClassPatterns` | A list of `ignoredClassPattern` elements from the plugin configuration  | ** Plugin version 1.2.1 + ** |
| `ignoredDependencies` | `ignoredDependencies` contains a list of `dependency` elements from the plugin configuration | |
| `conflictingDependencies` | Contains a list of `conflictingDependency` elements | |

The `ignoredResourcePattern` list will also contain the deprecated `ignoredResource` elements if they are used in the configuration.

### `conflictingDependency` element

Full name: `duplicate-finder-result.configuration.conflictingDependencies.conflictingDependency`

Reflects a configured conflicting dependency from the configuration.

| Attribute name | Type | Notes |
| -------------- | -----| ----- |
| `currentProject` | boolean | This is the value of the `currentProject` element from a `conflictingDependency` element in the configuration |
| `currentProjectIncluded` | boolean | Reflects whether the current project is present in this conflicting dependencies list. This can be set by either setting the `currentProject` flag or including a dependency that matches the current project. |
| `wildcard` | boolean | `true` if this is a wildcard match (no classes, packages, resources or resourcePattern elements defined. |

| Child element name | Function |
| ------------------ | -------- |
| `dependencies` | Contains a list of `dependency` elements which participate in this conflicting dependency definition. |
| `packages` | A list of package names from the plugin configuration. |
| `classes` | A list of class names from the plugin configuration. |
| `resources` | A list of defined resources from the plugin configuration. |
| `resourcePatterns` | A list of defined resource patterns from the plugin configuration. |

### `result` element

Full name: `duplicate-finder-result.results.result`

Contains a duplicate finder plugin result. As the plugin will evaluate different classpath settings, there may be more than one result.

| Attribute name | Type | Notes |
| -------------- | -----| ----- |
| `name`         | string | The classpath name for this result. Current values are `test`, `compile` and `runtime`. |
| `conflictState` | string | Overall state of this result. Can be `no-conflict`, `content-different` or `content-equal`. |
| `failed` | boolean | `true` if this result failed the overall build, `false` if not. |

| Child element name | Function | Notes |
| ------------------ | -------- | ----- |
| `conflicts` | A list of `conflict` elements describing the conflicts in this result. | |
| `ignoredResourcePatterns` | A list of `ignoredResourcePattern` elements which contain ignored resources for this result. | |
| `ignoredClassPatterns` | A list of `ignoredClassPattern` elements which contain ignored classes for this result.  | ** Plugin version 1.2.1 + ** |
| `ignoredDirectoryPatterns` | A list of `ignoredDirectoryPattern` elements which contain local directory names that were ignored for this result. | |
| `classpathElements` | A list of `classpathElement` elements. | |

The `classpathElements` element can occur multiple times.


### `conflict` element

Full name: `duplicate-finder-result.results.result.conflicts.conflict`

A conflict contains one or more `conflictResult` elements as a list. A conflict has multiple `conflictResult` elements if the same dependencies have multiple conflicts (e.g. two jars contain multiple, different classes with the same name).

| Attribute name | Type | Notes |
| -------------- | -----| ----- |
| `name`         | string | An unique name for a specific conflict. This name is a comma-separated list of the nested `conflictName` element `name` attributes. |


### `conflictResult` element

Full name: `duplicate-finder-result.results.result.conflicts.conflict.conflictResults.conflictResult`

A `conflictResult` element contains a single conflict between multiple class path dependencies.

| Attribute name | Type | Notes |
| -------------- | -----| ----- |
| `name` | string | The class name (for a class) or the resource name for a classpath element. |
| `type` | string | The type of elements listed. Current values are `classes` for classes and `resources` for resources. |
| `excepted` | boolean | `true` if any exception rule has excluded this conflict from failing the build. |
| `failed` | boolean | `true` if this conflict has failed the build. |
| `printed` | boolean | `true` if this conflict was reported as part of the plugin output. |
| `conflictState` | string | The type of conflict. Can be `content-different` or `content-equal`. |

The `conflictResult` element contains a list of `conflictName` elements.

### `conflictName` element

Full name: `duplicate-finder-result.results.result.conflicts.conflict.conflictResults.conflictResult.conflictNames.conflictName`

| Attribute name | Type | Notes |
| -------------- | -----| ----- |
| `name` | string | An unique name for this specific `conflictName` element. |
| `artifact` | boolean | ** Plugin version 1.1.1+ ** True if the element has a nested `artifact` element. |
| `localFolder` | boolean | ** Plugin version 1.1.1+ ** True if the element represents a local project folder. It has either a nested `directory` or `file` element. |
| `bootClasspathElement` | boolean | ** Plugin version 1.1.1+ ** True if the element represents an element from the boot classpath. It has either a nested `directory` or `file` element. |

This element has one of the following elements nested:

* `artifact` describes a classpath artifact which contains the class or resource in conflict. Its attributes are identical to a `dependency` element.
* `directory` is an absolute directory path which contains the class or resource in conflict. This is an absolute path which generally is not portable.
* `file` is an absolute file path which contains the class or resource in conflict. This is an absolute path which generally is not portable.

### `classpathElements` element

Full name: `duplicate-finder-result.results.result.classpathElements`

Contains a list of `classpathElement` elements.

| Attribute name | Type | Notes |
| -------------- | -----| ----- |
| `type`         | string | The type of elements listed. Current values are `classes` for classes and `resources` for resources. |

### `classpathElement` element

Full name: `duplicate-finder-result.results.result.classpathElements.classpathElement`

| Attribute name | Type | Notes |
| -------------- | -----| ----- |
| `name`         | string | The class name (for a class) or the resource name for a classpath element. |

Contains a list of `file` elements which describe the absolute location of a classpath resource which contains the element listed. A `classpathElement` is only present in the output file if its child count is greater or equal to the value of the `resultFileMinClasspathCount` setting in the plugin configuration. The values of the `file` child elements is an absolute path which generally not portable.

