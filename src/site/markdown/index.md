A maven plugin to find and flag duplicate and conflicting classes and resources on the java classpath.

Eliminating any duplicates from the classpath improves the stability of a maven build and makes the runtime behavior deterministic. Exposing duplicates will expose possible problems or conflicts with dependencies in a project.

## About

This plugin is a friendly fork (same [main authors](https://github.com/basepom/duplicate-finder-maven-plugin/blob/main/NOTICE.txt)) of the [Ning maven-duplicate-finder plugin](https://github.com/ning/maven-duplicate-finder-plugin). It is mostly configuration compatible to the ning plugin; the only change required is the group and artifact id of the plugin itself.

## Requirements

The plugins requires Apache Maven 3.x.x.

| Version           | Minimum Java version |
|-------------------|----------------------|
| up to 1.2.0       | Java 6 or better     |
| 1.2.0 up to 1.3.0 | Java 7 or better     |
| 1.3.0 up to 2.0.0 | Java 8 or better     |
| 2.0.0             | Java 11 or better    |

## Goals

The plugin has two goals:

* `duplicate-finder:check` - the main goal of the plugin. Runs duplicate check on the maven classpaths.
* `duplicate-finder:help` - displays standard maven plugin help information.

## Configuration overview

The duplicate-finder plugins supports a number of configuration settings:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.basepom.maven</groupId>
      <artifactId>duplicate-finder-maven-plugin</artifactId>
      <version>2.0.0</version>
      <executions>
        <execution>
          <id>default</id>
          <phase>verify</phase>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
      <configuration>
        <printEqualFiles>false</printEqualFiles>
        <failBuildInCaseOfDifferentContentConflict>false</failBuildInCaseOfDifferentContentConflict>
        <failBuildInCaseOfEqualContentConflict>false</failBuildInCaseOfEqualContentConflict>
        <failBuildInCaseOfConflict>false</failBuildInCaseOfConflict>
        <checkCompileClasspath>true</checkCompileClasspath>
        <checkRuntimeClasspath>true</checkRuntimeClasspath>
        <checkTestClasspath>true</checkTestClasspath>
        <skip>false</skip>
        <preferLocal>true</preferLocal>
        <useResultFile>true</useResultFile>
        <resultFileMinClasspathCount>2</resultFileMinClasspathCount>
        <resultFile>${project.build.directory}/duplicate-finder-result.xml</resultFile>

        <!-- removed in 2.0.0 -->
        <quiet>false</quiet>
        <!-- removed in 2.0.0 -->

        <!-- Version 1.1.1+ -->
        <useDefaultResourceIgnoreList>true</useDefaultResourceIgnoreList>
        <!-- Version 1.1.1+ -->

        <!-- Version 1.1.1+, remove in 2.0.0 -->
        <includeBootClasspath>false</includeBootClasspath>
        <bootClasspathProperty>sun.boot.class.path</bootClasspathProperty>
        <!-- Version 1.1.1+, remove in 2.0.0 -->

        <!-- Version 1.2.0+ -->
        <includePomProjects>false</includePomProjects>
        <!-- Version 1.2.0+ -->

        <!-- Version 1.2.1+ -->
        <useDefaultResourceIgnoreList>true</useDefaultResourceIgnoreList>
        <!-- Version 1.2.1+ -->

        <exceptions>
          <exception>
            <currentProject>false</currentProject>

            <!-- Version 1.1.1+, remove in 2.0.0 -->
            <bootClasspath>false</bootClasspath>
            <!-- Version 1.1.1+, remove in 2.0.0 -->

            <conflictingDependencies>
              <dependency>
                <artifactId>...</artifactId>
                <groupId>...</groupId>
                <version>...</version>
                <type>...</type>
                <classifier>...</classifier>
              </dependency>
              ...
            </conflictingDependencies>
            <classes>
              <class>...</class>               
            </classes>
            <packages>
              <package>...</package>
            </packages>
            <resources>
              <resource>...</resource>
            </resources>
            <resourcePatterns>
              <resourcePattern>...</resourcePattern>
            </resourcePatterns>
          </exception>
        </exceptions>
        <ignoredResourcePatterns>
          <ignoredResourcePattern>...</ignoredResourcePattern>
        </ignoredResourcePatterns>

        <!-- Version 1.2.1+ -->
        <ignoredClassPatterns>
          <ignoredClassPattern>...</ignoredClassPattern>
        </ignoredClassPatterns>
        <!-- Version 1.2.1+ -->

        <ignoredDependencies>
          <dependency>
            <artifactId>...</artifactId>
            <groupId>...</groupId>
            <version>...</version>
            <type>...</type>
            <classifier>...</classifier>
          </dependency>
        </ignoredDependencies>
      </configuration>
    </plugin>
  </plugins>
</build>
```
