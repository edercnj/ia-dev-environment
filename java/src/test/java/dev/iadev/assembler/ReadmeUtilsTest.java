package dev.iadev.assembler;

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
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countRules(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("returns 0 when rules dir empty")
        void zeroWhenEmpty(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve("rules"));

            assertThat(ReadmeUtils.countRules(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts 1 md file")
        void countsOneMdFile(@TempDir Path tempDir)
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
        void countsFiveMdFiles(@TempDir Path tempDir)
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
        void ignoresNonMdFiles(@TempDir Path tempDir)
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
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countSkills(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts subdirs with SKILL.md")
        void countsSkillMdSubdirs(@TempDir Path tempDir)
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
        void ignoresSubdirsWithoutSkillMd(
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
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countAgents(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts md files in agents dir")
        void countsMdFiles(@TempDir Path tempDir)
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
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(
                    ReadmeUtils.countKnowledgePacks(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts only knowledge packs")
        void countsOnlyKnowledgePacks(
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
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countHooks(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts all entries in hooks dir")
        void countsEntries(@TempDir Path tempDir)
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
        void zeroWhenNoFiles(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countSettings(tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("returns 2 when both files exist")
        void twoBothExist(@TempDir Path tempDir)
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
        void oneWhenOnlyMain(@TempDir Path tempDir)
                throws IOException {
            Files.writeString(
                    tempDir.resolve("settings.json"),
                    "{}", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countSettings(tempDir))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countGithubFiles")
    class CountGithubFiles {

        @Test
        @DisplayName("returns 0 when dir missing")
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countGithubFiles(
                    tempDir.resolve(".github")))
                    .isZero();
        }

        @Test
        @DisplayName("counts files recursively")
        void countsRecursive(@TempDir Path tempDir)
                throws IOException {
            Path ghDir =
                    Files.createDirectories(
                            tempDir.resolve(".github"));
            Files.writeString(
                    ghDir.resolve("copilot.md"),
                    "content", StandardCharsets.UTF_8);
            Path subDir =
                    Files.createDirectories(
                            ghDir.resolve("instructions"));
            Files.writeString(
                    subDir.resolve("arch.md"),
                    "content", StandardCharsets.UTF_8);

            assertThat(
                    ReadmeUtils.countGithubFiles(ghDir))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countGithubComponent")
    class CountGithubComponent {

        @Test
        @DisplayName("returns 0 when component missing")
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countGithubComponent(
                    tempDir, "instructions"))
                    .isZero();
        }

        @Test
        @DisplayName("counts files in component dir")
        void countsFiles(@TempDir Path tempDir)
                throws IOException {
            Path instrDir =
                    Files.createDirectories(
                            tempDir.resolve("instructions"));
            Files.writeString(
                    instrDir.resolve("arch.md"),
                    "c", StandardCharsets.UTF_8);
            Files.writeString(
                    instrDir.resolve("coding.md"),
                    "c", StandardCharsets.UTF_8);

            assertThat(ReadmeUtils.countGithubComponent(
                    tempDir, "instructions"))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countGithubSkills")
    class CountGithubSkills {

        @Test
        @DisplayName("returns 0 when skills dir missing")
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countGithubSkills(
                    tempDir))
                    .isZero();
        }

        @Test
        @DisplayName("counts SKILL.md in subdirs")
        void countsSkillMd(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "name: x-review");
            createSkill(skillsDir, "x-deploy",
                    "name: x-deploy");

            assertThat(ReadmeUtils.countGithubSkills(
                    tempDir))
                    .isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("countCodexFiles")
    class CountCodexFiles {

        @Test
        @DisplayName("returns 0 when dir missing")
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countCodexFiles(
                    tempDir.resolve(".codex")))
                    .isZero();
        }

        @Test
        @DisplayName("counts only files, not dirs")
        void countsOnlyFiles(@TempDir Path tempDir)
                throws IOException {
            Path codexDir =
                    Files.createDirectories(
                            tempDir.resolve(".codex"));
            Files.writeString(
                    codexDir.resolve("config.toml"),
                    "c", StandardCharsets.UTF_8);
            Files.createDirectories(
                    codexDir.resolve("subdir"));

            assertThat(
                    ReadmeUtils.countCodexFiles(codexDir))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("countCodexAgentsFiles")
    class CountCodexAgentsFiles {

        @Test
        @DisplayName("returns 0 when dir missing")
        void zeroWhenMissing(@TempDir Path tempDir) {
            assertThat(ReadmeUtils.countCodexAgentsFiles(
                    tempDir.resolve(".agents")))
                    .isZero();
        }

        @Test
        @DisplayName("counts files recursively")
        void countsRecursive(@TempDir Path tempDir)
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
        void trueForUserInvocableFalse(
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
        void trueForKnowledgePackHeader(
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
        void falseForRegularSkill(
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
        void extractsLeadingDigits() {
            assertThat(
                    ReadmeUtils.extractRuleNumber(
                            "01-project-identity.md"))
                    .isEqualTo("01");
        }

        @Test
        @DisplayName("extracts multi-digit number")
        void extractsMultiDigit() {
            assertThat(
                    ReadmeUtils.extractRuleNumber(
                            "123-some-rule.md"))
                    .isEqualTo("123");
        }

        @Test
        @DisplayName("returns empty for no leading digits")
        void emptyForNoDigits() {
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
        void stripsNumberAndExtension() {
            assertThat(
                    ReadmeUtils.extractRuleScope(
                            "01-project-identity.md"))
                    .isEqualTo("project identity");
        }

        @Test
        @DisplayName("handles filename without number")
        void handlesNoNumber() {
            assertThat(
                    ReadmeUtils.extractRuleScope(
                            "coding-standards.md"))
                    .isEqualTo("coding standards");
        }

        @Test
        @DisplayName("handles single word scope")
        void handlesSingleWord() {
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
        void extractsDescription(@TempDir Path tempDir)
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
        void stripsQuotes(@TempDir Path tempDir)
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
        void emptyWhenNoDescription(@TempDir Path tempDir)
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
        void countsNestedFiles(@TempDir Path tempDir)
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
