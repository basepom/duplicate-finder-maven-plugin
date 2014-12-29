# duplicate-finder Maven plugin

A maven plugin to find and flag duplicate classes and resources on the java classpath.

This ensures stability and reproducability of a maven build and will flag possible problems or conflicts with dependencies in a project.

## About

This plugin is a friendly fork (same main authors) of the [Ning maven-duplicate-finder plugin](https://github.com/ning/maven-duplicate-finder-plugin). It is configuration compatible to the ning plugin; the only change required is the group and artifact id of the plugin itself.

## Requirements

The plugins requires Apache Maven 3.x.x and Java 6 or better.

## Goals

The plugin has two goals:

* `duplicate-finder:check` - the main goal of the plugin. Runs duplicate check on the maven classpaths.
* `duplicate-finder:help` - displays standard maven plugin help information.

## Documentation

Up-to-date documentation is available from the [duplicate finder plugin Wiki](https://github.com/basepom/duplicate-finder-maven-plugin/wiki). A snapshot from the last release of the plugin is available in the `docs` folder in this repository.

[![Build Status](https://travis-ci.org/basepom/duplicate-finder-maven-plugin.svg?branch=master)](https://travis-ci.org/basepom/duplicate-finder-maven-plugin)
