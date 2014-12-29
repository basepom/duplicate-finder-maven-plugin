The duplicate finder plugin allows global exclusion of dependencies
and resources. Any matching dependency or resource will not be considered 
duplicate, no matter how often it appears on the classpath.

This feature is very easy to abuse. Please use with care.

```xml
<configuration>
    <useDefaultResourceIgnoreList>true</useDefaultResourceIgnoreList>
    <ignoredResourcePatterns>
        <ignoredResourcePattern>...</ignoredResourcePattern>
    </ignoredResourcePatterns>
    <ignoredDependencies>
        <dependency>
            <artifactId>...</artifactId>
            <groupId>...</groupId>
            <version>...</version>
            <type>...</type>
            <classifier>...</classifier>
        </dependency>
    </ignoredDependencies>
    <ignoredResources>
        <ignoredResource>...</ignoredResource>
    </ignoredResources>
</configuration>
```

### `ignoredResources` deprecation

For backwards compatibility to the old Ning plugin, the
`<ignoredResources>` element is equivalent to
`<ignoredResourcePatterns>`. The plugin will print a warning if the
old element is used. The `<ignoredResources>` element will be removed
with the release of version **1.2.0** of the plugin.

#### `useDefaultResourceIgnoreList` flag

By default, the duplicate finder plugin ignores a set of resources on
the classpath which tend to be duplicates all the time. 

These resources are specified as standard Java regular expressions. All patterns are case insensitive.

The default resource ignore list [is documented here](Default ignored elements).

Maven command line property: ``duplicate-finder.useDefaultResourceIgnoreList`
Default: **true**

**Warning!** Setting this element to `false` will result in a significant number of false positives.

#### `ignoredResourcePatterns` for global resource exclusion

The `ignoredResourcePatterns` element lists standard Java regular expression patterns that are excluded from the duplicate check. All patterns are treated as case insensitive.

Any pattern added here is treated similar to the patterns on the default resource ignore list.

**Note**. Any pattern here is applied to the whole resource path, not
  to sub-components. It is therefore important to anchor a pattern to
  the beginning, the end or a separator (`/`). It is recommended to
  test any regular expression with a
  [regular expression tester](http://www.regexplanet.com/advanced/java/index.html).

##### Examples

Ignore all resources ending in `index.html`:

```xml
<configuration>
    <ignoredResourcePatterns>
        <ignoredResourcePattern>.*index\.html$</ignoredResourcePattern>
    </ignoredResourcePatterns>
</configuration>
```
Ignore all log4j and logback configuration resources:

```xml
<configuration>
    <ignoredResourcePatterns>
        <ignoredResourcePattern>/?[^/]*?log4j\.xml$</ignoredResourcePattern>
        <ignoredResourcePattern>/?[^/]*?log4j\.properties$</ignoredResourcePattern>
        <ignoredResourcePattern>/?[^/]*?logback\.xml$</ignoredResourcePattern>
        <ignoredResourcePattern>/?[^/]*?logback\.properties$</ignoredResourcePattern>
    </ignoredResourcePatterns>
</configuration>
```


#### `ignoreDependencies` for global dependency exclusion

Sometimes, a dependency is hopeless. It may drag in a large number of
duplicates or it may not have been written to any given standard. For
these very rare cases, it is possible to completely exclude a
dependency and everything that is related to it from the duplication
check. Usually, these dependencies are also in a non-standard scope
(such as `provided` or `system`).

**Warning!** Excluding a dependency globally may make the duplicate
  finder check pass. It will not, however, fix the underlying cause
  for the failure. Ignoring duplicate dependencies can and almost
  always will lead to hard-to-debug runtime problems such as
  hard-to-explain
  [`ClassNotFoundException`](http://docs.oracle.com/javase/8/docs/api/java/lang/ClassNotFoundException.html)
  (or their big sibling
  [`NoClassDefFoundError`](http://docs.oracle.com/javase/8/docs/api/java/lang/NoClassDefFoundError.html))
  problems.

The `ignoreDependencies` section contains [`dependency`](Describing
Dependencies) elements. Each `dependency` can be fully or partially
defined.

Each element listed here will be removed from the list of dependencies
that are checked for duplicates.

##### Example

Remove `org.jruby:jruby-complete` from the duplicate check:

```xml
<!-- jRuby is hopeless -->
<configuration>
    <ignoredDependencies>
        <dependency>
            <groupId>org.jruby</groupId>
            <artifactId>jruby-complete</artifactId>
        </dependency>
    </ignoredDependencies>
</configuration>
```
