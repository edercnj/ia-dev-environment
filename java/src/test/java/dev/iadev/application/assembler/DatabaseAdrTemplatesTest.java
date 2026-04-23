package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.Disabled;
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
 * Tests for story-0023-0014: ADR templates for database
 * decisions in data-modeling knowledge pack.
 *
 * <p>Covers Gherkin scenarios @GK-1 through @GK-6.</p>
 */
@Disabled("EPIC-0051 complete: SkillsAssembler no longer emits KP output under .claude/skills/{kp}/; replaced by KnowledgePackMigrationSmokeTest + KnowledgeAssemblerTest on the new .claude/knowledge/ layout. See ADR-0013.")
@DisplayName("Database ADR Templates")
class DatabaseAdrTemplatesTest {

    private static final String REF_PATH =
            "skills/data-modeling/references/"
                    + "database-adr-templates.md";

    @Nested
    @DisplayName("@GK-1: File existence")
    class FileExistence {

        @Test
        @DisplayName("database-adr-templates.md exists"
                + " in data-modeling references")
        void render_withDatabase_fileExists(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            Path file = tempDir.resolve(REF_PATH);
            assertThat(file).exists();
            assertThat(Files.readString(file,
                    StandardCharsets.UTF_8)).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("@GK-2: SQL vs NoSQL template sections")
    class SqlVsNoSqlTemplate {

        @Test
        @DisplayName("contains SQL vs NoSQL template"
                + " with all 5 mandatory sections")
        void render_sqlVsNosql_hasAllSections(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            assertThat(content)
                    .contains("SQL vs NoSQL database"
                            + " selection");
            assertThat(content)
                    .contains("### Context");
            assertThat(content)
                    .contains("### Decision Drivers");
            assertThat(content)
                    .contains("### Considered Options");
            assertThat(content)
                    .contains("### Decision Outcome");
            assertThat(content)
                    .contains("### Consequences");
        }

        @Test
        @DisplayName("SQL vs NoSQL has at least"
                + " 3 decision drivers")
        void render_sqlVsNosql_hasMinDrivers(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            String sqlSection = extractTemplate(
                    content,
                    "SQL vs NoSQL database selection");
            long driverCount = sqlSection.lines()
                    .filter(l -> l.trim().startsWith("- "))
                    .count();
            assertThat(driverCount)
                    .as("SQL vs NoSQL decision drivers")
                    .isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("@GK-3: Partitioning strategy options")
    class PartitioningTemplate {

        @Test
        @DisplayName("contains Range, Hash, List,"
                + " and Composite partitioning")
        void render_partitioning_allStrategies(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            assertThat(content)
                    .contains("Range partitioning")
                    .contains("Hash partitioning")
                    .contains("List partitioning")
                    .contains("Composite partitioning");
        }
    }

    @Nested
    @DisplayName("@GK-4: Distributed transaction options")
    class DistributedTransactionTemplate {

        @Test
        @DisplayName("contains 2PC, Saga,"
                + " and Outbox options")
        void render_distTx_allOptions(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            assertThat(content)
                    .contains("Two-Phase Commit (2PC)")
                    .contains("Saga pattern")
                    .contains("Transactional Outbox");
        }
    }

    @Nested
    @DisplayName("@GK-5: File line limit")
    class FileLineLimit {

        @Test
        @DisplayName("file has at most 300 lines")
        void render_adrTemplates_maxLines(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            long lineCount = content.lines().count();
            assertThat(lineCount)
                    .as("database-adr-templates.md"
                            + " must be <= 300 lines")
                    .isLessThanOrEqualTo(300);
        }
    }

    @Nested
    @DisplayName("@GK-6: No duplication with"
            + " data-management")
    class NoDuplication {

        @Test
        @DisplayName("does not contain migration"
                + " checklists")
        void render_noDuplication_noMigrationChecklist(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            assertThat(content)
                    .doesNotContain("Pre-Migration")
                    .doesNotContain("rollback script"
                            + " tested on staging");
        }

        @Test
        @DisplayName("does not contain rollback"
                + " procedures")
        void render_noDuplication_noRollbackProcedures(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            assertThat(content)
                    .doesNotContain("Rollback Plan")
                    .doesNotContain(
                            "Rollback communication plan");
        }
    }

    @Nested
    @DisplayName("All 6 templates present")
    class AllTemplatesPresent {

        @Test
        @DisplayName("contains exactly 6 ADR templates")
        void render_allTemplates_sixPresent(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            assertThat(content)
                    .contains("SQL vs NoSQL database"
                            + " selection")
                    .contains("Embedded vs Referenced"
                            + " data model")
                    .contains("Partitioning/sharding"
                            + " strategy selection")
                    .contains("Caching layer selection"
                            + " and topology")
                    .contains("Read replica topology")
                    .contains("Distributed transaction"
                            + " strategy");

            long templateCount = content.lines()
                    .filter(l -> l.startsWith(
                            "## ADR: "))
                    .count();
            assertThat(templateCount)
                    .as("exactly 6 ADR templates")
                    .isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("Each template has 5 sections")
    class TemplateSections {

        @Test
        @DisplayName("Embedded vs Referenced has all"
                + " 5 mandatory sections")
        void render_embeddedRef_hasSections(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            String section = extractTemplate(content,
                    "Embedded vs Referenced data model");
            assertThat(section)
                    .contains("### Context")
                    .contains("### Decision Drivers")
                    .contains("### Considered Options")
                    .contains("### Decision Outcome")
                    .contains("### Consequences");
        }

        @Test
        @DisplayName("Caching layer has all"
                + " 5 mandatory sections")
        void render_caching_hasSections(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            String section = extractTemplate(content,
                    "Caching layer selection and topology");
            assertThat(section)
                    .contains("### Context")
                    .contains("### Decision Drivers")
                    .contains("### Considered Options")
                    .contains("### Decision Outcome")
                    .contains("### Consequences");
        }

        @Test
        @DisplayName("Read replica topology has all"
                + " 5 mandatory sections")
        void render_readReplica_hasSections(
                @TempDir Path tempDir)
                throws IOException {
            String content = renderAndRead(tempDir);

            String section = extractTemplate(content,
                    "Read replica topology");
            assertThat(section)
                    .contains("### Context")
                    .contains("### Decision Drivers")
                    .contains("### Considered Options")
                    .contains("### Decision Outcome")
                    .contains("### Consequences");
        }
    }

    @Nested
    @DisplayName("SKILL.md references ADR templates")
    class SkillMdReference {

        @Test
        @DisplayName("SKILL.md references the ADR"
                + " templates file")
        void render_skillMd_referencesAdr(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String skillContent = Files.readString(
                    tempDir.resolve(
                            "skills/data-modeling/SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(skillContent)
                    .contains("database-adr-templates.md");
        }
    }

    private String renderAndRead(Path tempDir)
            throws IOException {
        ProjectConfig config =
                TestConfigBuilder.builder()
                        .database("postgresql", "16")
                        .build();

        new SkillsAssembler().assemble(
                config, new TemplateEngine(), tempDir);

        return Files.readString(
                tempDir.resolve(REF_PATH),
                StandardCharsets.UTF_8);
    }

    private String extractTemplate(
            String content, String templateTitle) {
        int start = content.indexOf(
                "## ADR: " + templateTitle);
        if (start < 0) {
            return "";
        }
        int nextTemplate = content.indexOf(
                "\n## ADR: ", start + 1);
        if (nextTemplate < 0) {
            return content.substring(start);
        }
        return content.substring(start, nextTemplate);
    }
}
