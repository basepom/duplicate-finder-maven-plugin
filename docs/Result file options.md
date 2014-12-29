The duplicate finder plugin can write its results to a file to allow other tools to pick up and post-process them.

```xml
<configuration>
    <useResultFile>true</useResultFile>
    <resultFile>${project.build.directory}/duplicate-finder-result.xml</resultFile>
    <resultFileMinClasspathCount>2</resultFileMinClasspathCount>
</configuration>
```

There are a number of flags controlling the generation of the result file.

The format of the result file [is documented here](Result file format).

### `useResultFile`

Controls whether a result file is written nor not. By default a result file is written.

Maven command line property: ``duplicate-finder.useResultFile`
Default: **true**

### `resultFile`

The location of the result file. If the file already exists, it is overwritten. If the folder it should be created in does not exist, it is created.

By default, a file inside the `${project.build.directory}` (usually `target`) is created.

Maven command line property: ``duplicate-finder.resultFile`
Default: **`${project.build.directory}/duplicate-finder-result.xml`**

### `resultFileMinClasspathCount`

As the result file contains a list of all classpath elements (see below), it will become very large for bigger projects with many classes and resources on the classpath. The `resultFileMinClasspathCount` element controls the minimum number of occurences of a class or resource on the classpath before it will be listed in the result file. Set this element to `1` to generate a complete list.

Maven command line property: ``duplicate-finder.resultFileMinClasspathCount`
Default: **2**
