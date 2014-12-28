A dependency description is used in multiple places in the configuration. Each dependency element can have the following nested elements:

| element name | is required? | description |
| ------------ | ------------ | ----------- |
| `artifactId` | **required** |  The artifact id for the dependency. |
| `groupId`    | **required** |  The group id for the dependency. |
| `version`    | **optional** | Can be a single version or a version range. Dependency version ranges are [described in depth](http://books.sonatype.com/mvnref-book/reference/pom-relationships-sect-project-dependencies.html#pom-relationships-sect-version-ranges) in the Sonatype Maven reference. If no version is given, any version matches. |
| `type`       | **optional** |  Defines the type of dependency matching. If no type is given, it defaults to `jar`. If the type `test-jar` is used, it is equivalent to using the type `jar` with the `tests` classifier. |
| `classifier` | **optional** | Defines the dependency classifier. A classifier is only very seldom used; the most common use case is specifying `tests` for a dependency containing the unit tests for an artifact. |

##### Examples

Matching any version of `commons-lang:commons-lang`:

```xml
<dependency>
    <groupId>commons-lang</groupId>
    <artifactId>commons-lang</artifactId>
</dependency>
```

Matching any version of `commons-lang:commons-lang` up to (but
excluding) 3.0:

```xml
<dependency>
    <groupId>commons-lang</groupId>
    <artifactId>commons-lang</artifactId>
    <version>(,3.0)</version>
</dependency>
```

Match the test classes jar for commons-lang:

```xml
<dependency>
    <groupId>commons-lang</groupId>
    <artifactId>commons-lang</artifactId>
    <classifier>tests</classifier>
</dependency>
```

Match a zip file containing sources for commons-lang:

```xml
<dependency>
    <groupId>commons-lang</groupId>
    <artifactId>commons-lang</artifactId>
    <classifier>sources</classifier>
    <type>zip</type>
</dependency>
```
