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
 * Tests for ReadmeUtils -- counting and extraction
 * utilities for README generation.
 */
@DisplayName("ReadmeUtils")
class ReadmeUtilsTest {

    @Nested
    @DisplayName("countRules")
    class CountRules {

        @Test
        @DisplayName("returns 0 when rules dir missing")
        void create_whenMissing_zero(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countRules(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("returns 0 when rules dir empty")
        void create_whenEmpty_zero(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve("rules"));

            assertThat(ReadmeUtils.countRules(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts 1 md file")
        void create_whenCalled_countsOneMdFile(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            Files.writeString(
                    rulesDir.resolve("01-identity.md"),
                    "content", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countRules(tempDir))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("counts 5 md files")
        void create_whenCalled_countsFiveMdFiles(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            for (int i = 1; i <= 5; i++) {
                Files.writeString(
                        rulesDir.resolve(
                                "0" + i + "-rule.md"),
                        "content",
                        StandardCharsets.UTF_8);
            }

            assertThat(ReadmeUtils.countRules(tempDir))
                    .isEqualTo(5);
        }

        @Test
        @DisplayName("ignores non-md files")
        void create_whenCalled_ignoresNonMdFiles(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            Files.writeString(
                    rulesDir.resolve("01-rule.md"),
                    "content", StandardCharsets.UTF_8);
            Files.writeString(
                    rulesDir.resolve("readme.txt"),
                    "content", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countRules(tempDir))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countSkills")
    class CountSkills {

        @Test
        @DisplayName("returns 0 when skills dir missing")
        void create_whenMissing_zero(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countSkills(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts subdirs with SKILL.md")
        void create_whenCalled_countsSkillMdSubdirs(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "description: Review skill");
            createSkill(skillsDir, "x-deploy",
                    "description: Deploy skill");

            assertThat(ReadmeUtils.countSkills(tempDir))
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("ignores subdirs without SKILL.md")
        void create_withoutSkillMd_ignoresSubdirs(
                @TempDir Path tempDir)
                throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "description: Review");
            Files.createDirectories(
                    skillsDir.resolve("empty-dir"));

            assertThat(ReadmeUtils.countSkills(tempDir))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countAgents")
    class CountAgents {

        @Test
        @DisplayName("returns 0 when agents dir missing")
        void create_whenMissing_zero(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countAgents(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts md files in agents dir")
        void create_whenCalled_countsMdFiles(@TempDir Path tempDir)
                throws IOException {
            Path agentsDir =
                    Files.createDirectories(
                            tempDir.resolve("agents"));
            Files.writeString(
                    agentsDir.resolve("architect.md"),
                    "content", StandardCharsets.UTF_8);
            Files.writeString(
                    agentsDir.resolve("tech-lead.md"),
                    "content", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countAgents(tempDir))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countKnowledgePacks")
    class CountKnowledgePacks {

        @Test
        @DisplayName("returns 0 when skills dir missing")
        void create_whenMissing_zero(@TempDir Path tempDir) {
            assertThat(
                    ReadmeUtils.countKnowledgePacks(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts only knowledge packs")
        void create_whenCalled_countsOnlyKnowledgePacks(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "description: Review\n"
                            + "user-invocable: true");
            createSkill(skillsDir, "coding-standards",
                    "description: Coding\n"
                            + "user-invocable: false");
            createSkill(skillsDir, "architecture",
                    "# Knowledge Pack\nInternal");

            assertThat(
                    ReadmeUtils.countKnowledgePacks(tempDir))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countHooks")
    class CountHooks {

        @Test
        @DisplayName("returns 0 when hooks dir missing")
        void create_whenMissing_zero(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countHooks(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts all entries in hooks dir")
        void create_whenCalled_countsEntries(@TempDir Path tempDir)
                throws IOException {
            Path hooksDir =
                    Files.createDirectories(
                            tempDir.resolve("hooks"));
            Files.writeString(
                    hooksDir.resolve(
                            "post-compile-check.sh"),
                    "#!/bin/bash",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countHooks(tempDir))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countSettings")
    class CountSettings {

        @Test
        @DisplayName("returns 0 when no settings files")
        void create_whenNoFiles_zero(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countSettings(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("returns 2 when both files exist")
        void create_whenCalled_twoBothExist(@TempDir Path tempDir)
                throws IOException {
            Files.writeString(
                    tempDir.resolve("settings.json"),
                    "{}", StandardCharsets.UTF_8);
            Files.writeString(
                    tempDir.resolve("settings.local.json"),
                    "{}", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countSettings(tempDir))
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("returns 1 when only settings.json")
        void create_whenOnlyMain_one(@TempDir Path tempDir)
                throws IOException {
            Files.writeString(
                    tempDir.resolve("settings.json"),
                    "{}", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countSettings(tempDir))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countCodexFiles")
    class CountCodexFiles {

        @Test
        @DisplayName("returns 0 when dir missing")
        void create_whenMissing_zero(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countCodexFiles(
                    tempDir.resolve(".codex")))
                    .isZero();
        }

        @Test
        @DisplayName("counts files recursively")
        void create_whenCalled_countsRecursive(@TempDir Path tempDir)
                throws IOException {
            Path codexDir =
                    Files.createDirectories(
                            tempDir.resolve(".codex"));
            Files.writeString(
                    codexDir.resolve("config.toml"),
                    "c", StandardCharsets.UTF_8);
            Files.createDirectories(
                    codexDir.resolve("subdir"));
            Files.writeString(
                    codexDir.resolve("subdir/extra.toml"),
                    "x", StandardCharsets.UTF_8);

            assertThat(
                    ReadmeUtils.countCodexFiles(codexDir))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countCodexAgentsFiles")
    class CountCodexAgentsFiles {

        @Test
        @DisplayName("returns 0 when dir missing")
        void create_whenMissing_zero(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countCodexAgentsFiles(
                    tempDir.resolve(".agents")))
                    .isZero();
        }

        @Test
        @DisplayName("counts files recursively")
        void create_whenCalled_countsRecursive(@TempDir Path tempDir)
                throws IOException {
            Path agentsDir =
                    Files.createDirectories(
                            tempDir.resolve(".agents"));
            Files.writeString(
                    agentsDir.resolve("AGENTS.md"),
                    "c", StandardCharsets.UTF_8);
            Path subDir =
                    Files.createDirectories(
                            agentsDir.resolve("skills"));
            Files.writeString(
                    subDir.resolve("SKILL.md"),
                    "c", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countCodexAgentsFiles(
                    agentsDir))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("isKnowledgePack")
    class IsKnowledgePack {

        @Test
        @DisplayName("returns true for user-invocable:"
                + " false")
        void create_forUserInvocableFalse_true(
                @TempDir Path tempDir) throws IOException {
            Path skillMd =
                    tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "name: coding-standards\n"
                            + "user-invocable: false\n",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.isKnowledgePack(skillMd))
                    .isTrue();
        }

        @Test
        @DisplayName("returns true for # Knowledge Pack"
                + " header")
        void create_forKnowledgePackHeader_true(
                @TempDir Path tempDir) throws IOException {
            Path skillMd =
                    tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "# Knowledge Pack\nInternal",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.isKnowledgePack(skillMd))
                    .isTrue();
        }

        @Test
        @DisplayName("returns false for regular skill")
        void create_forRegularSkill_false(
                @TempDir Path tempDir) throws IOException {
            Path skillMd =
                    tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "name: x-review\n"
                            + "description: Review skill\n",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.isKnowledgePack(skillMd))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("extractRuleNumber")
    class ExtractRuleNumber {

        @Test
        @DisplayName("extracts leading digits")
        void create_whenCalled_extractsLeadingDigits() {
            assertThat(
                    ReadmeUtils.extractRuleNumber(
                            "01-project-identity.md"))
                    .isEqualTo("01");
        }

        @Test
        @DisplayName("extracts multi-digit number")
        void create_whenCalled_extractsMultiDigit() {
            assertThat(
                    ReadmeUtils.extractRuleNumber(
                            "123-some-rule.md"))
                    .isEqualTo("123");
        }

        @Test
        @DisplayName("returns empty for no leading digits")
        void create_forNoDigits_empty() {
            assertThat(
                    ReadmeUtils.extractRuleNumber(
                            "name.md"))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("extractRuleScope")
    class ExtractRuleScope {

        @Test
        @DisplayName("strips number and extension,"
                + " replaces hyphens")
        void create_whenCalled_stripsNumberAndExtension() {
            assertThat(
                    ReadmeUtils.extractRuleScope(
                            "01-project-identity.md"))
                    .isEqualTo("project identity");
        }

        @Test
        @DisplayName("handles filename without number")
        void create_whenCalled_handlesNoNumber() {
            assertThat(
                    ReadmeUtils.extractRuleScope(
                            "coding-standards.md"))
                    .isEqualTo("coding standards");
        }

        @Test
        @DisplayName("handles single word scope")
        void create_whenCalled_handlesSingleWord() {
            assertThat(
                    ReadmeUtils.extractRuleScope(
                            "05-domain.md"))
                    .isEqualTo("domain");
        }
    }

    @Nested
    @DisplayName("extractSkillDescription")
    class ExtractSkillDescription {

        @Test
        @DisplayName("extracts description from"
                + " frontmatter")
        void create_whenCalled_extractsDescription(@TempDir Path tempDir)
                throws IOException {
            Path skillMd =
                    tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "name: x-review\n"
                            + "description: Review code\n",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.extractSkillDescription(
                    skillMd))
                    .isEqualTo("Review code");
        }

        @Test
        @DisplayName("strips quotes from description")
        void create_whenCalled_stripsQuotes(@TempDir Path tempDir)
                throws IOException {
            Path skillMd =
                    tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "name: x-review\n"
                            + "description: \"Review\"\n",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.extractSkillDescription(
                    skillMd))
                    .isEqualTo("Review");
        }

        @Test
        @DisplayName("returns empty when no description")
        void create_whenNoDescription_empty(@TempDir Path tempDir)
                throws IOException {
            Path skillMd =
                    tempDir.resolve("SKILL.md");
            Files.writeString(skillMd,
                    "name: x-review\n",
                    StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.extractSkillDescription(
                    skillMd))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("countFilesRecursive")
    class CountFilesRecursive {

        @Test
        @DisplayName("counts files in nested dirs")
        void create_whenCalled_countsNestedFiles(@TempDir Path tempDir)
                throws IOException {
            Files.writeString(
                    tempDir.resolve("file1.md"),
                    "c", StandardCharsets.UTF_8);
            Path subDir =
                    Files.createDirectories(
                            tempDir.resolve("sub"));
            Files.writeString(
                    subDir.resolve("file2.md"),
                    "c", StandardCharsets.UTF_8);
            Path deepDir =
                    Files.createDirectories(
                            subDir.resolve("deep"));
            Files.writeString(
                    deepDir.resolve("file3.md"),
                    "c", StandardCharsets.UTF_8);

            assertThat(
                    ReadmeUtils.countFilesRecursive(tempDir))
                    .isEqualTo(3);
        }
    }

    private static void createSkill(
            Path skillsDir, String name, String content)
            throws IOException {
        Path dir = Files.createDirectories(
                skillsDir.resolve(name));
        Files.writeString(dir.resolve("SKILL.md"),
                content, StandardCharsets.UTF_8);
    }
}
