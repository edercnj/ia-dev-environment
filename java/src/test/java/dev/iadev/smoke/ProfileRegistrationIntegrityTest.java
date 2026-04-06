package dev.iadev.smoke;

import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.stack.ResolvedStack;
import dev.iadev.domain.stack.StackMapping;
import dev.iadev.domain.stack.StackResolver;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that validates the registration
 * integrity of all bundled stack profiles.
 *
 * <p>Prevents configuration drift where a config
 * template YAML exists on classpath but is not
 * registered in {@link ConfigProfiles}, or vice versa.
 * Also validates that each profile's framework is
 * properly registered in {@link StackMapping}.</p>
 *
 * <p>This test would have caught the
 * {@code typescript-commander-cli} registration gap
 * where the YAML template existed but the profile
 * was not in {@code STACK_KEYS}.</p>
 *
 * @see ConfigProfiles
 * @see StackMapping
 */
@DisplayName("Profile Registration Integrity")
class ProfileRegistrationIntegrityTest {

    private static final String TEMPLATE_DIR =
            "shared/config-templates/";
    private static final String TEMPLATE_PREFIX =
            "setup-config.";
    private static final String TEMPLATE_SUFFIX = ".yaml";

    @Nested
    @DisplayName("YAML ↔ STACK_KEYS symmetry")
    class YamlStackKeysSymmetry {

        @Test
        @DisplayName(
                "every config template YAML has a "
                        + "STACK_KEYS entry")
        void everyYaml_hasStackKeyEntry()
                throws IOException {
            Set<String> yamlProfiles =
                    discoverYamlProfiles();
            List<String> stacks =
                    ConfigProfiles.getAvailableStacks();

            List<String> orphanYamls = yamlProfiles.stream()
                    .filter(y -> !stacks.contains(y))
                    .sorted()
                    .toList();

            assertThat(orphanYamls)
                    .as("Config template YAMLs without "
                            + "a STACK_KEYS entry — these "
                            + "profiles exist on classpath "
                            + "but cannot be loaded via "
                            + "ConfigProfiles.getStack()")
                    .isEmpty();
        }

        @Test
        @DisplayName(
                "every STACK_KEYS entry has a config "
                        + "template YAML")
        void everyStackKey_hasYamlTemplate()
                throws IOException {
            Set<String> yamlProfiles =
                    discoverYamlProfiles();
            List<String> stacks =
                    ConfigProfiles.getAvailableStacks();

            List<String> orphanKeys = stacks.stream()
                    .filter(k -> !yamlProfiles.contains(k))
                    .sorted()
                    .toList();

            assertThat(orphanKeys)
                    .as("STACK_KEYS entries without "
                            + "a config template YAML — "
                            + "these keys are registered "
                            + "but have no backing "
                            + "configuration")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Framework registration")
    class FrameworkRegistration {

        static Stream<String> allStacks() {
            return ConfigProfiles.getAvailableStacks()
                    .stream();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "framework is in "
                        + "FRAMEWORK_LANGUAGE_RULES")
        void framework_isInLanguageRules(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);
            String framework =
                    config.framework().name();

            boolean isCli = config.interfaces().stream()
                    .anyMatch(i -> "cli".equals(i.type()));

            if (!isCli) {
                assertThat(StackMapping
                        .FRAMEWORK_LANGUAGE_RULES)
                        .as("Framework '%s' (from %s) "
                                        + "must be in "
                                        + "FRAMEWORK_LANGUAGE_"
                                        + "RULES",
                                framework, stackKey)
                        .containsKey(framework);
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "framework language matches config "
                        + "language")
        void framework_languageMatchesConfig(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);
            String framework =
                    config.framework().name();
            String language =
                    config.language().name();

            List<String> validLanguages =
                    StackMapping.FRAMEWORK_LANGUAGE_RULES
                            .get(framework);

            if (validLanguages != null) {
                assertThat(validLanguages)
                        .as("Framework '%s' must accept "
                                        + "language '%s' "
                                        + "(from %s)",
                                framework, language,
                                stackKey)
                        .contains(language);
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "non-CLI framework has port mapping")
        void nonCliFramework_hasPortMapping(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);
            String framework =
                    config.framework().name();

            boolean isCli = config.interfaces().stream()
                    .anyMatch(i -> "cli".equals(i.type()));

            if (!isCli) {
                assertThat(StackMapping.FRAMEWORK_PORTS)
                        .as("Non-CLI framework '%s' "
                                        + "(from %s) must "
                                        + "have a port "
                                        + "mapping",
                                framework, stackKey)
                        .containsKey(framework);
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "non-CLI framework has health path")
        void nonCliFramework_hasHealthPath(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);
            String framework =
                    config.framework().name();

            boolean isCli = config.interfaces().stream()
                    .anyMatch(i -> "cli".equals(i.type()));

            if (!isCli) {
                assertThat(StackMapping
                        .FRAMEWORK_HEALTH_PATHS)
                        .as("Non-CLI framework '%s' "
                                        + "(from %s) must "
                                        + "have a health "
                                        + "path mapping",
                                framework, stackKey)
                        .containsKey(framework);
            }
        }
    }

    @Nested
    @DisplayName("Stack resolution")
    class StackResolution {

        static Stream<String> allStacks() {
            return ConfigProfiles.getAvailableStacks()
                    .stream();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "StackResolver resolves without error")
        void stackResolver_resolvesSuccessfully(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);

            ResolvedStack resolved =
                    StackResolver.resolve(config);

            assertThat(resolved)
                    .as("StackResolver must resolve "
                            + "for %s", stackKey)
                    .isNotNull();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "language has Docker base image")
        void language_hasDockerImage(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);
            String language =
                    config.language().name();

            assertThat(StackMapping.DOCKER_BASE_IMAGES)
                    .as("Language '%s' (from %s) must "
                                    + "have a Docker base "
                                    + "image mapping",
                            language, stackKey)
                    .containsKey(language);
        }
    }

    @Nested
    @DisplayName("Config loading validity")
    class ConfigLoadingValidity {

        static Stream<String> allStacks() {
            return ConfigProfiles.getAvailableStacks()
                    .stream();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "profile loads without exception")
        void profile_loadsSuccessfully(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);

            assertThat(config).isNotNull();
            assertThat(config.project().name())
                    .isNotBlank();
            assertThat(config.language().name())
                    .isNotBlank();
            assertThat(config.language().version())
                    .isNotBlank();
            assertThat(config.framework().name())
                    .isNotBlank();
            assertThat(config.framework().buildTool())
                    .isNotBlank();
            assertThat(config.architecture().style())
                    .isNotBlank();
            assertThat(config.interfaces())
                    .isNotEmpty();
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "architecture style is valid")
        void profile_hasValidArchitectureStyle(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);

            assertThat(StackMapping
                    .VALID_ARCHITECTURE_STYLES)
                    .as("Architecture style '%s' "
                                    + "(from %s) must be "
                                    + "in VALID_ARCHITECTURE"
                                    + "_STYLES",
                            config.architecture().style(),
                            stackKey)
                    .contains(
                            config.architecture().style());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("allStacks")
        @DisplayName(
                "all interface types are valid")
        void profile_hasValidInterfaceTypes(
                String stackKey) {
            ProjectConfig config =
                    ConfigProfiles.getStack(stackKey);

            for (var iface : config.interfaces()) {
                assertThat(StackMapping
                        .VALID_INTERFACE_TYPES)
                        .as("Interface type '%s' "
                                        + "(from %s) must "
                                        + "be valid",
                                iface.type(), stackKey)
                        .contains(iface.type());
            }
        }
    }

    /**
     * Discovers all config template YAML profile names
     * by listing the classpath resource directory.
     */
    private static Set<String> discoverYamlProfiles()
            throws IOException {
        Set<String> profiles = new TreeSet<>();
        ClassLoader cl = ProfileRegistrationIntegrityTest
                .class.getClassLoader();

        try (InputStream is = cl.getResourceAsStream(
                TEMPLATE_DIR);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(
                             is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(TEMPLATE_PREFIX)
                        && line.endsWith(TEMPLATE_SUFFIX)) {
                    String name = line.substring(
                            TEMPLATE_PREFIX.length(),
                            line.length()
                                    - TEMPLATE_SUFFIX
                                    .length());
                    profiles.add(name);
                }
            }
        }
        return profiles;
    }
}
