Managing exceptions is an important aspect for the duplicate finder
plugin. Often, third-party dependencies, that can not be changed have
conflicts or include classes and resources that are duplicate with
other classpath elements.

The duplicate finder plugin allows listing and excluding these
conflicts.


```xml
<configuration>
    <exceptions>
        <exception>
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
            <currentProject>false</currentProject>

            <!-- Version 1.1.1+, removed in 2.0.0 -->
            <bootClasspath>false</bootClasspath>
            <!-- Version 1.1.1+, removed in 2.0.0 -->

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
        </exception>
    </exceptions>
</configuration>
```

### Exception overview

Within the plugin configuration, one or more exceptions can be listed
in the `<exceptions>` element. Each exception is applied to all
classpath scopes (compile, runtime and test).

Each exception lists its conflicting dependencies in the `conflictingDependencies`
element and then optionally the specific conflicting classes and
resources. Any conflict covered by an exception will not be reported
or fail the build.

If an exception only contains a `conflictingDependencies` element (no
`classes`, `packages`, `resources` or `resourcePatterns`), *any*
duplicate between all the listed dependencies is ignored. If any of
these elements is present, *only* the listed classes or resources will
be ignored.

**Warning!** The presence of any of these elements will turn off the
  wildcard behavior of an exception. Especially the presence of a
  `classes` or `packages` element will also turn off the wildcard
  behavior for resources and the presence of a `resources` or
  `resourcePatterns` element will turn off the wildcard behavior for
  classes. This was undefined in previous versions of the duplicate
  finder plugin.

#### The `conflictingDependencies` element

The `conflictingDependencies` element lists all artifacts considered
for an exception as [`dependency`](describing_dependencies.html) elements. Each
`dependency` can be fully or partially defined.

#### The `currentProject` flag

The `currentProject` flag signals that the current project should be
added to the exception as a dependency. Any resource or class that is
duplicate between the current project and the listed dependencies will
be ignored.

The option is equivalent to having the current project listed as a
dependency in the `conflictingDependencies` element.

If the `conflictingDependencies` element already contains a dependency
for the current project, the flag has no effect.

**Warning!** This flag can lead to surprising behavior, especially if
  it is used in the root POM of a multi-module build or a parent
  pom. The 'current project' is evaluated at runtime and in a
  multi-module build it will be applied each time when the plugin is
  executed on a sub-project. It is recommended to use this flag
  specifically in the project POM where conflicts are expected.

##### Examples

In a mult-module build with two sub-projects, sub-project
`sub-project-a` has a conflict with `commons-lang:commons-lang` and
`sub-project-b` has a conflict with
`commons-collections:commons-collections`. It would be possible to
configure the duplicate finder plugin to ignore these by adding
exceptions to the project root POM:

```xml
<project>
  <modules>
    <module>sub-project-a</module>
    <module>sub-project-b</module>
  </modules>

  <build>
    <plugins>
      <plugin>
        <groupId>org.basepom.maven</groupId>
        <artifactId>duplicate-finder-maven-plugin</artifactId>
        <configuration>
          <exceptions>
            <exception>
              <currentProject>true</currentProject>
              <conflictingDependencies>
                <dependency>
                  <groupId>commons-lang</groupId>
                  <artifactId>commons-lang</artifactId>
                </dependency>
              </conflictingDependencies>
            </exception>
            <exception>
              <currentProject>true</currentProject>
              <conflictingDependencies>
                <dependency>
                  <groupId>commons-collections</groupId>
                  <artifactId>commons-collections</artifactId>
                </dependency>
              </conflictingDependencies>
            </exception>
          </exceptions>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

In this case, each of the sub projects (`sub-project-a` and
`sub-project-b`) will have any possible conflicts to
`commons-lang:commons-lang` and
`commons-collections:commons-collections` excluded. However, this
would also exclude possible conflicts between `sub-project-a` and
`commons-collections:commons-collections` or `sub-project-b` and
`commons-lang:commons-lang` even though each probably should have been
flagged.

A better way to solve this is moving the exceptions to the actual
sub-project POMs:

Root POM:

```xml
<project>
  <modules>
    <module>sub-project-a</module>
    <module>sub-project-b</module>
  </modules>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.basepom.maven</groupId>
          <artifactId>duplicate-finder-maven-plugin</artifactId>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

Sub-project POM `sub-project-a`:

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>org.basepom.maven</groupId>
        <artifactId>duplicate-finder-maven-plugin</artifactId>
        <configuration>
          <exceptions>
            <exception>
              <currentProject>true</currentProject>
              <conflictingDependencies>
                <dependency>
                  <groupId>commons-lang</groupId>
                  <artifactId>commons-lang</artifactId>
                </dependency>
              </conflictingDependencies>
            </exception>
          </exceptions>
        </configuration>
      </plugin>
    </plugins>
    </build>
</project>
```

Sub-project POM `sub-project-b`:

```xml
<project>
  <build>
    <plugins>
      <plugin>
        <groupId>org.basepom.maven</groupId>
        <artifactId>duplicate-finder-maven-plugin</artifactId>
        <configuration>
          <exceptions>
            <exception>
              <currentProject>true</currentProject>
              <conflictingDependencies>
                <dependency>
                  <groupId>commons-collections</groupId>
                  <artifactId>commons-collections</artifactId>
                </dependency>
              </conflictingDependencies>
            </exception>
          </exceptions>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

Now, `sub-project-a` will have its conflicts with
`commons-lang:commons-lang` excluded and `sub-project-b` will have its
conflicts with `commons-collections:commons-collections` excluded
while any other conflict will still be flagged correctly.

#### The `bootClasspath` flag

** Available in plugin version 1.1.1, removed in 2.0.0! **

_This flag only works up to JDK 8! As 2.0.0 no longer supports JDK 8, it was deprecated._

The `bootClasspath` flag signals that an exception to elements on the
boot classpath is defined. Any match between classes or resources in
the listed artifacts and the boot classpath elements is ignored.

This flag can be combined with the `currentProject` flag.

```xml
<exception>
    <currentProject>false</currentProject>
    <bootClasspath>false</bootClasspath>
</exception>
```

will ignore any conflict between classes and resources in the current
project and the boot classpath.

#### Managing class exceptions with  `classes` and `packages`

The `classes` and `packages` elements control which classes are
ignored by the duplicate finder plugin when determining conflicts.

Specific classes are listed as fully-qualified names in the `classes`
element and full packages are listed in the `packages` element. Any
package listed will match any class in that package *and any
sub-package*. This is a very powerful mechanism and it should be used
with care.

##### Examples

Exclude a set of classes that are copied around into different jars:

```xml
<exception>
  <conflictingDependencies>
    <dependency>
      <groupId>commons-beanutils</groupId>
      <artifactId>commons-beanutils</artifactId>
    </dependency>
    <dependency>
      <groupId>commons-beanutils</groupId>
      <artifactId>commons-beanutils-core</artifactId>
    </dependency>
    <dependency>
      <groupId>commons-collections</groupId>
      <artifactId>commons-collections</artifactId>
    </dependency>
  </conflictingDependencies>
    <!-- Well done, Apache! -->
  <classes>
    <class>org.apache.commons.collections.ArrayStack</class>
    <class>org.apache.commons.collections.Buffer</class>
    <class>org.apache.commons.collections.BufferUnderflowException</class>
    <class>org.apache.commons.collections.FastHashMap</class>
  </classes>
</exception>
```

Exclude a package that was copied into a jar:

```xml
<exception>
  <conflictingDependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>(,4.11)</version>
    </dependency>
    <dependency>
      <groupId>org.hamcrest</groupId>
      <artifactId>hamcrest-core</artifactId>
    </dependency>
  </conflictingDependencies>
  <packages>
    <package>org.hamcrest</package>
  </packages>
</exception>
```

#### Manging resource exceptions with `resources` and `resourcePatterns`

The `resources` and `resourcePatterns` elements control which
resources are ignored. Similar to the class elements `classes` and
`packages`, `resource` lists single resource elements and
`resourcePatterns` can match one or many resources.

A specific resource is listed with its full path in the `resources`
element. Any resource listed should be fully qualfied and start with a
`/`. For backwards compatibility, relative resource names (without a
leading `/`) are accepted but internally treated like absolute
resource names.

The `resourcePatterns` element accepts any standard Java regular
expression. Resource names are matched against these expressions. If
relative matching is not desired, a pattern must be anchored with
either a a start anchor (`^`) or an end anchor (`$`).

##### Examples

Exclude a specific resource that is duplicate in the Jersey jars:

```xml
<exception>
  <conflictingDependencies>
    <dependency>
      <groupId>com.sun.jersey</groupId>
      <artifactId>jersey-core</artifactId>
    </dependency>
    <dependency>
      <groupId>com.sun.jersey</groupId>
      <artifactId>jersey-client</artifactId>
    </dependency>
    <dependency>
      <groupId>com.sun.jersey</groupId>
      <artifactId>jersey-server</artifactId>
    </dependency>
  </conflictingDependencies>
  <resources>
    <resource>/META-INF/jersey-module-version</resource>
  </resources>
</exception>
```
