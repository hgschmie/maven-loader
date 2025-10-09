# maven-loader - a dead simple artifact loader for Maven repository system


This is a repackaged version of a bunch of Apache Maven dependencies, intended to offer a quick way to locate and download artifacts from the Maven central repository. It resolves all maven internal dependencies consistently and hides the existing library dependency problems from code that wants to download artifacts from the Maven repository system.


As most things Maven are simply a mess (and even internally inconsistent), this bundles up

- The Apache Maven resolver to locate artifacts
- File and HTTP transports
- Maven settings to load and interpret the local settings file

All the functionality is in the `MavenArtifactLoader` class. See the javadoc for the class for more details.


## Find artifacts

Do a partial match:

``` java
MavenArtifactLoader loader = new MavenArtifactLoader();

List<String> results = loader.builder(GROUP_ID, ARTIFACT_ID)
        .includeSnapshots(false)
        .partialMatch("2.1")
        .findAll();
```

Do an exact match:

``` java
MavenArtifactLoader loader = new MavenArtifactLoader();

Optional<String> result = loader.builder(GROUP_ID, ARTIFACT_ID)
        .includeSnapshots(false)
        .exactMatch("2.1.1")
        .findBestMatch();
```

Find best semver match for a major version:


``` java
MavenArtifactLoader loader = new MavenArtifactLoader();

Optional<String> result = loader.builder(GROUP_ID, ARTIFACT_ID)
        .includeSnapshots(false)
        .semVerMajor(2)
        .findBestMatch();
```

Find all matches for semver major and minor:

``` java
MavenArtifactLoader loader = new MavenArtifactLoader();

List<String> results = loader.builder(GROUP_ID, ARTIFACT_ID)
        .includeSnapshots(false)
        .semVerMinor(2, 1)
        .findAll();
```

## Load an artifact

``` java
MavenArtifactLoader loader = new MavenArtifactLoader();
File artifactFile = loader.getArtifactFile(GROUP_ID, ARTIFACT_ID, "2.1.1");
```
