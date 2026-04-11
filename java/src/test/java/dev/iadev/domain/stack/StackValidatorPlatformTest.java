package dev.iadev.domain.stack;

import dev.iadev.domain.model.Platform;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for StackValidator — platform validation from
 * YAML config.
 */
@DisplayName("StackValidator — platform validation")
class StackValidatorPlatformTest {

    @Nested
    @DisplayName("validatePlatforms()")
    class ValidatePlatformsTests {

        @Test
        @DisplayName("valid platform has no errors")
        void validatePlatforms_validPlatform_noErrors() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .architectureStyle("microservice")
                    .platforms(Set.of(Platform.CLAUDE_CODE))
                    .build();

            var errors =
                    StackValidator.validatePlatforms(
                            config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("empty platforms (all) has no errors")
        void validatePlatforms_empty_noErrors() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .architectureStyle("microservice")
                    .build();

            var errors =
                    StackValidator.validatePlatforms(
                            config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("SHARED platform reports error")
        void validatePlatforms_shared_reportsError() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .architectureStyle("microservice")
                    .platforms(Set.of(Platform.SHARED))
                    .build();

            var errors =
                    StackValidator.validatePlatforms(
                            config);

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0))
                    .contains("Invalid platform value")
                    .contains("shared")
                    .contains("Valid values");
        }

        @Test
        @DisplayName("single valid platform no errors")
        void validatePlatforms_singleValid_noErrors() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .architectureStyle("microservice")
                    .platforms(Set.of(
                            Platform.CLAUDE_CODE))
                    .build();

            var errors =
                    StackValidator.validatePlatforms(
                            config);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("validateStack includes platform check")
        void validateStack_includesPlatformValidation() {
            var config = TestConfigBuilder.builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .architectureStyle("microservice")
                    .platforms(Set.of(Platform.SHARED))
                    .build();

            var errors =
                    StackValidator.validateStack(config);

            assertThat(errors)
                    .anyMatch(e -> e.contains(
                            "Invalid platform value"));
        }
    }

    @Nested
    @DisplayName("YAML parsing validation")
    class YamlParsingValidation {

        @Test
        @DisplayName("valid platform string parses ok")
        void yamlParsing_validString_noErrors() {
            var map = buildMinimalConfigMap();
            map.put("platform", "claude-code");

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms())
                    .containsExactly(Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("valid platform list parses ok")
        void yamlParsing_validList_noErrors() {
            var map = buildMinimalConfigMap();
            map.put("platform",
                    List.of("claude-code"));

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms())
                    .containsExactlyInAnyOrder(
                            Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("absent platform defaults to empty")
        void yamlParsing_absent_defaultsToEmpty() {
            var map = buildMinimalConfigMap();

            var config = ProjectConfig.fromMap(map);

            assertThat(config.platforms()).isEmpty();
        }
    }

    private Map<String, Object> buildMinimalConfigMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("project", Map.of(
                "name", "test", "purpose", "test"));
        map.put("architecture", Map.of(
                "style", "microservice"));
        map.put("interfaces", List.of(
                Map.of("type", "rest")));
        map.put("language", Map.of(
                "name", "java", "version", "21"));
        map.put("framework", Map.of(
                "name", "quarkus", "version", "3.17",
                "build_tool", "maven"));
        return map;
    }
}
