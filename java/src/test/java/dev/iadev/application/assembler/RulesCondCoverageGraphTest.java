package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage tests for RulesConditionals —
 * Graph database types (Neo4j, Neptune).
 */
@DisplayName("RulesConditionals — Graph databases")
class RulesCondCoverageGraphTest {

    @Nested
    @DisplayName("copyDatabaseRefs — Neo4j")
    class CopyDatabaseRefsNeo4j {

        @Test
        @DisplayName("neo4j copies graph common + neo4j")
        void copyDatabaseRefs_neo4j_copiesGraphFiles(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neo4j", "5")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path graphCommon = resourceDir.resolve(
                    "knowledge/databases/graph/common");
            Files.createDirectories(graphCommon);
            Files.writeString(
                    graphCommon.resolve(
                            "graph-principles.md"),
                    "Graph principles");
            Path neo4jDir = resourceDir.resolve(
                    "knowledge/databases/graph/neo4j");
            Files.createDirectories(neo4jDir);
            Files.writeString(
                    neo4jDir.resolve(
                            "modeling-patterns.md"),
                    "Neo4j modeling");
            Files.writeString(
                    neo4jDir.resolve(
                            "migration-patterns.md"),
                    "Neo4j migrations");
            Files.writeString(
                    neo4jDir.resolve(
                            "query-optimization.md"),
                    "Neo4j optimization");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(4);
            assertThat(result.get(0))
                    .contains("graph-principles.md");
            assertThat(result.get(1))
                    .contains("migration-patterns.md");
            assertThat(result.get(2))
                    .contains("modeling-patterns.md");
            assertThat(result.get(3))
                    .contains("query-optimization.md");
        }

        @Test
        @DisplayName("neo4j with missing common copies"
                + " only neo4j-specific files")
        void copyDatabaseRefs_neo4j_noCommon(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neo4j", "5")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(
                    resourceDir.resolve(
                            "knowledge/databases"));
            Path neo4jDir = resourceDir.resolve(
                    "knowledge/databases/graph/neo4j");
            Files.createDirectories(neo4jDir);
            Files.writeString(
                    neo4jDir.resolve(
                            "modeling-patterns.md"),
                    "Neo4j modeling");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                    .contains("modeling-patterns.md");
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — Neptune")
    class CopyDatabaseRefsNeptune {

        @Test
        @DisplayName("neptune copies graph common"
                + " + neptune")
        void copyDatabaseRefs_neptune_copiesGraphFiles(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neptune", "1.2")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path graphCommon = resourceDir.resolve(
                    "knowledge/databases/graph/common");
            Files.createDirectories(graphCommon);
            Files.writeString(
                    graphCommon.resolve(
                            "graph-principles.md"),
                    "Graph principles");
            Path neptuneDir = resourceDir.resolve(
                    "knowledge/databases/graph/neptune");
            Files.createDirectories(neptuneDir);
            Files.writeString(
                    neptuneDir.resolve(
                            "modeling-patterns.md"),
                    "Neptune modeling");
            Files.writeString(
                    neptuneDir.resolve(
                            "migration-patterns.md"),
                    "Neptune migrations");
            Files.writeString(
                    neptuneDir.resolve(
                            "query-optimization.md"),
                    "Neptune optimization");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(4);
            assertThat(result.get(0))
                    .contains("graph-principles.md");
        }

        @Test
        @DisplayName("neptune with missing neptune dir"
                + " copies only common")
        void copyDatabaseRefs_neptune_noSpecificDir(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neptune", "1.2")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path graphCommon = resourceDir.resolve(
                    "knowledge/databases/graph/common");
            Files.createDirectories(graphCommon);
            Files.writeString(
                    graphCommon.resolve(
                            "graph-principles.md"),
                    "Graph principles");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(1);
            assertThat(result.get(0))
                    .contains("graph-principles.md");
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — Graph edge cases")
    class CopyDatabaseRefsGraphEdgeCases {

        @Test
        @DisplayName("neo4j with no graph dirs copies"
                + " nothing")
        void copyDatabaseRefs_neo4j_noDirs(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neo4j", "5")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(
                    resourceDir.resolve(
                            "knowledge/databases"));
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("neptune with empty dirs copies"
                + " nothing")
        void copyDatabaseRefs_neptune_emptyDirs(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neptune", "1.2")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(
                    resourceDir.resolve(
                            "knowledge/databases"
                            + "/graph/common"));
            Files.createDirectories(
                    resourceDir.resolve(
                            "knowledge/databases"
                            + "/graph/neptune"));
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).isEmpty();
        }
    }
}
