These options control aspects of the plugin execution.

```xml
<configuration>
    <skip>false</skip>
    <quiet>false</quiet>
    <checkCompileClasspath>true</checkCompileClasspath>
    <checkRuntimeClasspath>true</checkRuntimeClasspath>
    <checkTestClasspath>true</checkTestClasspath>
    <failBuildInCaseOfDifferentContentConflict>false</failBuildInCaseOfDifferentContentConflict>
    <failBuildInCaseOfEqualContentConflict>false</failBuildInCaseOfEqualContentConflict>
    <failBuildInCaseOfConflict>false</failBuildInCaseOfConflict>
    <printEqualFiles>false</printEqualFiles>
    <preferLocal>true</preferLocal>
</configuration>
```

### `skip`

Skips the plugin execution completely. No other option is evaluated.

Default: **false**

### `quiet`

Reduces the amount of output that the plugin generates. Only errors
and warnings are reported.

Default: **false**

### `checkCompileClasspath`

Check the **compile** classpath for duplicates. This includes
all dependencies in the following maven scopes: `compile`, `provided`,
`system`.

Default: **true**

### `checkRuntimeClasspath`

Check the **runtime** classpath for duplicates. This includes
all dependencies in the following maven scopes: `compile`, `runtime`.

Default: **true**

### `checkTestClasspath`

Check the **test** classpath for duplicates. This includes all
dependencies in any maven scopes.

This is the most comprehensive check because it includes any maven scope. It is very rare to have duplicates on the `runtime` or `compile` classpath and not on the `test` classpath.

Default: **true**

### `failBuildInCaseOfDifferentContentConflict`

Fail the build if any class or resource on the classpath is duplicate
and their SHA256 hash is different.

Default: **false**

### `failBuildInCaseOfEqualContentConflict`

Fail the build if any class or resource on the classpath is duplicate
and their SHA256 hash is equal. Setting this flag to **true** also
reports any equal file equivalent to `printEqualFile`.

Default: **false**

### `failBuildInCaseOfConflict`

Fail the build if any class or resource on the classpath is duplicate
and their SHA256 hash is different.

Default: **false**

### `printEqualFiles`

Report files that exist multiple times on the classpath even if their
SHA256 hash is equal.

Default: **false**

### `preferLocal`

This element is only relevant for multi-module builds. If it is set to **true**, 
the plugin will prefer the local sub-modules (using their `target/classes` and
`target/test-classes` folders) in the current build over matching artifacts 
from the local or remote artifact repository.

This is almost always the preferred setting because it ensures that
the latest (current) build of other sub-modules is checked. The only
real use case for setting this flag to `false` is when building each
sub-module separately (not using the full multi-module build) and 
installing the resulting artifacts into the local repository.

Default: **true**

