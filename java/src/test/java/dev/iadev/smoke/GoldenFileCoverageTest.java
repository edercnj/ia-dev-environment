package dev.iadev.smoke;

import dev.iadev.config.ConfigProfiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that validates coverage between
 * golden file directories and smoke-testable profiles.
 *
 * <p>Ensures no golden file directory exists without
 * a corresponding smoke profile, and no smoke profile
 * lacks golden files. Also verifies that the expected
 * artifacts manifest is in sync with golden files.</p>
 *
 * @see SmokeProfiles
 * @see ConfigProfiles
 */
@DisplayName("Golden File Coverage")
class GoldenFileCoverageTest {

    private static final String SELF_PROFILE =
            "java-picocli-cli";

    /**
     * Profiles registered in STACK_KEYS that are not
     * yet smoke-testable. These are excluded from the
     * registration-coverage check until golden files
     * and smoke profiles are added (story-0023-0013).
     */
    private static final Set<String>
            PENDING_SMOKE_PROFILES = Set.of(
            "java-spring-neo4j",
            "java-spring-clickhouse",
            "python-fastapi-timescale",
            "java-spring-elasticsearch"
    );

    @Nested
    @DisplayName("Golden ↔ SmokeProfiles symmetry")
    class GoldenSmokeSymmetry {

        @Test
        @DisplayName(
                "every golden directory has a "
                        + "SmokeProfiles entry")
        void everyGoldenDir_hasSmokeProfileEntry()
                throws IOException, URISyntaxException {
            Set<String> goldenDirs =
                    discoverGoldenDirectories();
            List<String> smokeProfiles =
                    SmokeProfiles.profileList();

            Set<String> orphanGolden =
                    new TreeSet<>(goldenDirs);
            orphanGolden.removeAll(smokeProfiles);

            assertThat(orphanGolden)
                    .as("Golden file directories without "
                            + "a SmokeProfiles entry — "
                            + "these golden files exist "
                            + "but are never tested")
                    .isEmpty();
        }

        @Test
        @DisplayName(
                "every SmokeProfile has a golden "
                        + "directory")
        void everySmokeProfile_hasGoldenDir()
                throws IOException, URISyntaxException {
            Set<String> goldenDirs =
                    discoverGoldenDirectories();
            List<String> smokeProfiles =
                    SmokeProfiles.profileList();

            List<String> missingGolden =
                    smokeProfiles.stream()
                            .filter(p ->
                                    !goldenDirs.contains(p))
                            .sorted()
                            .toList();

            assertThat(missingGolden)
                    .as("SmokeProfiles without golden "
                            + "file directories — these "
                            + "profiles are listed but "
                            + "have no golden files for "
                            + "byte-for-byte validation")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Registration coverage")
    class RegistrationCoverage {

        @Test
        @DisplayName(
                "every non-self profile in STACK_KEYS "
                        + "is smoke-tested")
        void everyNonSelfProfile_isSmokeTestable() {
            List<String> stacks =
                    ConfigProfiles.getAvailableStacks();
            List<String> smokeProfiles =
                    SmokeProfiles.profileList();

            List<String> untested = stacks.stream()
                    .filter(s ->
                            !SELF_PROFILE.equals(s))
                    .filter(s ->
                            !smokeProfiles.contains(s))
                    .filter(s ->
                            !PENDING_SMOKE_PROFILES
                                    .contains(s))
                    .sorted()
                    .toList();

            assertThat(untested)
                    .as("Non-self STACK_KEYS profiles "
                            + "not in SmokeProfiles — "
                            + "these profiles are "
                            + "registered but never "
                            + "smoke-tested or golden-"
                            + "file-validated")
                    .isEmpty();
        }

        @Test
        @DisplayName(
                "every SmokeProfile is a valid "
                        + "STACK_KEYS entry")
        void everySmokeProfile_isValidStackKey() {
            List<String> smokeProfiles =
                    SmokeProfiles.profileList();

            List<String> invalid =
                    smokeProfiles.stream()
                            .filter(p -> !ConfigProfiles
                                    .isValidStack(p))
                            .sorted()
                            .toList();

            assertThat(invalid)
                    .as("SmokeProfiles that are not "
                            + "valid STACK_KEYS entries")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Expected artifacts manifest")
    class ManifestCoverage {

        @Test
        @DisplayName(
                "manifest covers all SmokeProfiles")
        void manifest_coversAllSmokeProfiles() {
            ExpectedArtifacts manifest =
                    ExpectedArtifacts.loadFromClasspath(
                            "smoke/expected-artifacts.json");
            List<String> smokeProfiles =
                    SmokeProfiles.profileList();

            List<String> missing =
                    smokeProfiles.stream()
                            .filter(p -> !manifest
                                    .getProfileNames()
                                    .contains(p))
                            .sorted()
                            .toList();

            assertThat(missing)
                    .as("SmokeProfiles not in expected-"
                            + "artifacts.json manifest")
                    .isEmpty();
        }

        @Test
        @DisplayName(
                "manifest has no extra profiles "
                        + "beyond SmokeProfiles")
        void manifest_hasNoExtraProfiles() {
            ExpectedArtifacts manifest =
                    ExpectedArtifacts.loadFromClasspath(
                            "smoke/expected-artifacts.json");
            List<String> smokeProfiles =
                    SmokeProfiles.profileList();

            Set<String> extra =
                    new TreeSet<>(
                            manifest.getProfileNames());
            extra.removeAll(smokeProfiles);

            assertThat(extra)
                    .as("Profiles in expected-artifacts"
                            + ".json not in SmokeProfiles")
                    .isEmpty();
        }
    }

    /**
     * Discovers golden file directory names under
     * {@code /golden/} on the test classpath.
     */
    private static Set<String> discoverGoldenDirectories()
            throws IOException, URISyntaxException {
        Set<String> dirs = new TreeSet<>();
        URL goldenUrl =
                GoldenFileCoverageTest.class
                        .getResource("/golden/");
        if (goldenUrl == null) {
            return dirs;
        }
        Path goldenRoot = Path.of(goldenUrl.toURI());
        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(
                             goldenRoot,
                             Files::isDirectory)) {
            for (Path dir : stream) {
                dirs.add(
                        dir.getFileName().toString());
            }
        }
        return dirs;
    }
}
