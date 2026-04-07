package dev.iadev.smoke;

import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.ProjectConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Profile integrity tests verifying that all bundled
 * config templates have the expected platform
 * configuration.
 *
 * <p>Since the YAML templates do NOT include a
 * {@code platform:} key (which defaults to "all"),
 * this test validates that each profile's parsed
 * config has an empty platforms set (= all).</p>
 *
 * @see ConfigProfiles
 */
@DisplayName("Profile Platform Integrity")
class ProfilePlatformIntegritySmokeTest {

    static Stream<String> allStacks() {
        return ConfigProfiles.getAvailableStacks()
                .stream();
    }

    @Nested
    @DisplayName("default platform is 'all'")
    class DefaultPlatform {

        @ParameterizedTest(name = "{0}")
        @MethodSource("dev.iadev.smoke."
                + "ProfilePlatformIntegritySmokeTest"
                + "#allStacks")
        @DisplayName("profile has empty platforms "
                + "(= all)")
        void profile_platformsDefaultToAll(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);

            assertThat(config.platforms())
                    .as("Profile %s should default to "
                            + "all platforms (empty set)",
                            stackKey)
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("YAML template content")
    class YamlTemplateContent {

        @Test
        @DisplayName("all templates set platform: all")
        void allTemplates_setPlatformAll()
                throws IOException {
            List<String> templatesNotAll =
                    findTemplatesWithNonAllPlatform();

            assertThat(templatesNotAll)
                    .as("All templates should have "
                            + "'platform: all' — these do "
                            + "not")
                    .isEmpty();
        }

        @Test
        @DisplayName("every template has a platform key")
        void everyTemplate_hasPlatformKey()
                throws IOException {
            List<String> templatesWithPlatform =
                    findTemplatesWithPlatformKey();
            int totalStacks = ConfigProfiles
                    .getAvailableStacks().size();

            assertThat(templatesWithPlatform)
                    .as("Every template should have "
                            + "'platform:' key")
                    .hasSize(totalStacks);
        }
    }

    @Nested
    @DisplayName("profile count consistency")
    class ProfileCount {

        @Test
        @DisplayName("ConfigProfiles has 18+ stacks "
                + "registered")
        void configProfiles_hasExpectedCount() {
            List<String> stacks =
                    ConfigProfiles.getAvailableStacks();

            assertThat(stacks.size())
                    .as("Should have at least 18 "
                            + "registered profiles")
                    .isGreaterThanOrEqualTo(18);
        }
    }

    /**
     * Scans config template YAMLs on classpath for any
     * that contain a {@code platform:} key at the root
     * level.
     */
    private static List<String> findTemplatesWithPlatformKey()
            throws IOException {
        List<String> found = new ArrayList<>();
        ClassLoader cl =
                ProfilePlatformIntegritySmokeTest.class
                        .getClassLoader();

        for (String stackKey
                : ConfigProfiles.getAvailableStacks()) {
            String resourcePath =
                    "shared/config-templates/"
                            + "setup-config." + stackKey
                            + ".yaml";
            try (InputStream is =
                         cl.getResourceAsStream(
                                 resourcePath)) {
                if (is == null) {
                    continue;
                }
                try (BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(
                                             is,
                                             StandardCharsets
                                                     .UTF_8))) {
                    if (containsPlatformKey(reader)) {
                        found.add(stackKey);
                    }
                }
            }
        }
        return found;
    }

    /**
     * Finds templates where {@code platform:} is set
     * to something other than "all".
     */
    private static List<String>
            findTemplatesWithNonAllPlatform()
            throws IOException {
        List<String> found = new ArrayList<>();
        ClassLoader cl =
                ProfilePlatformIntegritySmokeTest.class
                        .getClassLoader();

        for (String stackKey
                : ConfigProfiles.getAvailableStacks()) {
            String resourcePath =
                    "shared/config-templates/"
                            + "setup-config." + stackKey
                            + ".yaml";
            try (InputStream is =
                         cl.getResourceAsStream(
                                 resourcePath)) {
                if (is == null) {
                    continue;
                }
                try (BufferedReader reader =
                             new BufferedReader(
                                     new InputStreamReader(
                                             is,
                                             StandardCharsets
                                                     .UTF_8))) {
                    if (hasNonAllPlatform(reader)) {
                        found.add(stackKey);
                    }
                }
            }
        }
        return found;
    }

    private static boolean containsPlatformKey(
            BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.startsWith("platform:")
                    && !trimmed.startsWith(
                            "platform_")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNonAllPlatform(
            BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.startsWith("platform:")
                    && !trimmed.startsWith(
                            "platform_")) {
                String value = trimmed.substring(
                        "platform:".length()).trim();
                return !"all".equals(value);
            }
        }
        return false;
    }
}
