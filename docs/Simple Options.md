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

Skips the plugin execution completely. If the `quiet` flag is `false`,
the message `Skipping duplicate-finder execution!` is output at INFO
level.

If the `quiet` flag is set to `true`, the message is only visible at
DEBUG level if maven was run with the `-X` option.

Maven command line property: `duplicate-finder.skip` (**Plugin version 1.1.1+**)

Default: **false**

### `quiet`

Reduces the amount of output that the plugin generates. Only errors
and warnings are reported.

Maven command line property: `duplicate-finder.quiet` (**Plugin version 1.1.1+**)

Default: **false**

### `checkCompileClasspath`

Check the **compile** classpath for duplicates. This includes
all dependencies in the following maven scopes: `compile`, `provided`,
`system`.

Maven command line property: `duplicate-finder.checkCompileClasspath` (**Plugin version 1.1.1+**)

Default: **true**

### `checkRuntimeClasspath`

Check the **runtime** classpath for duplicates. This includes
all dependencies in the following maven scopes: `compile`, `runtime`.

Maven command line property: `duplicate-finder.checkRuntimeClasspath` (**Plugin version 1.1.1+**)

Default: **true**

### `checkTestClasspath`

Check the **test** classpath for duplicates. This includes all
dependencies in any maven scopes.

This is the most comprehensive check because it includes any maven scope. It is very rare to have duplicates on the `runtime` or `compile` classpath and not on the `test` classpath.

Maven command line property: `duplicate-finder.checkTestClasspath` (**Plugin version 1.1.1+**)

Default: **true**

### `failBuildInCaseOfDifferentContentConflict`

Fail the build if any class or resource on the classpath is duplicate
and their SHA256 hash is different.

Maven command line property: `duplicate-finder.failBuildInCaseOfDifferentContentConflict` (**Plugin version 1.1.1+**)

Default: **false**

### `failBuildInCaseOfEqualContentConflict`

Fail the build if any class or resource on the classpath is duplicate
and their SHA256 hash is equal. Setting this flag to **true** also
reports any equal file equivalent to `printEqualFile`.

Maven command line property: `duplicate-finder.failBuildInCaseOfEqualContentConflict` (**Plugin version 1.1.1+**)

Default: **false**

### `failBuildInCaseOfConflict`

Fail the build if any class or resource on the classpath is duplicate
and their SHA256 hash is different.

Maven command line property: `duplicate-finder.failBuildInCaseOfConflict` (**Plugin version 1.1.1+**)

Default: **false**

### `printEqualFiles`

Report files that exist multiple times on the classpath even if their
SHA256 hash is equal.

Maven command line property: `duplicate-finder.printEqualFiles` (**Plugin version 1.1.1+**)

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

Maven command line property: `duplicate-finder.preferLocal` (**Plugin version 1.1.1+**)

Default: **true**

