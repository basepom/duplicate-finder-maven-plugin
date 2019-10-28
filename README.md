[![Build Status](https://travis-ci.org/basepom/duplicate-finder-maven-plugin.svg?branch=master)](https://travis-ci.org/basepom/duplicate-finder-maven-plugin)[![Latest Release](https://maven-badges.herokuapp.com/maven-central/org.basepom.maven/duplicate-finder-maven-plugin/badge.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.basepom.maven%22%20AND%20a%3A%22duplicate-finder-maven-plugin%22)

# duplicate-finder Maven plugin

A maven plugin to find and flag duplicate classes and resources on the java classpath.

This ensures stability and reproducability of a maven build and will flag possible problems or conflicts with dependencies in a project.

## About

This plugin is a friendly fork (same main authors) of the [Ning maven-duplicate-finder plugin](https://github.com/ning/maven-duplicate-finder-plugin). It is configuration compatible to the ning plugin; the only change required is the group and artifact id of the plugin itself.

## Requirements

The plugins requires Apache Maven 3.x.x.

Starting with release 1.2.0, Java 7 or better is required.

Starting with release 1.3.0, Java 8 or better is required.

## Goals

The plugin has two goals:

* `duplicate-finder:check` - the main goal of the plugin. Runs duplicate check on the maven classpaths.
* `duplicate-finder:help` - displays standard maven plugin help information.

## Documentation

Up-to-date documentation is available from the [duplicate finder plugin Wiki](https://github.com/basepom/duplicate-finder-maven-plugin/wiki). A snapshot from the last release of the plugin is available in the `docs` folder in this repository.

## Release Notes

### 1.4.0

#### added

#### changed

* Build with basepom 30
* use slf4j 1.7.28 (from 1.7.21)
* update plugin plugin to 3.6.0 (from 3.4)
* update groovy to 3.0.0-beta-3 (from 2.4.12)
* changed Travis Build to use OpenJDK 8 and 11

#### deprecated

#### removed

#### fixed

* Do not crash if more than one artifact reference points to the same file (#25, #29, #35)
* Fix Java 9+ module-version problems with multi-release jars (#31, #33)
* Triage and fix outdated bugs (#23, #20, #17)

#### security

* Update Guava dependency to 28.1-jre (#36)
* Use plexus 2.0.0 to build (#37)


