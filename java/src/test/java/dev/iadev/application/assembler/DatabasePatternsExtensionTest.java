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
 * Tests for database-patterns KP extension and
 * database-engineer agent enhancements per
 * story-0013-0017.
 */
@Disabled("EPIC-0051 complete: SkillsAssembler no longer emits KP output under .claude/skills/{kp}/; replaced by KnowledgePackMigrationSmokeTest + KnowledgeAssemblerTest on the new .claude/knowledge/ layout. See ADR-0013.")
@DisplayName("Database Patterns Extension"
        + " (story-0013-0017)")
class DatabasePatternsExtensionTest {

    @Nested
    @DisplayName("database-patterns KP — existing content"
            + " preserved (RULE-010)")
    class ExistingContentPreserved {

        @Test
        @DisplayName("existing sections remain intact"
                + " after extension")
        void kpExtension_existingSections_preservedIntact(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains(
                            "## Universal Rules (Always Apply)")
                    .contains(
                            "### 1. Schema Naming Conventions")
                    .contains(
                            "### 2. Mandatory Columns (SQL)")
                    .contains("### 3. Data Security")
                    .contains("### 4. Query Best Practices")
                    .contains(
                            "### 5. Connection Pool Sizing")
                    .contains(
                            "### 6. Cache Key Naming"
                                    + " (when cache enabled)")
                    .contains("## Anti-Patterns");
        }

        @Test
        @DisplayName("frontmatter preserved after"
                + " extension")
        void kpExtension_frontmatter_preserved(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("name: database-patterns");
        }
    }

    @Nested
    @DisplayName("database-patterns KP — new sections"
            + " added")
    class NewSectionsAdded {

        @Test
        @DisplayName("Connection Pool Management"
                + " section present")
        void kpExtension_connectionPool_sectionPresent(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains(
                            "## Connection Pool Management")
                    .contains("connectionTimeout")
                    .contains("idleTimeout")
                    .contains("maxLifetime");
        }

        @Test
        @DisplayName("Index Management section present")
        void kpExtension_indexManagement_sectionPresent(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## Index Management")
                    .contains("Partial Indexes")
                    .contains("Covering Indexes");
        }

        @Test
        @DisplayName("Maintenance Operations section"
                + " present")
        void kpExtension_maintenance_sectionPresent(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## Maintenance Operations")
                    .contains("VACUUM")
                    .contains("ANALYZE")
                    .contains("REINDEX");
        }

        @Test
        @DisplayName("Data Governance Patterns section"
                + " present")
        void kpExtension_dataGovernance_sectionPresent(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## Data Governance Patterns")
                    .contains("Classification")
                    .contains("Retention")
                    .contains("Audit");
        }

        @Test
        @DisplayName("Backup Strategy Patterns section"
                + " present")
        void kpExtension_backupStrategy_sectionPresent(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("## Backup Strategy Patterns")
                    .contains("pg_dump")
                    .contains("Point-in-Time Recovery")
                    .contains("WAL");
        }

        @Test
        @DisplayName("new sections appear after existing"
                + " Anti-Patterns section")
        void kpExtension_newSections_afterExisting(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "SKILL.md"),
                    StandardCharsets.UTF_8);
            int antiPatternsIdx =
                    content.indexOf("## Anti-Patterns");
            int connectionPoolIdx =
                    content.indexOf(
                            "## Connection Pool Management");
            assertThat(connectionPoolIdx)
                    .isGreaterThan(antiPatternsIdx);
        }
    }

    @Nested
    @DisplayName("database-patterns KP — reference file")
    class ReferenceFile {

        @Test
        @DisplayName("connection-pool-tuning.md reference"
                + " exists")
        void kpExtension_reference_connectionPoolExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/database-patterns/references/"
                            + "connection-pool-tuning.md"))
                    .exists();
        }

        @Test
        @DisplayName("connection-pool-tuning.md has sizing"
                + " formulas")
        void kpExtension_reference_hasSizingFormulas(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "skills/database-patterns/"
                                    + "references/"
                                    + "connection-pool-tuning"
                                    + ".md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("core_count")
                    .contains("HikariCP")
                    .contains("asyncpg");
        }
    }

    @Nested
    @DisplayName("database-patterns KP — not generated"
            + " without database")
    class NotGeneratedWithoutDb {

        @Test
        @DisplayName("KP not generated when database=none")
        void kpExtension_noDb_notGenerated(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "skills/database-patterns"))
                    .doesNotExist();
        }
    }

    @Nested
    @DisplayName("database-engineer agent — 18-point"
            + " severity checklist")
    class AgentSeverityChecklist {

        @Test
        @DisplayName("agent has 18-point checklist with"
                + " severity classification")
        void agent_checklist_has18PointsWithSeverity(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/database-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains(
                            "18-Point Severity Checklist");
        }

        @Test
        @DisplayName("CRITICAL items 1-6 classified"
                + " correctly")
        void agent_checklist_criticalItems(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/database-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("CRITICAL (1-6)")
                    .contains("Schema design normalized");
        }

        @Test
        @DisplayName("MEDIUM items 7-13 classified"
                + " correctly")
        void agent_checklist_mediumItems(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/database-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("MEDIUM (7-13)")
                    .contains("No raw SQL");
        }

        @Test
        @DisplayName("LOW items 14-18 classified"
                + " correctly")
        void agent_checklist_lowItems(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/database-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("LOW (14-18)")
                    .contains("Transaction scope minimized");
        }

        @Test
        @DisplayName("agent not generated without"
                + " database")
        void agent_noDb_notGenerated(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .eventDriven(false)
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            assertThat(outputDir.resolve(
                    "agents/database-engineer.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("agent preserves existing 30-point"
                + " checklist")
        void agent_existing30PointChecklist_preserved(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/database-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("30-Point Database Checklist");
        }

        @Test
        @DisplayName("agent has integration notes with"
                + " x-review")
        void agent_integrationNotes_present(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            AgentsAssembler assembler =
                    new AgentsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            assembler.assemble(
                    config, new TemplateEngine(),
                    outputDir);
            String content = Files.readString(
                    outputDir.resolve(
                            "agents/database-engineer.md"),
                    StandardCharsets.UTF_8);
            assertThat(content)
                    .contains("x-review")
                    .contains("database-patterns");
        }
    }
}
