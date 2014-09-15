# Maven duplicate finder plugin

## Major changes from Version 1.0.x

Starting with version 1.1.0 of the duplicate finder plugin, Maven 3.x and Java 6 or better are **required**.

Also, the plugin artifact id has been changed to *duplicate-finder-maven-plugin* as is required with Maven 3.x (and probably enforced at some point).

If upgrading to version 1.1.0 or newer is not possible, use

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>maven-duplicate-finder-plugin</artifactId>
  <version>1.0.8</version>
</plugin>
```

## What is it

The `duplicate-finder-maven-plugin` is a plugin that will search for classes with the same name, as well as resources with the same path,
in the classpaths of a maven project. More specifically, it will check the compile, runtime, and test classpaths for

* Classes with the same qualified name in the current project and all dependencies relevant for that classpath
* Files that are not `class` files, with the same resource path (i.e. as if it would be accessed via the classloader) in the current project and all dependencies relevant for that classpath
 (Note that at the moment, the plugin does not check if the files are actually the same or not, it only looks for the same file/class name.)

## How to use it

### Configuration

To make commandline usage a bit easier, you should add the
`com.ning.maven.plugins` group to the `pluginGroups` section in your settings file:

    <settings>
      ...
      <pluginGroups>
        <pluginGroup>com.ning.maven.plugins</pluginGroup>
      </pluginGroups>
      ...
    </settings>

### Running it

Usually, the plugin does not need to be configured in the POM. You can simply execute it via maven command line in a directory that contains a POM
(which can be a multi-module POM):

```bash
mvn duplicate-finder:check
```

This will check for the latest version of the plugin, download it if necessary, and then execute it. Note that you might get snapshot versions this way.
You can also run a specific version using the fully qualified plugin identifier:

```bash
    mvn com.ning.maven.plugins:duplicate-finder-maven-plugin:1.1.0:check
```

If you want to include it in the normal build, configure it like this:

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>duplicate-finder-maven-plugin</artifactId>
  <executions>
    <execution>
      <phase>verify</phase>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

A successful run will look like this:

```
[INFO] [duplicate-finder:check {execution: default-cli}]
[INFO] Checking compile classpath
[INFO] Checking runtime classpath
[INFO] Checking test classpath
```

If you run the plugin on its own source code, you'll instead get something like

```
[WARNING] Found duplicate and different classes in [commons-logging:commons-logging:1.1.1,commons-logging:commons-logging-api:1.1] :
[WARNING]   org.apache.commons.logging.Log
[WARNING]   org.apache.commons.logging.LogConfigurationException
[WARNING]   org.apache.commons.logging.LogFactory
[WARNING]   org.apache.commons.logging.LogSource
[WARNING]   org.apache.commons.logging.impl.Jdk14Logger
[WARNING]   org.apache.commons.logging.impl.LogFactoryImpl
[WARNING]   org.apache.commons.logging.impl.NoOpLog
[WARNING]   org.apache.commons.logging.impl.SimpleLog
[WARNING]   org.apache.commons.logging.impl.WeakHashtable
[WARNING] Found duplicate and different resources in [org.springframework:spring-aop:3.0.3.RELEASE,org.springframework:spring-beans:3.0.3.RELEASE,org.springframework:spring-context:3.0.3.RELEASE,org.springframework:spring-core:3.0.3.RELEASE] :
[WARNING]   overview.html
[WARNING] Found duplicate and different resources in [org.springframework:spring-aop:3.0.3.RELEASE,org.springframework:spring-beans:3.0.3.RELEASE,org.springframework:spring-context:3.0.3.RELEASE] :
[WARNING]   META-INF/spring.tooling
```

The above example shows a duplicate resource and a duplicate class in the current POM. In either case, it will print the qualified names of all artifacts that
have these classes/resources - this includes the current project. To make the output a bit easier to read, all duplicate classes are grouped by the set of
artifacts that have it. E.g. in the example, the `org.apache.commons.logging.*` classes are all found in two particular artifacts, so they are all
combined in one group.

The plugin can also be configured to fail the build (see below), in which case it will print an additional

```
[ERROR] Found duplicate classes/resources
```

### Configuring it

In some cases you need to configure the plugin to make exceptions, i.e. allow duplicate classes or resources for specific artifacts. For instance, multiple
dependencies might contain base XML classes in the `javax.xml` and `org.w3c` packages. If you are sure that these are the same and thus the
duplication can be ignored, you can define exceptions like this:

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>duplicate-finder-maven-plugin</artifactId>
  <version>1.1.0</version>
  <configuration>
    <exceptions>
      <exception>
        <conflictingDependencies>
          <dependency>
            <groupId>jaxen</groupId>
            <artifactId>jaxen</artifactId>
            <version>1.1.1</version>
          </dependency>
          <dependency>
            <groupId>xml-apis</groupId>
            <artifactId>xml-apis</artifactId>
            <version>[1.3.02,1.3.03]</version>
          </dependency>
        </conflictingDependencies>
        <classes>
          <class>org.w3c.dom.UserDataHandler</class>
        </classes>
      </exception>
      <exception>
        <conflictingDependencies>
          <dependency>
            <groupId>xerces</groupId>
            <artifactId>xercesImpl</artifactId>
            <version>2.6.2</version>
          </dependency>
          <dependency>
            <groupId>xml-apis</groupId>
            <artifactId>xml-apis</artifactId>
            <version>[1.3.02,1.3.03]</version>
          </dependency>
        </conflictingDependencies>
        <packages>
          <package>org.w3c.dom.ls</package>
        </packages>
      </exception>
      <exception>
        <conflictingDependencies>
          <dependency>
            <groupId>xerces</groupId>
            <artifactId>xmlParserAPIs</artifactId>
            <version>2.6.2</version>
          </dependency>
          <dependency>
            <groupId>xml-apis</groupId>
            <artifactId>xml-apis</artifactId>
            <version>[1.3.02,1.3.03]</version>
          </dependency>
          <dependency>
            <groupId>xmlrpc</groupId>
            <artifactId>xmlrpc</artifactId>
            <version>2.0</version>
          </dependency>
        </conflictingDependencies>
        <classes>
          <class>org.apache.xmlcommons.Version</class>
        </classes>
        <packages>
          <package>javax.xml.parsers</package>
          <package>javax.xml.transform</package>
          <package>org.w3c.dom</package>
          <package>org.xml.sax</package>
        </packages>
      </exception>
      <exception>
        <conflictingDependencies>
          <dependency>
            <groupId>stax</groupId>
            <artifactId>stax-api</artifactId>
            <version>1.0.1</version>
          </dependency>
          <dependency>
            <groupId>xml-apis</groupId>
            <artifactId>xml-apis</artifactId>
            <version>[1.3.02,1.3.03]</version>
          </dependency>
        </conflictingDependencies>
        <classes>
          <class>javax.xml.XMLConstants</class>
          <class>javax.xml.namespace.NamespaceContext</class>
          <class>javax.xml.namespace.QName</class>
        </classes>
      </exception>
      <exception>
        <conflictingDependencies>
          <dependency>
            <groupId>xalan</groupId>
            <artifactId>xalan</artifactId>
            <version>2.6.0</version>
          </dependency>
          <dependency>
            <groupId>xml-apis</groupId>
            <artifactId>xml-apis</artifactId>
            <version>[1.3.02,1.3.03]</version>
          </dependency>
        </conflictingDependencies>
        <packages>
          <package>org.w3c.dom.xpath</package>
        </packages>
      </exception>
    </exceptions>
  </configuration>
  <executions>
    <execution>
      <phase>verify</phase>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### Ignoring dependencies

In some rare cases, you want to completely remove a dependency from the check. For instance, in the above example, it might be better to completely ignore
the `xml-apis:xml-apis` dependencies:

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>duplicate-finder-maven-plugin</artifactId>
  <version>1.1.0</version>
  <configuration>
    <ignoredDependencies>
      <dependency>
        <groupId>xml-apis</groupId>
        <artifactId>xml-apis</artifactId>
        <version>1.3.02</version>
      </dependency>
      <dependency>
        <groupId>xml-apis</groupId>
        <artifactId>xml-apis</artifactId>
        <version>1.3.03</version>
      </dependency>
    </ignoredDependencies>
    ...
  </configuration>
</plugin>
```

*Please use this only if absolutely necessary!*

### Allowing classes

```xml
<exception>
  <conflictingDependencies>
    <dependency>
      <groupId>xerces</groupId>
      <artifactId>xmlParserAPIs</artifactId>
      <version>2.6.2</version>
    </dependency>
    <dependency>
      <groupId>xml-apis</groupId>
      <artifactId>xml-apis</artifactId>
      <version>[1.3.02,1.3.03]</version>
    </dependency>
    <dependency>
      <groupId>xmlrpc</groupId>
      <artifactId>xmlrpc</artifactId>
      <version>2.0</version>
    </dependency>
  </conflictingDependencies>
  <classes>
    <class>org.apache.xmlcommons.Version</class>
  </classes>
  <packages>
    <package>javax.xml.parsers</package>
    <package>javax.xml.transform</package>
    <package>org.w3c.dom</package>
    <package>org.xml.sax</package>
  </packages>
</exception>
```

The `conflictingDependencies` section states the artifacts for which duplicate class conflicts are allowed. Note that in a particular project, not all of
these artifacts have to participate. E.g. for the above example, a duplicate class for class `org.apache.xmlcommons.Version` between artifacts
`xerces:xmlParserAPIs:2.6.2` and `xml-apis:xml-apis:1.3.03` would be matched even if `xmlrpc:xmlrpc` is not involved.

The `version` identifier uses the normal [Maven version specification](http://docs.codehaus.org/display/MAVEN/Dependency+Mediation+and+Conflict+Resolution).

*Please use version ranges only if you are sure that all of these versions are fine.*

You can specify the classes via direct `class` elements (e.g. for `org.apache.xmlcommons.Version` in the above section), or via
`packages` elements. Note that a `<package>org.xml.sax</package>` element will match the `org.xml.sax` package *plus* all sub packages.

*You want to be as exact as possible. Use packages only if necessary, and use the most specific package.*

### Allowing resources

Adding exceptions for resources works the same way:

```xml
<exception>
  <conflictingDependencies>
    ...
  </conflictingDependencies>
  <resources>
    <resource>log4j.xml</resource>
  </resources>
  <resourcePatterns>
    <resourcePattern>files.*</resourcePattern>
  </resourcePatterns>
</exception>
```

Note that there is no corresponding concept to packages for resources, you'll have to specify each resource individually.

The Resource pattern allows excluding multiple files or pathes by applying regular expressions to the resources in question.

The plugin by default ignores several resources that are frequently found in dependencies. They follow these regular expressions (case-insensitive):

```
(META-INF/)?ASL2\.0(\.TXT)?
META-INF/DEPENDENCIES(\.TXT)?
META-INF/DISCLAIMER(\.TXT)?
(META-INF/)?[A-Z_-]*LICENSE.*
META-INF/MANIFEST\.MF
META-INF/INDEX\.LIST
META-INF/MAVEN/.*
META-INF/SERVICES/.*,
(META-INF/)?NOTICE(\.TXT)?
README(\.TXT)?
.*PACKAGE\.HTML
.*OVERVIEW\.HTML
META-INF/SPRING\.HANDLERS
META-INF/SPRING\.SCHEMAS
META-INF/SPRING\.TOOLING
.GIT
.SVN
.HG
.BZR
```

You can turn these default resource exceptions off via the `useDefaultResourceIgnoreList` configuration option:

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>duplicate-finder-maven-plugin</artifactId>
  <version>1.1.0</version>
  <configuration>
    <useDefaultResourceIgnoreList>false</useDefaultResourceIgnoreList>
    ...
  </configuration>
</plugin>
```

### Globally ignoring additional resources

It is possible to specify additional resources on the classpath that are excluded from the duplication check:

```xml
<ignoredResources>
  <ignoredResource>overview\.html</ignoredResource>
</ignoredResources>
```

will ignore all occurences of `overview.html` on the classpath. This is useful for resources that are present in many dependencies. 

The resource definition is a pattern as consumed by `java.util.regex.Pattern`.

### Failing the build

By default, the plugin will only print warning. However, you can configure it to fail the build using the `failBuildInCaseOfConflict` configuration option:

```xml
<plugin>
  <groupId>com.ning.maven.plugins</groupId>
  <artifactId>duplicate-finder-maven-plugin</artifactId>
  <configuration>
    <failBuildInCaseOfConflict>true</failBuildInCaseOfConflict>
    ...
  </configuration>
</plugin>
```

This will fail the build even in case of duplicate but identical classes/resources (as per their SHA256).
If you don't want that, then use these two for more fine-grained control:

```xml
<failBuildInCaseOfDifferentContentConflict>true</failBuildInCaseOfDifferentContentConflict>
<failBuildInCaseOfEqualContentConflict>false</failBuildInCaseOfEqualContentConflict>
```

### Verbose output

By default the plugin will not print duplicate but identical classes/resources (as per SHA256).
If you want to see those classes/resources, add this configuration option:

```xml
<printEqualFiles>true</printEqualFiles>
```

Note that this flag is ignored if either `failBuildInCaseOfConflict` or
`failBuildInCaseOfEqualContentConflict` are set to `true`.

## License

(C) 2010 Ning, Inc.

Licensed under the Apache Software License, Version 2.0. For details  see the COPYING file.

