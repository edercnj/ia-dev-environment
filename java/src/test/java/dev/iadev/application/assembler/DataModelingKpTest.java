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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0023-0001: data-modeling knowledge pack
 * with cross-cutting patterns (schema design, concurrency,
 * test data).
 *
 * <p>Covers Gherkin scenarios @GK-1 through @GK-5.</p>
 */
@DisplayName("Data Modeling Knowledge Pack")
class DataModelingKpTest {

    @Nested
    @DisplayName("@GK-1: Exclusion when no database")
    class ExclusionNoDB {

        @Test
        @DisplayName("excludes data-modeling when"
                + " database is none")
        void select_noDatabase_excludesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .doesNotContain("data-modeling");
        }

        @Test
        @DisplayName("output dir does not contain"
                + " data-modeling when database is none")
        void render_noDatabase_notGenerated(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            Path kpDir = tempDir.resolve(
                    "skills/data-modeling");
            assertThat(kpDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("@GK-2: Inclusion with PostgreSQL")
    class InclusionPostgres {

        @Test
        @DisplayName("includes data-modeling when"
                + " database is postgresql")
        void select_postgresql_includesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("data-modeling");
        }

        @Test
        @DisplayName("includes both data-modeling and"
                + " database-patterns with postgresql")
        void select_postgresql_includesBothPacks() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("data-modeling")
                    .contains("database-patterns");
        }

        @Test
        @DisplayName("data-modeling output dir exists"
                + " when database is postgresql")
        void render_postgresql_generated(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            Path kpDir = tempDir.resolve(
                    "skills/data-modeling");
            assertThat(kpDir).exists();
        }
    }

    @Nested
    @DisplayName("@GK-2b: Inclusion with other databases")
    class InclusionOtherDatabases {

        @Test
        @DisplayName("includes data-modeling when"
                + " database is mysql")
        void select_mysql_includesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("mysql", "8")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("data-modeling");
        }

        @Test
        @DisplayName("includes data-modeling when"
                + " only cache is configured")
        void select_cacheOnly_includesPack() {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .cache("redis", "7")
                            .build();

            List<String> packs =
                    KnowledgePackSelection
                            .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("data-modeling");
        }
    }

    @Nested
    @DisplayName("@GK-3: SKILL.md content sections")
    class SkillMdContent {

        @Test
        @DisplayName("SKILL.md contains Schema Design"
                + " Patterns section")
        void render_skillMd_hasSchemaDesign(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "data-modeling/SKILL.md");
            assertThat(content)
                    .contains("Schema Design Patterns");
        }

        @Test
        @DisplayName("SKILL.md contains Concurrency"
                + " Patterns section")
        void render_skillMd_hasConcurrency(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "data-modeling/SKILL.md");
            assertThat(content)
                    .contains("Concurrency Patterns");
        }

        @Test
        @DisplayName("SKILL.md contains Test Data"
                + " Patterns section")
        void render_skillMd_hasTestData(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "data-modeling/SKILL.md");
            assertThat(content)
                    .contains("Test Data Patterns");
        }

        @Disabled("story-0051-0003: old KP frontmatter contract (user-invocable: false, etc.) replaced by Rule-051-07; test will be rewritten or removed when core consumers are retrofitted")
        @Test
        @DisplayName("SKILL.md has user-invocable false"
                + " in frontmatter")
        void render_skillMd_hasUserInvocableFalse(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "data-modeling/SKILL.md");
            assertThat(content)
                    .contains("user-invocable: false");
        }

        @Test
        @DisplayName("SKILL.md has correct name"
                + " in frontmatter")
        void render_skillMd_hasCorrectName(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "data-modeling/SKILL.md");
            assertThat(content)
                    .contains("name: data-modeling");
        }

        @Test
        @DisplayName("rendered SKILL.md has no Pebble"
                + " directives")
        void render_skillMd_noPebbleDirectives(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readSkill(tempDir,
                    "data-modeling/SKILL.md");
            assertThat(content)
                    .doesNotContain("{%");
        }
    }

    @Nested
    @DisplayName("@GK-4: Reference files max 300 lines")
    class ReferenceLineLimit {

        @Test
        @DisplayName("schema-design-patterns.md has"
                + " at most 300 lines")
        void render_schemaDesign_maxLines(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            long lines = countLines(tempDir,
                    "data-modeling/references/"
                            + "schema-design-patterns.md");
            assertThat(lines)
                    .as("schema-design-patterns.md"
                            + " must be <= 300 lines")
                    .isLessThanOrEqualTo(300);
        }

        @Test
        @DisplayName("concurrency-patterns.md has"
                + " at most 300 lines")
        void render_concurrency_maxLines(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            long lines = countLines(tempDir,
                    "data-modeling/references/"
                            + "concurrency-patterns.md");
            assertThat(lines)
                    .as("concurrency-patterns.md"
                            + " must be <= 300 lines")
                    .isLessThanOrEqualTo(300);
        }

        @Test
        @DisplayName("test-data-patterns.md has"
                + " at most 300 lines")
        void render_testData_maxLines(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            long lines = countLines(tempDir,
                    "data-modeling/references/"
                            + "test-data-patterns.md");
            assertThat(lines)
                    .as("test-data-patterns.md"
                            + " must be <= 300 lines")
                    .isLessThanOrEqualTo(300);
        }
    }

    @Nested
    @DisplayName("@GK-5: No duplication with core 11")
    class NoDuplication {

        @Test
        @DisplayName("SKILL.md does not contain ACID"
                + " definition")
        void render_skillMd_noAcidDefinition(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readAllDataModeling(tempDir);
            assertThat(content)
                    .doesNotContain("Atomicity, Consistency,"
                            + " Isolation, Durability");
        }

        @Test
        @DisplayName("references do not contain CAP"
                + " theorem definition")
        void render_references_noCapTheorem(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readAllDataModeling(tempDir);
            assertThat(content)
                    .doesNotContain("CAP theorem");
        }

        @Test
        @DisplayName("references do not contain basic"
                + " normalization definitions")
        void render_references_noBasicNormalization(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String content = readAllDataModeling(tempDir);
            assertThat(content)
                    .doesNotContain("First Normal Form")
                    .doesNotContain("Second Normal Form")
                    .doesNotContain("Third Normal Form");
        }
    }

    @Nested
    @DisplayName("References directory structure")
    class ReferencesStructure {

        @Test
        @DisplayName("references directory exists with"
                + " three reference files")
        void render_references_threeFilesExist(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            Path refsDir = tempDir.resolve(
                    "skills/data-modeling/references");
            assertThat(refsDir).exists();
            assertThat(refsDir.resolve(
                    "schema-design-patterns.md")).exists();
            assertThat(refsDir.resolve(
                    "concurrency-patterns.md")).exists();
            assertThat(refsDir.resolve(
                    "test-data-patterns.md")).exists();
        }

        @Test
        @DisplayName("reference files have no Pebble"
                + " directives")
        void render_references_noPebbleDirectives(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();

            new SkillsAssembler().assemble(
                    config, new TemplateEngine(), tempDir);

            String schema = readRef(tempDir,
                    "schema-design-patterns.md");
            String concurrency = readRef(tempDir,
                    "concurrency-patterns.md");
            String testData = readRef(tempDir,
                    "test-data-patterns.md");

            String combined =
                    schema + concurrency + testData;
            assertThat(combined)
                    .doesNotContain("{%");
        }
    }

    private String readSkill(Path outputDir, String path)
            throws IOException {
        return Files.readString(
                outputDir.resolve("skills/" + path),
                StandardCharsets.UTF_8);
    }

    private String readRef(Path outputDir, String filename)
            throws IOException {
        return Files.readString(
                outputDir.resolve(
                        "skills/data-modeling/references/"
                                + filename),
                StandardCharsets.UTF_8);
    }

    private String readAllDataModeling(Path outputDir)
            throws IOException {
        String skill = readSkill(outputDir,
                "data-modeling/SKILL.md");
        String schema = readRef(outputDir,
                "schema-design-patterns.md");
        String concurrency = readRef(outputDir,
                "concurrency-patterns.md");
        String testData = readRef(outputDir,
                "test-data-patterns.md");
        return skill + "\n" + schema
                + "\n" + concurrency + "\n" + testData;
    }

    private long countLines(Path outputDir, String path)
            throws IOException {
        return Files.readString(
                outputDir.resolve("skills/" + path),
                StandardCharsets.UTF_8)
                .lines().count();
    }
}
