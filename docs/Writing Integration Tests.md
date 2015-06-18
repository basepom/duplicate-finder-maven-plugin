The duplicate finder plugin comes with a comprehensive integration test suite to ensure its behavior.

Integration tests are executed using the [Maven Invoker Plugin](http://maven.apache.org/plugins/maven-invoker-plugin/integration-test-mojo.html). The test suite comes with a set of tools that makes writing integration tests simple.

### Writing an integration test for the duplicate finder plugin

Integration tests are located in the [`src/it`](https://github.com/basepom/duplicate-finder-maven-plugin/tree/master/src/it) folder of the source tree. Any directory that starts with `test-` contains an integration test. All integration tests are always executed as part of the plugin build process.

Any integration test directory **must** contain at least three files:

* `pom.xml` - The project POM, which must inherit from the base POM
* `invoker.properties` - The goals to invoke and the expected outcome.
* `verify.groovy` - The result verification script. All integration tests use groovy.

Tests that add code, compile classes etc. may have more files.

#### The integration test POM

Any integration test must inherit from the base POM which sets up the plugin correctly. That ensures that any global change in the future will be picked up by all unit tests.

The POM for any integration test should look like this:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>@project.groupId@.@project.artifactId@</groupId>
        <artifactId>basepom</artifactId>
        <version>1.0.under-test</version>
    </parent>

    <artifactId>... the artifact id of the test ...</artifactId>
    <description>... brief description what the test tries to accomplish ...</description>

... additional POM elements ...
</project>
```

Any integration test must use the name of its directory as the artifactId. As integration test directory names must start with `test-`, any integration test artifactId must be `test-....`.

#### The `invoker.properties` file

All integration tests should contain almost identical `invoker.properties` files:

```properties
invoker.goals=clean verify
invoker.buildResult = success
```

for a successful plugin execution

```properties
invoker.goals=clean verify
invoker.buildResult = failure
```

for an unsuccessful plugin execution.

The integration test should still write a duplicate plugin result file which then is verified using the `verify.groovy` script

#### The `verify.groovy` script

Almost all integration tests will use the result file output after a plugin run to check the outcome. The integration test suite contains a library which makes these checks reasonably simple.

The `ITools` library is based on the [XmlSlurper](http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlSlurper) library; additional processing can be done with the tools listed there.

The source code for the `ITools` library [is in the main source tree](https://github.com/basepom/duplicate-finder-maven-plugin/blob/master/src/test/groovy/org/basepom/mojo/duplicatefinder/ITools.groovy). Also, the existing integration tests serve as examples on how to write tests and verify plugin results.

Any `verify.groovy` script should start with

```groovy
import static org.basepom.mojo.duplicatefinder.groovy.ITools.*

def result = loadTestXml(basedir)
...
```

This loads the result file information for the `test` classpath scope into the `result` variable.

Any test that needs to validate from a different scope or must access elements that are not enclosed by a `result` element need to use

```groovy
def (result, xml) = loadXmlAndResult(basedir, "runtime")
```

Here the `xml` variable will contain the full result file element tree and `result` will contain the test result for the `runtime` scope.


Checking the general outcome of an integration test:

```groovy
overallState(conflictState, count, failState, result)
```

* `conflictState` can be `NO_CONFLICT`, `CONFLICT_EQUAL` or `CONFLICT_DIFF` (constants are defined in the `ITools` library)
* `failState` can be `FAILED` or `NOT_FAILED`  (constants are defined in the `ITools` library)
* `count` is the number of elements in conflict state. Note that multiple conflicts between the same elements (e.g. the `first-jar` and `second-jar`) only count as one.
* `result` is the value returned by `loadTestXml` or `loadXmlAndResult` or `loadXml`.

Do a conflict check:

```groovy
checkConflictResult(conflictName, conflictType, conflictState, excepted, printed, failState, conflictResult)
```

* `conflictName` is the class or resource name that is in conflict.
* `conflictType` can be `TYPE_CLASS` or `TYPE_RESOURCE` (constants are defined in the `ITools` library)
* `conflictState` can be `NO_CONFLICT`, `CONFLICT_EQUAL` or `CONFLICT_DIFF` (constants are defined in the `ITools` library)
* `excepted` can be `NOT_EXCEPTED` or `EXCEPTED` (constants are defined in the `ITools` library). An `excepted` conflict is covered by an `<exception>` element from the configuration.
* `printed` can be `NOT_PRINTED` or `PRINTED` (constants are defined in the `ITools` library). A `PRINTED` conflict was reported on the command line.
* `failState` can be `FAILED` or `NOT_FAILED`  (constants are defined in the `ITools` library). A `FAILED` conflict also failed the build.

The following tools help build the `conflictResult` value:

```groovy
findConflictResult(result, String ... matches)

findConflictResult(result, count, String ... matches)
```

* `result` is the value returned by `loadTestXml` or `loadXmlAndResult` or `loadXml`.
* `count` is the number of expected result elements that match all of the `matches` elements. If omitted, *1* is assumed.
* `matches` A list of element names that match the test results.

* For the predefined jars, the *Match name* (see below) can be used.
* For a match from the local project, the `projectTargetFolder(basedir)` and `projectTargetTestFolder(basedir)` tools can be used.


#### Jars for duplication checks

As the duplicate finder plugin deals with a classpath elements, there are a number of pre-built jars that can be used in an integration test:

| Maven GAV coordinates | Contents | Match name | Usage |
| --------------------- | -------- | ---------- | ----- |
| `testjar:first-class-jar:1.0.under-test` | `diff.Demo` class | `FIRST_CLASS_JAR` | Use with `first-diff-jar` to find classpath class duplicates with different SHA |
| `testjar:second-class-jar:1.0.under-test` | `demo.Demo` class | `SECOND_CLASS_JAR` | Use with `second-equal-jar` to find classpath class duplicates with same SHA |
| `testjar:first-diff-jar:1.0.under-test` | `diff.Demo` class, different SHA from `first-class-jar` | `FIRST_DIFF_JAR` | |
| `testjar:second-equal-jar:1.0.under-test` | `demo.Demo` class, same SHA as `second-class-jar` | `SECOND_EQUAL_JAR` | |
| `testjar:first-jar:1.0.under-test` | `conflict-same-content` and `conflict-different-content` resources | `FIRST_JAR` | Use with `second-jar` to find classpath resource duplicates with the same or different SHA values. |
| `testjar:second-jar:1.0.under-test` |`conflict-same-content` with the same SHA and `conflict-different-content` with a different SHA from `first-jar` | `SECOND_JAR` | |

