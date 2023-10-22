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

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;

interface VersionStrategy {

    static VersionStrategy partialMatch(String partial) {
        return artifactVersion -> partial.isEmpty()
                || artifactVersion.toString().equals(partial)
                || artifactVersion.toString().startsWith(partial + '.');
    }

    static VersionStrategy exactMatch(String version) {
        return artifactVersion -> artifactVersion.toString().equals(version);
    }

    static VersionStrategy semVerMatchMajor(int major) {
        return new SemVerVersionStrategy(major, -1);
    }

    static VersionStrategy semVerMatchMinor(int major, int minor) {
        return new SemVerVersionStrategy(major, minor);
    }

    boolean matchVersion(Version version);

    class SemVerVersionStrategy implements VersionStrategy {

        private static final GenericVersionScheme SCHEME = new GenericVersionScheme();

        private final VersionRange range;

        SemVerVersionStrategy(int major, int minor) {

            StringBuilder matchString = new StringBuilder("[")
                    .append(major)
                    .append('.');

            if (minor > 0) {
                matchString.append(minor)
                        .append('.');
            }

            matchString.append("*]");

            try {
                this.range = SCHEME.parseVersionRange(matchString.toString());
            } catch (InvalidVersionSpecificationException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        public boolean matchVersion(Version version) {
            return range.containsVersion(version);
        }
    }
}
