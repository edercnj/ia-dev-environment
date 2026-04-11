package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional coverage tests for ReadmeUtils —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("ReadmeUtils — coverage")
class ReadmeUtilsCoverageTest {

    @Nested
    @DisplayName("countSettings — partial files")
    class CountSettingsPartial {

        @Test
        @DisplayName("returns 1 when only"
                + " settings.local.json")
        void render_whenCalled_oneWhenOnlyLocal(@TempDir Path tempDir)
                throws IOException {
            Files.writeString(
                    tempDir.resolve("settings.local.json"),
                    "{}", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countSettings(tempDir))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("isKnowledgePack — edge cases")
    class IsKnowledgePackEdge {

        @Test
        @DisplayName("knowledge pack header not at"
                + " line start is not matched")
        void isKnowledgePack_whenCalled_headerNotAtStartNotMatched(
                @TempDir Path tempDir) throws IOException {
            Path skillMd = tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "name: test\n"
                            + "## Knowledge Pack\n"
                            + "user-invocable: true\n",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.isKnowledgePack(skillMd))
                    .isFalse();
        }

        @Test
        @DisplayName("empty file is not knowledge pack")
        void isKnowledgePack_emptyFileNotKp_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path skillMd = tempDir.resolve("SKILL.md");
            Files.writeString(skillMd, "",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.isKnowledgePack(skillMd))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("extractSkillDescription — edge cases")
    class ExtractSkillDescriptionEdge {

        @Test
        @DisplayName("description with single quotes"
                + " stripped")
        void extractSkillDescription_singleQuotesStripped_succeeds(@TempDir Path tempDir)
                throws IOException {
            Path skillMd = tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "name: x-test\n"
                            + "description: 'Test skill'\n",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.extractSkillDescription(
                    skillMd))
                    .isEqualTo("Test skill");
        }

        @Test
        @DisplayName("description with only key and"
                + " colon returns empty")
        void extractSkillDescription_descriptionOnlyColon_returnsEmpty(
                @TempDir Path tempDir) throws IOException {
            Path skillMd = tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "name: x-test\ndescription:\n",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.extractSkillDescription(
                    skillMd))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("extractRuleNumber — edge cases")
    class ExtractRuleNumberEdge {

        @Test
        @DisplayName("digits in middle not extracted")
        void extractRuleNumber_whenCalled_digitsInMiddle() {
            assertThat(ReadmeUtils.extractRuleNumber(
                    "rule-01.md"))
                    .isEmpty();
        }

        @Test
        @DisplayName("empty string returns empty")
        void extractRuleNumber_empty_returnsEmpty() {
            assertThat(ReadmeUtils.extractRuleNumber(""))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("extractRuleScope — edge cases")
    class ExtractRuleScopeEdge {

        @Test
        @DisplayName("double hyphen in name preserved")
        void extractRuleScope_whenCalled_doubleHyphen() {
            assertThat(ReadmeUtils.extractRuleScope(
                    "01-some--rule.md"))
                    .isEqualTo("some  rule");
        }

        @Test
        @DisplayName("file without .md extension")
        void extractRuleScope_noMdExtension_succeeds() {
            assertThat(ReadmeUtils.extractRuleScope(
                    "01-identity"))
                    .isEqualTo("identity");
        }
    }

    @Nested
    @DisplayName("countFilesRecursive — edge cases")
    class CountFilesRecursiveEdge {

        @Test
        @DisplayName("empty directory returns 0")
        void countFilesRecursive_emptyDir_returnsZero(@TempDir Path tempDir) {
            assertThat(
                    ReadmeUtils.countFilesRecursive(tempDir))
                    .isZero();
        }
    }
}
