package dev.iadev.nativeimage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates GraalVM native image configuration files are
 * well-formed JSON with the expected entries.
 *
 * <p>These tests verify that the static configuration files
 * under {@code META-INF/native-image/} are valid JSON and
 * contain all classes and resources needed for native
 * compilation.</p>
 */
@DisplayName("GraalVM Native Image Configuration")
class NativeImageConfigTest {

    private static final String CONFIG_BASE =
            "META-INF/native-image/dev.iadev/ia-dev-env/";

    private static final ObjectMapper MAPPER =
            new ObjectMapper();

    @Nested
    @DisplayName("reflect-config.json")
    class ReflectConfig {

        @Test
        @DisplayName("isValidJson_whenParsed_returnsNonEmptyArray")
        void isValidJson_whenParsed_returnsNonEmptyArray()
                throws IOException {
            JsonNode root = loadJson("reflect-config.json");
            assertThat(root.isArray())
                    .as("reflect-config must be a JSON array")
                    .isTrue();
            assertThat(root.size())
                    .as("reflect-config must not be empty")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("containsPicocliCommands_whenParsed_allThreePresent")
        void containsPicocliCommands_whenParsed_allThreePresent()
                throws IOException {
            Set<String> classNames = extractClassNames();
            assertThat(classNames).contains(
                    "dev.iadev.cli.IaDevEnvApplication",
                    "dev.iadev.cli.GenerateCommand",
                    "dev.iadev.cli.ValidateCommand"
            );
        }

        @Test
        @DisplayName("containsJdkCollections_whenParsed_coreCollectionsPresent")
        void containsJdkCollections_whenParsed_coreCollectionsPresent()
                throws IOException {
            Set<String> classNames = extractClassNames();
            assertThat(classNames).contains(
                    "java.util.LinkedHashMap",
                    "java.util.ArrayList",
                    "java.util.HashMap"
            );
        }

        @Test
        @DisplayName("containsLogbackClasses_whenParsed_loggerContextPresent")
        void containsLogbackClasses_whenParsed_loggerContextPresent()
                throws IOException {
            Set<String> classNames = extractClassNames();
            assertThat(classNames).contains(
                    "ch.qos.logback.classic.LoggerContext"
            );
        }

        @Test
        @DisplayName("allEntriesHaveName_whenParsed_noEntryMissingName")
        void allEntriesHaveName_whenParsed_noEntryMissingName()
                throws IOException {
            JsonNode root = loadJson("reflect-config.json");
            for (JsonNode entry : root) {
                assertThat(entry.has("name"))
                        .as("Every reflect entry must have 'name'")
                        .isTrue();
                assertThat(entry.get("name").asText())
                        .as("name must not be blank")
                        .isNotBlank();
            }
        }

        @Test
        @DisplayName("picocliEntries_haveFullReflectionAccess")
        void picocliEntries_whenCalled_haveFullReflectionAccess()
                throws IOException {
            List<Map<String, Object>> entries = loadEntries();
            List<String> picocliClasses = List.of(
                    "dev.iadev.cli.IaDevEnvApplication",
                    "dev.iadev.cli.GenerateCommand",
                    "dev.iadev.cli.ValidateCommand"
            );
            for (Map<String, Object> entry : entries) {
                String name = (String) entry.get("name");
                if (picocliClasses.contains(name)) {
                    assertThat(entry.get(
                            "allDeclaredConstructors"))
                            .as(name + " must have constructors")
                            .isEqualTo(true);
                    assertThat(entry.get(
                            "allDeclaredMethods"))
                            .as(name + " must have methods")
                            .isEqualTo(true);
                    assertThat(entry.get(
                            "allDeclaredFields"))
                            .as(name + " must have fields")
                            .isEqualTo(true);
                }
            }
        }

        @Test
        @DisplayName("referencedClasses_existOnClasspath")
        void referencedClasses_whenCalled_existOnClasspath()
                throws IOException {
            Set<String> classNames = extractClassNames();
            for (String className : classNames) {
                assertThat(classExists(className))
                        .as("Class %s must be on the classpath",
                                className)
                        .isTrue();
            }
        }

        private Set<String> extractClassNames()
                throws IOException {
            List<Map<String, Object>> entries = loadEntries();
            return entries.stream()
                    .map(e -> (String) e.get("name"))
                    .collect(Collectors.toSet());
        }

        private List<Map<String, Object>> loadEntries()
                throws IOException {
            return MAPPER.readValue(
                    loadResource("reflect-config.json"),
                    new TypeReference<>() {});
        }
    }

    @Nested
    @DisplayName("resource-config.json")
    class ResourceConfig {

        @Test
        @DisplayName("isValidJson_whenParsed_hasResourcesKey")
        void isValidJson_whenParsed_hasResourcesKey()
                throws IOException {
            JsonNode root = loadJson("resource-config.json");
            assertThat(root.isObject())
                    .as("resource-config must be a JSON object")
                    .isTrue();
            assertThat(root.has("resources"))
                    .as("must have 'resources' key")
                    .isTrue();
        }

        @Test
        @DisplayName("hasIncludesArray_whenParsed_includesNotEmpty")
        void hasIncludesArray_whenParsed_includesNotEmpty()
                throws IOException {
            JsonNode root = loadJson("resource-config.json");
            JsonNode includes =
                    root.path("resources").path("includes");
            assertThat(includes.isArray())
                    .as("resources.includes must be an array")
                    .isTrue();
            assertThat(includes.size())
                    .as("includes must not be empty")
                    .isGreaterThan(0);
        }

        @Test
        @DisplayName("containsTemplatePattern_whenParsed_templatesIncluded")
        void containsTemplatePattern_whenParsed_templatesIncluded()
                throws IOException {
            Set<String> patterns = extractPatterns();
            assertThat(patterns).contains("templates/.*");
        }

        @Test
        @DisplayName("containsConfigTemplates_whenParsed_configTemplatesIncluded")
        void containsConfigTemplates_whenParsed_configTemplatesIncluded()
                throws IOException {
            Set<String> patterns = extractPatterns();
            assertThat(patterns).contains(
                    "config-templates/.*");
        }

        @Test
        @DisplayName("containsAllResourceDirs_whenParsed_allDirsIncluded")
        void containsAllResourceDirs_whenParsed_allDirsIncluded()
                throws IOException {
            Set<String> patterns = extractPatterns();
            List<String> requiredPatterns = List.of(
                    "templates/.*",
                    "config-templates/.*",
                    "targets/claude/agents/.*",
                    "core/.*",
                    "targets/claude/rules/.*",
                    "frameworks/.*",
                    "languages/.*",
                    "targets/claude/skills/.*",
                    "github-agents-templates/.*",
                    "github-skills-templates/.*"
            );
            assertThat(patterns)
                    .containsAll(requiredPatterns);
        }

        @Test
        @DisplayName("allEntriesHavePattern_whenParsed_noMissingPatterns")
        void allEntriesHavePattern_whenParsed_noMissingPatterns()
                throws IOException {
            JsonNode includes = loadJson(
                    "resource-config.json")
                    .path("resources")
                    .path("includes");
            for (JsonNode entry : includes) {
                assertThat(entry.has("pattern"))
                        .as("Every include must have 'pattern'")
                        .isTrue();
                assertThat(entry.get("pattern").asText())
                        .as("pattern must not be blank")
                        .isNotBlank();
            }
        }

        private Set<String> extractPatterns()
                throws IOException {
            JsonNode includes = loadJson(
                    "resource-config.json")
                    .path("resources")
                    .path("includes");
            Set<String> patterns = new java.util.HashSet<>();
            for (JsonNode entry : includes) {
                patterns.add(entry.get("pattern").asText());
            }
            return patterns;
        }
    }

    @Nested
    @DisplayName("native-image.properties")
    class NativeImageProperties {

        @Test
        @DisplayName("exists_whenLoaded_resourceNotNull")
        void exists_whenLoaded_resourceNotNull()
                throws IOException {
            String content = loadResourceAsString(
                    "native-image.properties");
            assertThat(content)
                    .as("native-image.properties must exist"
                            + " and have content")
                    .isNotEmpty();
        }

        @Test
        @DisplayName("containsNoFallback_whenRead_flagPresent")
        void containsNoFallback_whenRead_flagPresent()
                throws IOException {
            String content = loadResourceAsString(
                    "native-image.properties");
            assertThat(content)
                    .contains("--no-fallback");
        }

        @Test
        @DisplayName("containsReportExceptions_whenRead_flagPresent")
        void containsReportExceptions_whenRead_flagPresent()
                throws IOException {
            String content = loadResourceAsString(
                    "native-image.properties");
            assertThat(content).contains(
                    "-H:+ReportExceptionStackTraces");
        }
    }

    private JsonNode loadJson(String filename)
            throws IOException {
        return MAPPER.readTree(loadResource(filename));
    }

    private InputStream loadResource(String filename) {
        InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(CONFIG_BASE + filename);
        assertThat(stream)
                .as("Resource %s must exist on classpath",
                        filename)
                .isNotNull();
        return stream;
    }

    private String loadResourceAsString(String filename)
            throws IOException {
        try (InputStream stream = loadResource(filename)) {
            return new String(stream.readAllBytes());
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false,
                    NativeImageConfigTest.class
                            .getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
