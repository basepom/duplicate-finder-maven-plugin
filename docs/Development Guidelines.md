Any pull request that contains integration tests to demonstrate the change and unit tests that test the code changes in isolation **will receive preferred treatment**. If no test cases or integration tests are included, pull request may sit dormant for a longer time.

### Building

Building the plugin requires a JDK7 installed, even if JDK8 or better is the default JDK on the system. It will build explicitly against the JDK7 class library to ensure that the resulting code will execute on any JDK7 or better.

* _Before plugin version 1.3.0_ An environment variable `JAVA7_HOME` must be set before running the build which points at the JDK7 installation. If this variable is not set, the build will fail.
* _Plugin version 1.3.0 and above_ The [Maven toolchains plugin](https://maven.apache.org/plugins/maven-toolchains-plugin/) is used. A `~/.m2/toolchains.xml` file must exist on the local system (as described in the [Guide to using toolchains](https://maven.apache.org/guides/mini/guide-using-toolchains.html)).

### Developing code and sending pull requests

* Use spaces to indent, not hard tab characters. The code base uses four space indents, please respect this.
* Use the US-ASCII charset. If unicode characters are necessary (e.g. for a test case), please use the `\uxxxx` escape syntax.
* Do not reformat an existing file or imports. If an existing file is changed, please format the code similar to the rest of the file.

### Integration test suite

The duplicate finder plugin contains a comprehensive [integration test suite](Writing Integration Tests) to ensure its behavior and catch possible regressions. Any significant code change, feature addition or fix **must** come with a test that fails before the change is applied and passes afterwards.

