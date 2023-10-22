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

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilder;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A quick and dirty artifact loader. Downloads published artifacts from the Maven repository system.
 * <p>
 * The loader respects the local maven settings (repositories, mirrors etc.) if present. If no configuration is found, a hard-coded reference to <a
 * href="https://repo.maven.apache.org/maven2/">Maven Central</a> is used.
 */
public final class MavenArtifactLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MavenArtifactLoader.class);

    private static final RemoteRepository CENTRAL_REPO = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();

    private static final String USER_HOME = System.getProperty("user.home");
    private static final File USER_MAVEN_HOME = new File(USER_HOME, ".m2");
    private static final String ENV_M2_HOME = System.getenv("M2_HOME");

    private static final File DEFAULT_USER_SETTINGS_FILE = new File(USER_MAVEN_HOME, "settings.xml");
    private static final File DEFAULT_USER_REPOSITORY = new File(USER_MAVEN_HOME, "repository");
    private static final File DEFAULT_GLOBAL_SETTINGS_FILE =
            new File(System.getProperty("maven.home", Objects.requireNonNullElse(ENV_M2_HOME, "")), "conf/settings.xml");

    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession mavenSession;
    private final List<RemoteRepository> remoteRepositories;

    private final String extension;

    /**
     * Creates a new artifact loader for 'jar' artifacts.
     */
    public MavenArtifactLoader() {
        this("jar");
    }

    /**
     * Creates a new artifact loader for artifacts.
     *
     * @param extension The artifact extension. Must not be null.
     */
    public MavenArtifactLoader(String extension) {
        this.extension = requireNonNull(extension, "extension is null");

        @SuppressWarnings("deprecation")
        ServiceLocator serviceLocator = createServiceLocator();
        this.repositorySystem = serviceLocator.getService(RepositorySystem.class);

        try {
            Settings settings = createSettings();
            File localRepositoryLocation = settings.getLocalRepository() != null ? new File(settings.getLocalRepository()) : DEFAULT_USER_REPOSITORY;
            LocalRepository localRepository = new LocalRepository(localRepositoryLocation);
            this.remoteRepositories = extractRemoteRepositories(settings);

            DefaultRepositorySystemSession mavenSession = MavenRepositorySystemUtils.newSession();

            this.mavenSession = mavenSession.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(mavenSession, localRepository));

        } catch (SettingsBuildingException e) {
            throw new IllegalStateException("Could not load maven settings:", e);
        }
    }

    /**
     * Create a new version match builder to retrieve an artifact.
     *
     * @param groupId    The Apache Maven Group Id. Must not be null.
     * @param artifactId The Apache Maven Artifact Id. Must not be null.
     * @return A {@link MavenVersionMatchBuilder} instance
     */
    public MavenVersionMatchBuilder builder(String groupId, String artifactId) {
        requireNonNull(groupId, "groupId is null");
        requireNonNull(artifactId, "artifactId is null");

        return new MavenVersionMatchBuilder(this, groupId, artifactId);
    }

    /**
     * Download an artifact file from the Maven repository system.
     *
     * @param groupId    The Apache Maven Group Id. Must not be null.
     * @param artifactId The Apache Maven Artifact Id. Must not be null.
     * @param version    The Apache Maven Artifact version. Must not be null.
     * @return A file representing a successfully downloaded artifact.
     * @throws IOException If the artifact could not be found or an IO problem happened while locating or downloading the artifact.
     */
    public File getArtifactFile(String groupId, String artifactId, String version) throws IOException {
        requireNonNull(groupId, "groupId is null");
        requireNonNull(artifactId, "artifactId is null");
        requireNonNull(version, "version is null");

        ArtifactRequest artifactRequest = new ArtifactRequest();
        artifactRequest.setArtifact(new DefaultArtifact(groupId, artifactId, extension, version));
        artifactRequest.setRepositories(this.remoteRepositories);
        try {
            ArtifactResult artifactResult = this.repositorySystem.resolveArtifact(mavenSession, artifactRequest);
            Artifact artifact = artifactResult.getArtifact();
            return artifact.getFile();
        } catch (RepositoryException e) {
            throw new IOException(e);
        }
    }

    /**
     * Find a matching artifact version from a partially defined artifact version.
     * <p>
     * Any located artifact in the repository system is compared to the version given.
     * <br>
     * Using this method is equivalent to calling
     * <pre>
     *  builder(groupId, artifactId)
     *                 .partialMatch(version)
     *                 .extension(extension)
     *                 .includeSnapshots(true)
     *                 .findBestMatch();
     * </pre>
     *
     * but will throw an IOException if no version could be found.
     *
     * @param groupId    The Apache Maven Group Id. Must not be null.
     * @param artifactId The Apache Maven Artifact Id. Must not be null.
     * @param version    A partial version string. Must not be null. An empty string matches any version.
     * @return The latest version that matches the partial version string. It either starts with the partial version string given (an empty version string
     * matches any version) or is exactly the provided version.
     * @throws IOException If an IO problem happened during artifact download or no versions were found during resolution.
     */
    public String findLatestVersion(String groupId, String artifactId, String version) throws IOException {
        requireNonNull(groupId, "groupId is null");
        requireNonNull(artifactId, "artifactId is null");
        requireNonNull(version, "version is null");
        return builder(groupId, artifactId)
                .partialMatch(version)
                .extension(extension)
                .includeSnapshots(true)
                .findBestMatch()
                .orElseThrow(() -> new IOException(format("No suitable candidate for %s:%s:%s found!", groupId, artifactId, version)));
    }

    SortedSet<Version> findAllVersions(MavenVersionMatchBuilder builder) throws IOException {

        Artifact artifact = new DefaultArtifact(builder.groupId(), builder.artifactId(), builder.extension(), "[0,)");

        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.setRepositories(this.remoteRepositories);

        try {
            VersionRangeResult rangeResult = this.repositorySystem.resolveVersionRange(mavenSession, rangeRequest);
            SortedSet<Version> resultBuilder = new TreeSet<>();
            List<Version> artifactVersions = rangeResult.getVersions();
            VersionStrategy versionStrategy = builder.versionStrategy();
            if (artifactVersions != null) {
                for (Version artifactVersion : artifactVersions) {
                    boolean isSnapshot = artifactVersion.toString().endsWith("-SNAPSHOT");
                    boolean match = versionStrategy.matchVersion(artifactVersion);

                    // remove match if snapshots are not requested but the version is a snapshot
                    if (isSnapshot) {
                        match &= builder.includeSnapshots();
                    }

                    if (match) {
                        resultBuilder.add(artifactVersion);
                    }
                }
            }
            return Collections.unmodifiableSortedSet(resultBuilder);
        } catch (VersionRangeResolutionException e) {
            throw new IOException(format("Could not resolve version range: %s", rangeRequest), e);
        }
    }


    @SuppressWarnings("deprecation")
    private static ServiceLocator createServiceLocator() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable e) {
                LOG.error(format("Could not create instance of %s (implementation %s): ", type.getSimpleName(), impl.getSimpleName()), e);
            }
        });

        return locator;
    }

    private static Settings createSettings() throws SettingsBuildingException {
        SettingsBuildingRequest settingsBuildingRequest = new DefaultSettingsBuildingRequest()
                .setSystemProperties(System.getProperties())
                .setUserSettingsFile(DEFAULT_USER_SETTINGS_FILE)
                .setGlobalSettingsFile(DEFAULT_GLOBAL_SETTINGS_FILE);

        DefaultSettingsBuilderFactory settingBuilderFactory = new DefaultSettingsBuilderFactory();
        DefaultSettingsBuilder settingsBuilder = settingBuilderFactory.newInstance();
        SettingsBuildingResult settingsBuildingResult = settingsBuilder.build(settingsBuildingRequest);

        return settingsBuildingResult.getEffectiveSettings();
    }

    private static List<RemoteRepository> extractRemoteRepositories(Settings settings) {
        Map<String, Profile> profiles = settings.getProfilesAsMap();
        List<RemoteRepository> builder = new ArrayList<>();

        boolean foundRepository = false;
        for (String profileName : settings.getActiveProfiles()) {
            Profile profile = profiles.get(profileName);
            if (profile != null) {
                List<Repository> repositories = profile.getRepositories();
                if (repositories != null) {
                    for (Repository repo : repositories) {
                        builder.add(new RemoteRepository.Builder(repo.getId(), "default", repo.getUrl()).build());
                        foundRepository = true;
                    }
                }
            }
        }

        if (!foundRepository && !settings.isOffline()) {
            builder.add(CENTRAL_REPO);
        }

        return Collections.unmodifiableList(builder);
    }
}
