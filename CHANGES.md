# Changes

[Keep a Changelog](https://keepachangelog.com/en/1.0.0/) specification.

### unreleased

#### added

* ignore changelog.txt (#44) (Thanks @adamzr)

#### changed

* use github actions for CI, remove travis
* use maven wrapper
* build with JDK9+ only, still support JDK8 as runtime
  * CI build with JDK 11, 14
  * CI run integration tests on JDK 8, 11, 14

### 1.4.0 - 2019-10-27

#### changed

* Build with basepom 30
* use slf4j 1.7.28 (from 1.7.21)
* update plugin plugin to 3.6.0 (from 3.4)
* update groovy to 3.0.0-beta-3 (from 2.4.12)
* changed Travis Build to use OpenJDK 8 and 11
* Clarify much more what is a resource and what is considered a class. This changes a lot of things that used to be matched as classes to be matched as resources. Especially all path elements (directories, jar file folders etc.) now must match the Java identifier specification. This e.g. rejects classes in Multi-release jars that are located in `META-INF/versions/<version>` because `META-INF` or the version number are not valid Java identifiers. An exception is made for `package-info` and `module-info` class files, whose names are not valid Java identifiers but are still loaded as classes from the class path.

#### fixed

* Do not crash if more than one artifact reference points to the same file (#25, #29, #35)
* Fix Java 9+ module-version problems with multi-release jars (#31, #33)
* Triage and fix outdated bugs (#23, #20, #17)

#### security

* Update Guava dependency to 28.1-jre (#36)
* Use plexus 2.0.0 to build (#37)


### 1.3.0 - 2018-02-20

__Starting with 1.3.0, JDK8 will be required to run the plugin.__

* Removed deprecated `<ignoredResources>` configuration option.
* Use correct path separator for classpath splitting, fixes #28 (Thanks @arxes-tolina)
* Fix typo in warning message (Thanks @gaul)

### 1.2.1 - 2015-09-18

* Add support for ignoring classes by use of a regular expression, #16 (Thanks @mbellomo)
* Allow turning off default class ignore list, #16 (Thanks @mbellomo)
* Add `includePomProjects` attribute in result file.

### 1.2.0 - 2015-06-18

* Add `includePomProjects` configuration setting to allow projects
with POM packaging to be checked. Suggested by #11. Thanks @camshoff.
* Allow inclusion of multiple artifacts that may map to the same local
project. This fixes issue #10 (thanks @jakub-bochenski).

### 1.1.2 - 2015-06-16

__1.1.2 will be the last release that works with JDK 6! With Maven having moved to JDK 7, there
is no reason anymore for plugins to stick to an old JDK version.__

* Fix a bug where an exception is ignored if more conflicts are listed in the exception than
actually conflicting jars are present.

### 1.1.1 - 2015-01-18

* Issue #9. Ignore project references that are not on the dependency list. Thanks @victornoel.
* Add maven properties for all simple (boolean, int, string) configuration settings.
* Ning Issue #44: Allow duplicate checking against elements from the boot classpath
  (this includes rt.jar).
* Ning Issue #19: Report a defined message if plugin skips execution.

### 1.1.0 - 2014-12-27

* Full rewrite from the old Ning plugin
