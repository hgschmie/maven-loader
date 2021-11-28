# maven-loader - a dead simple artifact loader for Maven repository system


This is a repackaged version of a bunch of Apache Maven dependencies, intended to offer a quick way to locate and download artifacts from the Maven central repository. It resolves all maven internal dependencies consistently and hides the existing library dependency problems from code that wants to download artifacts from the Maven repository system.


As most things Maven are simply a mess (and even internally inconsistent), this bundles up

- The Apache Maven resolver to locate artifacts
- File and HTTP transports
- Maven settings to load and interpret the local settings file

All the functionality is in the `MavenArtifactLoader` class. See the javadoc for the class for more details.

----

(C) 2021 Henning P. Schmiedehausen
Licensed under the Apache Software License V2.0
