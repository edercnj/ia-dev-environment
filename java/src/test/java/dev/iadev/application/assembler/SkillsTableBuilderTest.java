package dev.iadev.application.assembler;

import dev.iadev.domain.model.ProjectConfig;
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
 * Tests for SkillsTableBuilder — builds markdown tables
 * for skills, agents, rules, knowledge packs, and hooks.
 */
@DisplayName("SkillsTableBuilder")
class SkillsTableBuilderTest {

    private final SkillsTableBuilder builder =
            new SkillsTableBuilder();

    @Nested
    @DisplayName("buildRulesTable")
    class BuildRulesTable {

        @Test
        @DisplayName("returns fallback when dir missing")
        void build_whenMissing_fallback(@TempDir Path tempDir) {
            assertThat(
                    builder.buildRulesTable(tempDir))
                    .isEqualTo("No rules configured.");
        }

        @Test
        @DisplayName("returns fallback when dir empty")
        void build_whenEmpty_fallback(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve("rules"));

            assertThat(
                    builder.buildRulesTable(tempDir))
                    .isEqualTo("No rules configured.");
        }

        @Test
        @DisplayName("builds table with rows")
        void build_withRows_buildsTable(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            Files.writeString(
                    rulesDir.resolve(
                            "01-project-identity.md"),
                    "content", StandardCharsets.UTF_8);

            String table =
                    builder.buildRulesTable(tempDir);

            assertThat(table)
                    .contains("| # | File | Scope |")
                    .contains("|---|------|-------|")
                    .contains("| 01 | `01-project-"
                            + "identity.md`"
                            + " | project identity |");
        }
    }

    @Nested
    @DisplayName("buildSkillsTable")
    class BuildSkillsTable {

        @Test
        @DisplayName("returns fallback when dir missing")
        void build_whenMissing_fallback(@TempDir Path tempDir) {
            assertThat(
                    builder.buildSkillsTable(tempDir))
                    .isEqualTo("No skills configured.");
        }

        @Test
        @DisplayName("excludes knowledge packs")
        void build_whenCalled_excludesKnowledgePacks(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "description: Review skill");
            createSkill(skillsDir, "coding-standards",
                    "description: Coding\n"
                            + "user-invocable: false");

            String table =
                    builder.buildSkillsTable(tempDir);

            assertThat(table)
                    .contains("**x-review**")
                    .doesNotContain("coding-standards");
        }

        @Test
        @DisplayName("returns fallback when all KPs")
        void build_whenAllKps_fallback(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "coding",
                    "user-invocable: false\n"
                            + "description: Internal");

            assertThat(
                    builder.buildSkillsTable(tempDir))
                    .isEqualTo("No skills configured.");
        }

        @Test
        @DisplayName("no SKILL.md returns fallback")
        void build_noSkillMd_returnsFallback(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir = Files.createDirectories(
                    tempDir.resolve("skills"));
            Files.createDirectories(
                    skillsDir.resolve("x-empty"));

            assertThat(
                    builder.buildSkillsTable(tempDir))
                    .isEqualTo("No skills configured.");
        }
    }

    @Nested
    @DisplayName("buildAgentsTable")
    class BuildAgentsTable {

        @Test
        @DisplayName("returns fallback when dir missing")
        void build_whenMissing_fallback(@TempDir Path tempDir) {
            assertThat(
                    builder.buildAgentsTable(tempDir))
                    .isEqualTo("No agents configured.");
        }

        @Test
        @DisplayName("returns fallback when dir empty")
        void build_whenEmpty_fallback(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve("agents"));

            assertThat(
                    builder.buildAgentsTable(tempDir))
                    .isEqualTo("No agents configured.");
        }

        @Test
        @DisplayName("builds table with name and file")
        void build_withNameAndFile_buildsTable(
                @TempDir Path tempDir) throws IOException {
            Path agentsDir =
                    Files.createDirectories(
                            tempDir.resolve("agents"));
            Files.writeString(
                    agentsDir.resolve("architect.md"),
                    "c", StandardCharsets.UTF_8);

            String table =
                    builder.buildAgentsTable(tempDir);

            assertThat(table)
                    .contains("| Agent | File |")
                    .contains("| **architect** |"
                            + " `architect.md` |");
        }
    }

    @Nested
    @DisplayName("buildKnowledgePacksTable")
    class BuildKnowledgePacksTable {

        @Test
        @DisplayName("returns fallback when dir missing")
        void build_whenMissing_fallback(@TempDir Path tempDir) {
            assertThat(
                    builder.buildKnowledgePacksTable(
                            tempDir))
                    .isEqualTo("No knowledge packs"
                            + " configured.");
        }

        @Test
        @DisplayName("includes only knowledge packs")
        void build_whenCalled_includesOnlyKps(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "description: Review");
            createSkill(skillsDir, "architecture",
                    "user-invocable: false\n"
                            + "description: Arch");

            String table = builder
                    .buildKnowledgePacksTable(tempDir);

            assertThat(table)
                    .contains("| `architecture` |")
                    .doesNotContain("x-review");
        }

        @Test
        @DisplayName("no KPs returns fallback")
        void build_noKps_returnsFallback(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir = Files.createDirectories(
                    tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "description: Review");

            assertThat(builder
                    .buildKnowledgePacksTable(tempDir))
                    .isEqualTo("No knowledge packs"
                            + " configured.");
        }
    }

    @Nested
    @DisplayName("buildReadmeHooksSection")
    class BuildReadmeHooksSection {

        @Test
        @DisplayName("kotlin returns hooks section")
        void build_kotlin_returnsHooksSection() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("kotlin", "2.0")
                    .framework("ktor", "")
                    .buildTool("gradle")
                    .build();

            String section =
                    builder.buildReadmeHooksSection(config);

            assertThat(section)
                    .contains("### Post-Compile Check")
                    .contains("PostToolUse")
                    .contains("`.kt`");
        }

        @Test
        @DisplayName("python returns fallback")
        void build_python_returnsFallback() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .buildTool("pip")
                    .build();

            assertThat(
                    builder.buildReadmeHooksSection(config))
                    .isEqualTo("No hooks configured.");
        }

        @Test
        @DisplayName("java returns hooks section")
        void build_java_returnsHooksSection() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .build();

            String section =
                    builder.buildReadmeHooksSection(config);

            assertThat(section)
                    .contains("### Post-Compile Check")
                    .contains("`.java`");
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
