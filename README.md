# duplicate-finder Maven plugin

A maven plugin to find and flag duplicate classes and resources on the java classpath.

This ensures stability and reproducability of a maven build and will flag possible problems or conflicts with dependencies in a project.

## About

This plugin is a friendly fork (same main authors) of the [Ning maven-duplicate-finder plugin](https://github.com/ning/maven-duplicate-finder-plugin). It is configuration compatible to the ning plugin; the only change required is the group and artifact id of the plugin itself.

## Requirements

The plugins requires Maven 3.x.x and Java 6 or better.

## Goals

The plugin has two goals:

* `duplicate-finder:check` - the main goal of the plugin. Runs duplicate check on the maven classpaths.
* `duplicate-finder:help` - displays standard maven plugin help information.

## Usage

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.basepom.maven</groupId>
            <artifactId>duplicate-finder-maven-plugin</artifactId>
            <version>1.1.0</version>
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
                <useDefaultResourceIgnoreList>true</useDefaultResourceIgnoreList>
                <checkCompileClasspath>true</checkCompileClasspath>
                <checkRuntimeClasspath>true</checkRuntimeClasspath>
                <checkTestClasspath>true</checkTestClasspath>
                <skip>false</skip>
                <quiet>false</quiet>
                <preferLocal>true</preferLocal>
                <useResultFile>true</useResultFile>
                <resultFileMinClasspathCount>2</resultFileMinClasspathCount>
                <resultFile>${project.build.directory}/duplicate-finder-result.xml</resultFile>
                <ignoredResources>
                    <ignoredResource>...</ignoredResource>
                </ignoredResources>
                <exceptions>
                    <exception>
                        <conflictingDependencies>
                            <dependency>
                                <artifactId>...</artifactId>
                                <groupId>...</groupId>
                                <version>...</version>
                                <versionRange>...</versionRange>
                                <type>...</type>
                                <classifier>...</classifier>
                            </dependency>
                            ...
                        <conflictingDependencies>
                        <resourcePatterns>
                            <resourcePattern>...</resourcePattern>
                        </resourcePatterns>
                        <classes>
                            <class>...</class>
                        </classes>
                        <packages>
                            <package>...</package>
                        </packages>
                        <resources>
                            <resource>...</resource>
                        </resources>
                </conflictingDependencies>
                <ignoredDependencies>
                    <dependency>
                        <artifactId>...</artifactId>
                        <groupId>...</groupId>
                        <version>...</version>
                        <versionRange>...</versionRange>
                        <type>...</type>
                        <classifier>...</classifier>
                    </dependency>
                </ignoredDependencies>
            </configuration>
        </plugin>
    </plugins>
</build>
```
