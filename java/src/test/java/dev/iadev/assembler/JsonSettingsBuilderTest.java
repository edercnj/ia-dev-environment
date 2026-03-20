package dev.iadev.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JsonSettingsBuilder — builds JSON content for
 * settings.json and settings.local.json.
 */
@DisplayName("JsonSettingsBuilder")
class JsonSettingsBuilderTest {

    private final JsonSettingsBuilder builder =
            new JsonSettingsBuilder();

    @Nested
    @DisplayName("build — settings.json content")
    class Build {

        @Test
        @DisplayName("without hooks produces permissions"
                + " only")
        void withoutHooksPermissionsOnly() {
            List<String> perms = List.of("Bash(git *)");

            String json = builder.build(
                    perms, HookPresence.WITHOUT_HOOKS);

            assertThat(json)
                    .contains("\"permissions\"")
                    .contains("\"allow\"")
                    .contains("Bash(git *)")
                    .doesNotContain("hooks");
        }

        @Test
        @DisplayName("with hooks includes PostToolUse")
        void withHooksIncludesPostToolUse() {
            List<String> perms = List.of("Bash(git *)");

            String json = builder.build(
                    perms, HookPresence.WITH_HOOKS);

            assertThat(json)
                    .contains("\"hooks\"")
                    .contains("\"PostToolUse\"")
                    .contains("\"Write|Edit\"")
                    .contains("post-compile-check.sh")
                    .contains("\"timeout\": 60")
                    .contains("Checking compilation...");
        }

        @Test
        @DisplayName("empty permissions produces empty"
                + " allow array")
        void emptyPermissions() {
            String json = builder.build(
                    List.of(), HookPresence.WITHOUT_HOOKS);

            assertThat(json)
                    .contains("\"allow\": [\n")
                    .contains("]\n");
        }

        @Test
        @DisplayName("multiple permissions separated"
                + " by commas")
        void multiplePermissions() {
            List<String> perms = List.of(
                    "Bash(git *)",
                    "Bash(mvn *)",
                    "Bash(npm *)");

            String json = builder.build(
                    perms, HookPresence.WITHOUT_HOOKS);

            assertThat(json)
                    .contains("\"Bash(git *)\"")
                    .contains("\"Bash(mvn *)\"")
                    .contains("\"Bash(npm *)\"");
            assertThat(json)
                    .contains("\"Bash(git *)\",");
            assertThat(json)
                    .contains("\"Bash(mvn *)\",");
        }

        @Test
        @DisplayName("JSON starts and ends with braces")
        void validJsonStructure() {
            String json = builder.build(
                    List.of("Bash(git *)"),
                    HookPresence.WITHOUT_HOOKS);

            assertThat(json.trim()).startsWith("{");
            assertThat(json.trim()).endsWith("}");
        }
    }

    @Nested
    @DisplayName("buildLocal — settings.local.json content")
    class BuildLocal {

        @Test
        @DisplayName("produces empty permissions")
        void producesEmptyPermissions() {
            String json = builder.buildLocal();

            assertThat(json)
                    .contains("\"permissions\"")
                    .contains("\"allow\": []");
        }

        @Test
        @DisplayName("valid JSON structure")
        void validJsonStructure() {
            String json = builder.buildLocal();

            assertThat(json.trim()).startsWith("{");
            assertThat(json.trim()).endsWith("}");
        }
    }
}
