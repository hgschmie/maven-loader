/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.softwareforge.testing.maven;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.aether.version.Version;

/**
 * A builder class to control better what versions should be returned.
 */
public final class MavenVersionMatchBuilder {

    private final MavenArtifactLoader loader;
    private final String groupId;
    private final String artifactId;

    private VersionStrategy versionStrategy = VersionStrategy.partialMatch("");

    private String extension = "jar";
    private boolean includeSnapshots = true;

    MavenVersionMatchBuilder(MavenArtifactLoader loader, String groupId, String artifactId) {
        this.loader = loader;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    /**
     * Support a partial match to the given version string. If the value is empty, any version will match. Otherwise, the version must be either exact or a
     * prefix to match a version.
     *
     * @param partial The partial version to match.
     * @return the builder
     */
    public MavenVersionMatchBuilder partialMatch(String partial) {
        this.versionStrategy = VersionStrategy.partialMatch(partial);
        return this;
    }

    /**
     * Support an exact match to a version.
     *
     * @param partial The version to match.
     * @return the builder
     */
    public MavenVersionMatchBuilder exactMatch(String partial) {
        this.versionStrategy = VersionStrategy.exactMatch(partial);
        return this;
    }

    /**
     * Supports semantic versioning, match the major version.
     *
     * @param major the major version to match.
     * @return the builder
     */
    public MavenVersionMatchBuilder semVerMajor(int major) {
        this.versionStrategy = VersionStrategy.semVerMatchMajor(major);
        return this;
    }

    /**
     * Supports semantic versioning, match the major and minor version.
     *
     * @param major the major version to match.
     * @param minor the minor version to match.
     * @return the builder
     */
    public MavenVersionMatchBuilder semVerMinor(int major, int minor) {
        this.versionStrategy = VersionStrategy.semVerMatchMinor(major, minor);
        return this;
    }

    /**
     * Set the extension to consider. Default is "jar".
     *
     * @param extension Sets the extension.
     * @return the builder
     */
    public MavenVersionMatchBuilder extension(String extension) {
        this.extension = requireNonNull(extension, "extension is null");
        return this;
    }

    /**
     * If true, snapshots are included in the results.
     *
     * @param includeSnapshots If true, include snapshots in the results. Default is {@code true}.
     * @return the builder
     */
    public MavenVersionMatchBuilder includeSnapshots(boolean includeSnapshots) {
        this.includeSnapshots = includeSnapshots;
        return this;
    }

    String groupId() {
        return groupId;
    }

    String artifactId() {
        return artifactId;
    }

    VersionStrategy versionStrategy() {
        return versionStrategy;
    }

    String extension() {
        return extension;
    }

    boolean includeSnapshots() {
        return includeSnapshots;
    }

    /**
     * Returns a list of all versions that match the search constraints.
     * @return A list of versions. This list may be empty but is never null.
     * @throws IOException If the underlying code encounters an IO problem (e.g. no network connection).
     */
    public LinkedList<String> findAll() throws IOException {
        return loader.findAllVersions(this)
                .stream()
                .map(Version::toString)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Returns the best match for the given version contraints.
     * @return The best match for the given version constraints. Can be {@link Optional#empty()} if no version matches.
     * @throws IOException If the underlying code encounters an IO problem (e.g. no network connection).
     */
    public Optional<String> findBestMatch() throws IOException {
        LinkedList<String> versions = findAll();
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.getLast());
    }
}
