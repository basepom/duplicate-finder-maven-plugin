The duplicate finder plugin ignores the following classpath elements by default unless the `useDefaultResourceIgnoreList` flag is set to `false`.

| Regular expression       | Description                                                                                                                                                                                                                                 |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `^META-INF/.*`           | matches any resource inside a `META-INF` directory in the classpath hierarchy root. Jars store information in `META-INF` and some resources actually must be on the classpath multiple times (e.g. for the Java Service Provider facility). |
| `^OSGI-INF/.*`           | matches any resource inside an `OSGI-INF` directory in the classpath hierarchy root. OSGi components store their information in this directory.                                                                                             |
| `^licenses/.*`           | matches any resource inside a `licenses` directory in the classpath hierarchy root. This is a common place for storing component specific license resources which often have the same name.                                                 |
| `.*license(\.txt)?$`     | matches any resource ending in `license` or `license.txt`. This is a common name for license resources which tend to get sprinkled all across dependencies.                                                                                 |
| `.*notice(\.txt)?$`      | matches any resource ending in `notice` or `notice.txt`. This is a common name for resources (e.g. in Apache provided jars) which tend to get sprinkled all across dependencies.                                                            |
| `.*readme(\.txt)?$`      | matches any resource ending in `readme` or `readme.txt`. This is a common name for component information which tend to get sprinkled all across dependencies.                                                                               |
| `.*third-party(\.txt)?$` | matches any resource ending in `third-party` or `third-party.txt`. This is a common name for resources (e.g. in Apache provided jars) which tend to get sprinkled all across dependencies.                                                  |
| `.*package\.html$`       | matches any resource ending in `package.html`. The old name of `package-info.java` which tend to get sprinkled across jars and then clash if a package is populated from multiple jars.                                                     |
| `.*overview\.html$`      | matches any resource ending in `overview.html`. This is a common name for documentation which tend to get sprinkled all across dependencies.                                                                                                |

The duplicate finder plugin ignores the following classes by default unless the `useDefaultClassIgnoreList` flag is set to `false`.

| Regular expression      | Description                                                                                                                                             |
|-------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `^.*\$/.*`              | matches Java inner and nested classes. Before plugin version 1.2.1, this match was implicit and could not be turned off. **Plugin version up to 1.3.0** |
| `module-info`           | JDK 9 module-info class. **Plugin version 1.3.0+**                                                                                                      |
| `^(.*\.)?.*\$.*$`       | matches Java inner and nested classes in any package. **Plugin version 1.4.0+**                                                                         |
| `^(.*\.)?package-info$` | matches Java `package-info` class files in any package. **Plugin version 1.4.0+**                                                                       |
| `^(.*\.)?module-info$`  | matches Java `module-info` class files in any package. **Plugin version 1.4.0+**                                                                        |

The following local directory names will also always be ignored. Files in these directory are never checked or touched by the plugins:

| Regular expression | Description              |
|--------------------|--------------------------|
| `^.git$`           | git SCM directory        |
| `^.svn$`           | Subversion SCM directory |
| `^.hg$`            | Mercurial SCM directory  |
| `^.bzr$`           | Bazaar SCM directory     |

