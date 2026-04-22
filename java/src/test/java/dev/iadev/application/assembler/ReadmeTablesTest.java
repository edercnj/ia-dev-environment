package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

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
 * Tests for ReadmeTables -- markdown table and section
 * builders for README.md.
 */
@DisplayName("ReadmeTables")
class ReadmeTablesTest {

    @Nested
    @DisplayName("buildRulesTable")
    class BuildRulesTable {

        @Test
        @DisplayName("returns fallback when dir missing")
        void create_whenMissing_fallback(@TempDir Path tempDir) {
            assertThat(
                    ReadmeTables.buildRulesTable(tempDir))
                    .isEqualTo("No rules configured.");
        }

        @Test
        @DisplayName("returns fallback when dir empty")
        void create_whenEmpty_fallback(@TempDir Path tempDir)
                throws IOException {
            Files.createDirectories(
                    tempDir.resolve("rules"));

            assertThat(
                    ReadmeTables.buildRulesTable(tempDir))
                    .isEqualTo("No rules configured.");
        }

        @Test
        @DisplayName("builds table with header and rows")
        void create_withRows_buildsTable(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            Files.writeString(
                    rulesDir.resolve(
                            "01-project-identity.md"),
                    "content", StandardCharsets.UTF_8);
            Files.writeString(
                    rulesDir.resolve("02-domain.md"),
                    "content", StandardCharsets.UTF_8);

            String table =
                    ReadmeTables.buildRulesTable(tempDir);

            assertThat(table)
                    .contains("| # | File | Scope |")
                    .contains("|---|------|-------|")
                    .contains("| 01 | `01-project-"
                            + "identity.md`"
                            + " | project identity |")
                    .contains("| 02 | `02-domain.md`"
                            + " | domain |");
        }

        @Test
        @DisplayName("table has correct number of rows")
        void create_whenCalled_correctRowCount(@TempDir Path tempDir)
                throws IOException {
            Path rulesDir =
                    Files.createDirectories(
                            tempDir.resolve("rules"));
            for (int i = 1; i <= 5; i++) {
                Files.writeString(
                        rulesDir.resolve(
                                String.format(
                                        "%02d-rule-%d.md",
                                        i, i)),
                        "c", StandardCharsets.UTF_8);
            }

            String table =
                    ReadmeTables.buildRulesTable(tempDir);
            // Header + separator + 5 data rows
            String[] lines = table.split("\n");

            assertThat(lines).hasSize(7);
        }
    }

    @Nested
    @DisplayName("buildSkillsTable")
    class BuildSkillsTable {

        @Test
        @DisplayName("returns fallback when dir missing")
        void create_whenMissing_fallback(@TempDir Path tempDir) {
            assertThat(
                    ReadmeTables.buildSkillsTable(tempDir))
                    .isEqualTo("No skills configured.");
        }

        @Test
        @DisplayName("excludes knowledge packs")
        void create_whenCalled_excludesKnowledgePacks(@TempDir Path tempDir)
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
                    ReadmeTables.buildSkillsTable(tempDir);

            assertThat(table)
                    .contains("**x-review**")
                    .doesNotContain("coding-standards");
        }

        @Test
        @DisplayName("includes skill path and description")
        void create_whenCalled_includesPathAndDescription(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "description: Review code");

            String table =
                    ReadmeTables.buildSkillsTable(tempDir);

            assertThat(table)
                    .contains("| Skill | Path |"
                            + " Description |")
                    .contains("|-------|------|"
                            + "-------------|")
                    .contains("| **x-review** |"
                            + " `/x-review` |"
                            + " Review code |");
        }

        @Test
        @DisplayName("returns fallback when all skills"
                + " are knowledge packs")
        void create_whenAllKps_fallback(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "coding",
                    "user-invocable: false\n"
                            + "description: Internal");

            assertThat(
                    ReadmeTables.buildSkillsTable(tempDir))
                    .isEqualTo("No skills configured.");
        }
    }

    @Nested
    @DisplayName("buildAgentsTable")
    class BuildAgentsTable {

        @Test
        @DisplayName("returns fallback when dir missing")
        void create_whenMissing_fallback(@TempDir Path tempDir) {
            assertThat(
                    ReadmeTables.buildAgentsTable(tempDir))
                    .isEqualTo("No agents configured.");
        }

        @Test
        @DisplayName("builds table with agent name and"
                + " file")
        void create_withNameAndFile_buildsTable(
                @TempDir Path tempDir) throws IOException {
            Path agentsDir =
                    Files.createDirectories(
                            tempDir.resolve("agents"));
            Files.writeString(
                    agentsDir.resolve("architect.md"),
                    "c", StandardCharsets.UTF_8);
            Files.writeString(
                    agentsDir.resolve("tech-lead.md"),
                    "c", StandardCharsets.UTF_8);

            String table =
                    ReadmeTables.buildAgentsTable(tempDir);

            assertThat(table)
                    .contains("| Agent | File |")
                    .contains("|-------|------|")
                    .contains("| **architect** |"
                            + " `architect.md` |")
                    .contains("| **tech-lead** |"
                            + " `tech-lead.md` |");
        }
    }

    @Nested
    @DisplayName("buildKnowledgePacksTable")
    class BuildKnowledgePacksTable {

        @Test
        @DisplayName("returns fallback when dir missing")
        void create_whenMissing_fallback(@TempDir Path tempDir) {
            assertThat(ReadmeTables
                    .buildKnowledgePacksTable(tempDir))
                    .isEqualTo("No knowledge packs"
                            + " configured.");
        }

        @Test
        @DisplayName("includes only knowledge packs")
        void create_whenCalled_includesOnlyKps(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir =
                    Files.createDirectories(
                            tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "description: Review");
            createSkill(skillsDir, "architecture",
                    "user-invocable: false\n"
                            + "description: Arch");

            String table = ReadmeTables
                    .buildKnowledgePacksTable(tempDir);

            assertThat(table)
                    .contains("| Pack | Usage |")
                    .contains("|------|-------|")
                    .contains("| `architecture` |"
                            + " Referenced internally"
                            + " by agents |")
                    .doesNotContain("x-review");
        }
    }

    @Nested
    @DisplayName("buildReadmeHooksSection")
    class BuildReadmeHooksSection {

        // create_forKotlin_returnsSection removed in EPIC-0048
        // full cleanup — kotlin no longer supported.

        @Test
        @DisplayName("returns fallback for python")
        void create_forPython_returnsFallback() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("python", "3.12")
                    .framework("fastapi", "0.115")
                    .buildTool("pip")
                    .build();

            assertThat(ReadmeTables
                    .buildReadmeHooksSection(config))
                    .isEqualTo("No hooks configured.");
        }
    }

    @Nested
    @DisplayName("buildSettingsSection")
    class BuildSettingsSection {

        @Test
        @DisplayName("contains settings.json description")
        void create_whenCalled_containsSettingsJsonDesc() {
            String section =
                    ReadmeTables.buildSettingsSection();

            assertThat(section)
                    .contains("### settings.json")
                    .contains("permissions.allow")
                    .contains("### settings.local.json")
                    .contains("Local overrides");
        }
    }

    @Nested
    @DisplayName("buildMappingTable")
    class BuildMappingTable {

        @Test
        @DisplayName("contains 6 mapping rows")
        void create_whenCalled_containsSixMappingRows(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir =
                    Files.createDirectories(
                            tempDir.resolve(".claude"));

            String table = ReadmeTables
                    .buildMappingTable(claudeDir);

            // After Codex removal the cross-platform
            // mapping table is vacuous and renders empty.
            assertThat(table).isEmpty();
        }

        @Test
        @DisplayName("empty result after codex removal")
        void create_whenCalled_returnsEmpty(@TempDir Path tempDir)
                throws IOException {
            Path claudeDir =
                    Files.createDirectories(
                            tempDir.resolve(".claude"));

            String table = ReadmeTables
                    .buildMappingTable(claudeDir);

            assertThat(table).isEmpty();
        }
    }

    @Nested
    @DisplayName("buildGenerationSummary")
    class BuildGenerationSummary {

        @Test
        @DisplayName("contains 7 component rows")
        void create_whenCalled_containsSevenComponents(
                @TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String summary = ReadmeTables
                    .buildGenerationSummary(
                            claudeDir, config);

            assertThat(summary)
                    .contains("| Component | Count |")
                    .contains("|-----------|-------|");
            int dataRows = 0;
            for (String line : summary.split("\n")) {
                if (line.startsWith("| ")
                        && !line.startsWith("| Component")
                        && !line.startsWith("|---")) {
                    dataRows++;
                }
            }
            assertThat(dataRows).isEqualTo(7);
        }

        @Test
        @DisplayName("contains ia-dev-env version")
        void create_whenCalled_containsVersion(@TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String summary = ReadmeTables
                    .buildGenerationSummary(
                            claudeDir, config);

            assertThat(summary)
                    .contains("Generated by `ia-dev-env"
                            + " v0.1.0`.");
        }

        @Test
        @DisplayName("includes all component labels")
        void create_whenCalled_includesAllLabels(@TempDir Path tempDir)
                throws IOException {
            Path claudeDir = setupMinimalOutput(tempDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String summary = ReadmeTables
                    .buildGenerationSummary(
                            claudeDir, config);

            assertThat(summary)
                    .contains("Rules (.claude)")
                    .contains("Skills (.claude)")
                    .contains("Knowledge Packs (.claude)")
                    .contains("Agents (.claude)")
                    .contains("Hooks (.claude)")
                    .contains("Settings (.claude)")
                    .doesNotContain("AGENTS.md")
                    .doesNotContain("Skills (.agents)")
                    .doesNotContain("Codex (.codex)");
        }

        private Path setupMinimalOutput(Path tempDir)
                throws IOException {
            return Files.createDirectories(
                    tempDir.resolve(".claude"));
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
