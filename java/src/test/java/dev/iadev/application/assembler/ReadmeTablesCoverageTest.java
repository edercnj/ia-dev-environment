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
 * Additional coverage tests for ReadmeTables —
 * targeting uncovered branches and edge cases.
 */
@DisplayName("ReadmeTables — coverage")
class ReadmeTablesCoverageTest {

    @Nested
    @DisplayName("buildSkillsTable — edge cases")
    class BuildSkillsTableEdge {

        @Test
        @DisplayName("skills dir with no SKILL.md"
                + " returns fallback")
        void buildTable_noSkillMd_returnsFallback(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir = Files.createDirectories(
                    tempDir.resolve("skills"));
            Files.createDirectories(
                    skillsDir.resolve("x-empty"));

            assertThat(
                    ReadmeTables.buildSkillsTable(tempDir))
                    .isEqualTo("No skills configured.");
        }

        @Test
        @DisplayName("skills with empty description")
        void buildTable_whenCalled_skillsWithEmptyDescription(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir = Files.createDirectories(
                    tempDir.resolve("skills"));
            createSkill(skillsDir, "x-test",
                    "name: x-test\n");

            String table =
                    ReadmeTables.buildSkillsTable(tempDir);

            assertThat(table)
                    .contains("**x-test**")
                    .contains("`/x-test`");
        }
    }

    @Nested
    @DisplayName("buildAgentsTable — edge cases")
    class BuildAgentsTableEdge {

        @Test
        @DisplayName("agents dir empty returns fallback")
        void buildAgentsTable_emptyDir_returnsFallback(
                @TempDir Path tempDir) throws IOException {
            Files.createDirectories(
                    tempDir.resolve("agents"));

            assertThat(
                    ReadmeTables.buildAgentsTable(tempDir))
                    .isEqualTo("No agents configured.");
        }
    }

    @Nested
    @DisplayName("buildKnowledgePacksTable — edge cases")
    class BuildKnowledgePacksTableEdge {

        @Test
        @DisplayName("skills with no KPs returns fallback")
        void buildKnowledgePacksTable_noKps_returnsFallback(@TempDir Path tempDir)
                throws IOException {
            Path skillsDir = Files.createDirectories(
                    tempDir.resolve("skills"));
            createSkill(skillsDir, "x-review",
                    "name: x-review\n"
                            + "description: Review\n");

            assertThat(ReadmeTables
                    .buildKnowledgePacksTable(tempDir))
                    .isEqualTo("No knowledge packs"
                            + " configured.");
        }

        @Test
        @DisplayName("skills dir with no SKILL.md"
                + " returns fallback")
        void buildKnowledgePacksTable_noSkillMd_returnsFallback(
                @TempDir Path tempDir) throws IOException {
            Path skillsDir = Files.createDirectories(
                    tempDir.resolve("skills"));
            Files.createDirectories(
                    skillsDir.resolve("empty-skill"));

            assertThat(ReadmeTables
                    .buildKnowledgePacksTable(tempDir))
                    .isEqualTo("No knowledge packs"
                            + " configured.");
        }
    }

    @Nested
    @DisplayName("buildReadmeHooksSection — edge cases")
    class BuildHooksEdge {

        @Test
        @DisplayName("java with maven returns hooks"
                + " section")
        void buildReadmeHooksSection_whenCalled_javaWithMavenHooks() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .buildTool("maven")
                    .build();

            String section = ReadmeTables
                    .buildReadmeHooksSection(config);

            assertThat(section)
                    .contains("### Post-Compile Check")
                    .contains("`.java`")
                    .contains("./mvnw compile -q");
        }

        @Test
        @DisplayName("rust with cargo returns hooks"
                + " section")
        void buildReadmeHooksSection_whenCalled_rustWithCargoHooks() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("rust", "2024")
                    .framework("axum", "")
                    .buildTool("cargo")
                    .build();

            String section = ReadmeTables
                    .buildReadmeHooksSection(config);

            assertThat(section)
                    .contains("### Post-Compile Check");
        }

        @Test
        @DisplayName("go with go buildTool returns"
                + " hooks section")
        void buildReadmeHooksSection_whenCalled_goHooks() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .language("go", "1.22")
                    .framework("gin", "")
                    .buildTool("go")
                    .build();

            String section = ReadmeTables
                    .buildReadmeHooksSection(config);

            assertThat(section)
                    .contains("### Post-Compile Check");
        }
    }

    @Nested
    @DisplayName("buildMappingTable — no github dir")
    class BuildMappingTableEdge {

        @Test
        @DisplayName("no github dir omits total line")
        void buildMappingTable_noGithubDirOmitsTotal_succeeds(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir = Files.createDirectories(
                    tempDir.resolve(".claude"));

            String table = ReadmeTables
                    .buildMappingTable(claudeDir);

            assertThat(table)
                    .doesNotContain("**Total .github/");
        }
    }

    @Nested
    @DisplayName("buildGenerationSummary — with"
            + " various files")
    class BuildSummaryEdge {

        @Test
        @DisplayName("summary contains claude rows only (no .agents or AGENTS.md)")
        void buildGenerationSummary_whenCalled_containsClaudeRowsOnly(
                @TempDir Path tempDir) throws IOException {
            Path claudeDir = Files.createDirectories(
                    tempDir.resolve(".claude"));

            ProjectConfig config =
                    TestConfigBuilder.minimal();

            String summary = ReadmeTables
                    .buildGenerationSummary(
                            claudeDir, config);

            assertThat(summary)
                    .contains("Rules (.claude)")
                    .contains("Skills (.claude)")
                    .doesNotContain("Skills (.agents)")
                    .doesNotContain("AGENTS.md")
                    .doesNotContain("Codex (.codex)");
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
