# Changes

##### 1.2.1

* Add `includePomProjects` attribute in result file.
* Add support for ignoring classes by use of a regular expression.

##### 1.2.0 - 2015-06-18

* Add `includePomProjects` configuration setting to allow projects
with POM packaging to be checked. Suggested by #11. Thanks @camshoff.
* Allow inclusion of multiple artifacts that may map to the same local
project. This fixes issue #10 (thanks @jakub-bochenski).

##### 1.1.2 - 2015-06-16

__1.1.2 will be the last release that works with JDK 6! With Maven having moved to JDK 7, there
is no reason anymore for plugins to stick to an old JDK version.__

* Fix a bug where an exception is ignored if more conflicts are listed in the exception than
actually conflicting jars are present.

##### 1.1.1 - 2015-01-18

* Issue #9. Ignore project references that are not on the dependency list. Thanks @victornoel.
* Add maven properties for all simple (boolean, int, string) configuration settings.
* Ning Issue #44: Allow duplicate checking against elements from the boot classpath
  (this includes rt.jar).
* Ning Issue #19: Report a defined message if plugin skips execution.

##### 1.1.0 - 2014-12-27

* Full rewrite from the old Ning plugin
