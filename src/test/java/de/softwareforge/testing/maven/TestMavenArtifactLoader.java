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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public final class TestMavenArtifactLoader {

    static final String GROUP_ID = "de.softwareforge.testing";
    static final String ARTIFACT_ID = "maven-loader";

    @Test
    void testFindVersion21X() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        List<String> results = loader.builder(GROUP_ID, ARTIFACT_ID)
                .includeSnapshots(false)
                .partialMatch("2.1")
                .findAll();

        assertThat(results)
                .isNotEmpty()
                .contains("2.1.0", "2.1.1")
                .doesNotContain("2.1.0-SNAPSHOT");
    }

    @Test
    void testFindVersion210() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        List<String> results = loader.builder(GROUP_ID, ARTIFACT_ID)
                .includeSnapshots(false)
                .exactMatch("2.1.0")
                .findAll();

        assertThat(results)
                .isNotEmpty()
                .containsExactly("2.1.0");
    }

    @Test
    void testFindSnapshotVersion210() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        List<String> results = loader.builder(GROUP_ID, ARTIFACT_ID)
                .includeSnapshots(true)
                .partialMatch("2.1")
                .findAll();

        assertThat(results)
                .isNotEmpty()
                .contains("2.1.0-SNAPSHOT");
    }

    @Test
    void testSemVerMajor() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        List<String> results = loader.builder(GROUP_ID, ARTIFACT_ID)
                .semVerMajor(1)
                .includeSnapshots(false)
                .findAll();

        assertThat(results)
                .contains("1.0", "1.1")
                .doesNotContain("2.0", "2.1.0");
    }

    @Test
    void testSemVerMajorBest() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        Optional<String> result = loader.builder(GROUP_ID, ARTIFACT_ID)
                .semVerMajor(1)
                .includeSnapshots(false)
                .findBestMatch();

        assertThat(result).isNotEmpty().contains("1.2");
    }

    @Test
    void testSemVerMajor0OnlySnapshot() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        Optional<String> result = loader.builder(GROUP_ID, ARTIFACT_ID)
                .semVerMajor(0)
                .findBestMatch();

        assertThat(result).isNotEmpty()
                .contains("0.1-SNAPSHOT");
    }

    @Test
    void testSemVerMajorNotFound() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        Optional<String> result = loader.builder(GROUP_ID, ARTIFACT_ID)
                .semVerMajor(0)
                .includeSnapshots(false)
                .findBestMatch();

        assertThat(result).isEmpty();
    }

    @Test
    void testSemVerMinor() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        List<String> results = loader.builder(GROUP_ID, ARTIFACT_ID)
                .semVerMinor(2, 1)
                .includeSnapshots(false)
                .findAll();

        assertThat(results)
                .doesNotContain("1.0", "1.1", "2.0")
                .contains("2.1.0", "2.1.1");
    }


    @Test
    void testFindLatestVersion() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        LinkedList<String> results = loader.builder(GROUP_ID, ARTIFACT_ID).findAll();

        String latestVersion = loader.findLatestVersion(GROUP_ID, ARTIFACT_ID, "");

        assertThat(results).isNotEmpty();
        assertThat(latestVersion)
                .isEqualTo(results.getLast());
    }


    @Test
    void testLoadArtifact() throws IOException {
        MavenArtifactLoader loader = new MavenArtifactLoader();

        Optional<String> result = loader.builder(GROUP_ID, ARTIFACT_ID)
                .includeSnapshots(false)
                .partialMatch("2.1")
                .findBestMatch();

        assertThat(result).isPresent();

        File artifactFile = loader.getArtifactFile(GROUP_ID, ARTIFACT_ID, result.get());

        assertThat(artifactFile).exists();
    }
}
