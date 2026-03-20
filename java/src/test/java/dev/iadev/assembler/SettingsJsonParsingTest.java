package dev.iadev.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SettingsAssembler — buildSettingsJson,
 * buildSettingsLocalJson, parseJsonStringArray,
 * and deduplicate.
 */
@DisplayName("SettingsAssembler — JSON parsing")
class SettingsJsonParsingTest {

    @Nested
    @DisplayName("buildSettingsJson — JSON structure")
    class BuildSettingsJson {

        @Test
        @DisplayName("without hooks permissions only")
        void buildSettingsJson_withoutHooks_succeeds() {
            List<String> perms = List.of("Bash(git *)");
            String json = SettingsAssembler
                    .buildSettingsJson(
                            perms,
                            HookPresence.WITHOUT_HOOKS);

            assertThat(json)
                    .contains("\"permissions\"")
                    .contains("\"allow\"")
                    .contains("Bash(git *)")
                    .doesNotContain("hooks");
        }

        @Test
        @DisplayName("with hooks includes PostToolUse")
        void buildSettingsJson_withHooks_succeeds() {
            List<String> perms = List.of("Bash(git *)");
            String json = SettingsAssembler
                    .buildSettingsJson(
                            perms,
                            HookPresence.WITH_HOOKS);

            assertThat(json)
                    .contains("\"hooks\"")
                    .contains("\"PostToolUse\"")
                    .contains("\"Write|Edit\"")
                    .contains("post-compile-check.sh")
                    .contains("\"timeout\": 60")
                    .contains("Checking compilation...");
        }
    }

    @Nested
    @DisplayName("buildSettingsLocalJson — structure")
    class BuildSettingsLocalJson {

        @Test
        @DisplayName("produces empty permissions")
        void buildSettingsLocalJson_empty() {
            String json = SettingsAssembler
                    .buildSettingsLocalJson();

            assertThat(json)
                    .contains("\"permissions\"")
                    .contains("\"allow\": []");
        }
    }

    @Nested
    @DisplayName("parseJsonStringArray — parsing")
    class ParseJsonStringArray {

        @Test
        @DisplayName("parses simple JSON array")
        void parseJsonStringArray_simple() {
            List<String> result = SettingsAssembler
                    .parseJsonStringArray(
                            "[\"a\", \"b\", \"c\"]");

            assertThat(result)
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("empty for empty array")
        void parseJsonStringArray_empty() {
            assertThat(SettingsAssembler
                    .parseJsonStringArray("[]"))
                    .isEmpty();
        }

        @Test
        @DisplayName("empty for non-array")
        void parseJsonStringArray_nonArray() {
            assertThat(SettingsAssembler
                    .parseJsonStringArray("{}"))
                    .isEmpty();
        }

        @Test
        @DisplayName("handles parentheses")
        void parseJsonStringArray_parens() {
            assertThat(SettingsAssembler
                    .parseJsonStringArray(
                            "[\"Bash(git *)\","
                                    + " \"Bash(ls *)\"]"))
                    .containsExactly(
                            "Bash(git *)",
                            "Bash(ls *)");
        }

        @Test
        @DisplayName("handles special chars")
        void parseJsonStringArray_specialChars() {
            assertThat(SettingsAssembler
                    .parseJsonStringArray(
                            "[\"WebFetch"
                                    + "(domain:github.com)\"]"))
                    .containsExactly(
                            "WebFetch(domain:github.com)");
        }
    }

    @Nested
    @DisplayName("deduplicate — removes duplicates")
    class Deduplicate {

        @Test
        @DisplayName("preserves order removes dupes")
        void deduplicate_preservesOrder() {
            assertThat(SettingsAssembler.deduplicate(
                    List.of("a", "b", "a", "c", "b")))
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("no duplicates unchanged")
        void deduplicate_noDups() {
            assertThat(SettingsAssembler.deduplicate(
                    List.of("a", "b", "c")))
                    .containsExactly("a", "b", "c");
        }
    }
}
