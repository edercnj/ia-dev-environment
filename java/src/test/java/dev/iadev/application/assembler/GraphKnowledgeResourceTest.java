package dev.iadev.application.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies graph database knowledge resource files
 * and settings files exist with correct content.
 */
@DisplayName("Graph Database Knowledge Resources")
class GraphKnowledgeResourceTest {

    private static final String GRAPH_BASE =
            "knowledge/databases/graph/";
    private static final String SETTINGS_BASE =
            "targets/claude/settings/";
    private static final int MAX_LINES = 300;

    @Nested
    @DisplayName("graph/common/ files")
    class GraphCommon {

        @Test
        @DisplayName("graph-principles.md exists and"
                + " has graph vs relational section")
        void graphPrinciples_exists_hasDecisionSection() {
            String content = readResource(
                    GRAPH_BASE
                            + "common/graph-principles.md");
            assertThat(content)
                    .isNotEmpty()
                    .containsIgnoringCase(
                            "graph vs relational")
                    .containsIgnoringCase("fan-out")
                    .containsIgnoringCase("traversal");
        }

        @Test
        @DisplayName("graph-principles.md has max"
                + " 300 lines")
        void graphPrinciples_withinLineLimit() {
            long lineCount = countLines(
                    GRAPH_BASE
                            + "common/graph-principles.md");
            assertThat(lineCount)
                    .isLessThanOrEqualTo(MAX_LINES);
        }
    }

    @Nested
    @DisplayName("graph/neo4j/ files")
    class Neo4jFiles {

        @ParameterizedTest
        @ValueSource(strings = {
            "modeling-patterns.md",
            "migration-patterns.md",
            "query-optimization.md"
        })
        @DisplayName("neo4j knowledge file exists"
                + " and is non-empty")
        void neo4jFile_exists_nonEmpty(String filename) {
            String content = readResource(
                    GRAPH_BASE + "neo4j/" + filename);
            assertThat(content).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "modeling-patterns.md",
            "migration-patterns.md",
            "query-optimization.md"
        })
        @DisplayName("neo4j knowledge file has max"
                + " 300 lines")
        void neo4jFile_withinLineLimit(String filename) {
            long lineCount = countLines(
                    GRAPH_BASE + "neo4j/" + filename);
            assertThat(lineCount)
                    .isLessThanOrEqualTo(MAX_LINES);
        }

        @Test
        @DisplayName("modeling-patterns.md contains"
                + " Cypher conventions")
        void modelingPatterns_containsCypher() {
            String content = readResource(
                    GRAPH_BASE
                            + "neo4j/modeling-patterns.md");
            assertThat(content)
                    .containsIgnoringCase("cypher")
                    .containsIgnoringCase("node")
                    .containsIgnoringCase("relationship");
        }

        @Test
        @DisplayName("migration-patterns.md contains"
                + " APOC or migration patterns")
        void migrationPatterns_containsMigration() {
            String content = readResource(
                    GRAPH_BASE
                            + "neo4j/migration-patterns.md");
            assertThat(content)
                    .containsIgnoringCase("apoc")
                    .containsIgnoringCase("constraint");
        }

        @Test
        @DisplayName("query-optimization.md contains"
                + " PROFILE/EXPLAIN")
        void queryOptimization_containsProfile() {
            String content = readResource(
                    GRAPH_BASE
                            + "neo4j/"
                            + "query-optimization.md");
            assertThat(content)
                    .containsIgnoringCase("profile")
                    .containsIgnoringCase("explain")
                    .containsIgnoringCase("index");
        }
    }

    @Nested
    @DisplayName("graph/neptune/ files")
    class NeptuneFiles {

        @ParameterizedTest
        @ValueSource(strings = {
            "modeling-patterns.md",
            "migration-patterns.md",
            "query-optimization.md"
        })
        @DisplayName("neptune knowledge file exists"
                + " and is non-empty")
        void neptuneFile_exists_nonEmpty(
                String filename) {
            String content = readResource(
                    GRAPH_BASE + "neptune/" + filename);
            assertThat(content).isNotEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "modeling-patterns.md",
            "migration-patterns.md",
            "query-optimization.md"
        })
        @DisplayName("neptune knowledge file has max"
                + " 300 lines")
        void neptuneFile_withinLineLimit(
                String filename) {
            long lineCount = countLines(
                    GRAPH_BASE + "neptune/" + filename);
            assertThat(lineCount)
                    .isLessThanOrEqualTo(MAX_LINES);
        }

        @Test
        @DisplayName("modeling-patterns.md contains"
                + " Gremlin vs SPARQL")
        void modelingPatterns_containsGremlinSparql() {
            String content = readResource(
                    GRAPH_BASE
                            + "neptune/"
                            + "modeling-patterns.md");
            assertThat(content)
                    .containsIgnoringCase("gremlin")
                    .containsIgnoringCase("sparql");
        }

        @Test
        @DisplayName("migration-patterns.md contains"
                + " bulk loader")
        void migrationPatterns_containsBulkLoader() {
            String content = readResource(
                    GRAPH_BASE
                            + "neptune/"
                            + "migration-patterns.md");
            assertThat(content)
                    .containsIgnoringCase("bulk")
                    .containsIgnoringCase("s3");
        }

        @Test
        @DisplayName("query-optimization.md contains"
                + " DFE engine")
        void queryOptimization_containsDfe() {
            String content = readResource(
                    GRAPH_BASE
                            + "neptune/"
                            + "query-optimization.md");
            assertThat(content)
                    .containsIgnoringCase("dfe")
                    .containsIgnoringCase("query hint");
        }
    }

    @Nested
    @DisplayName("Settings files")
    class SettingsFiles {

        @Test
        @DisplayName("database-neo4j.json exists"
                + " with cypher-shell permission")
        void neo4jSettings_containsCypherShell() {
            String content = readResource(
                    SETTINGS_BASE + "database-neo4j.json");
            assertThat(content)
                    .contains("cypher-shell");
        }

        @Test
        @DisplayName("database-neptune.json exists"
                + " with aws neptune permission")
        void neptuneSettings_containsAwsNeptune() {
            String content = readResource(
                    SETTINGS_BASE
                            + "database-neptune.json");
            assertThat(content)
                    .contains("aws neptune");
        }
    }

    @Nested
    @DisplayName("File name parity between"
            + " neo4j and neptune")
    class FileNameParity {

        @Test
        @DisplayName("neo4j and neptune have identical"
                + " file names")
        void fileNames_identical() {
            List<String> expectedFiles = List.of(
                    "modeling-patterns.md",
                    "migration-patterns.md",
                    "query-optimization.md");
            for (String filename : expectedFiles) {
                assertThat(resourceExists(
                        GRAPH_BASE + "neo4j/" + filename))
                        .as("neo4j/%s exists", filename)
                        .isTrue();
                assertThat(resourceExists(
                        GRAPH_BASE
                                + "neptune/" + filename))
                        .as("neptune/%s exists", filename)
                        .isTrue();
            }
        }
    }

    private String readResource(String path) {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream(path);
        assertThat(is)
                .as("Resource %s must exist", path)
                .isNotNull();
        return new String(
                readAllBytes(is), StandardCharsets.UTF_8);
    }

    private boolean resourceExists(String path) {
        return getClass().getClassLoader()
                .getResourceAsStream(path) != null;
    }

    private long countLines(String path) {
        InputStream is = getClass().getClassLoader()
                .getResourceAsStream(path);
        assertThat(is)
                .as("Resource %s must exist", path)
                .isNotNull();
        return new BufferedReader(
                new InputStreamReader(
                        is, StandardCharsets.UTF_8))
                .lines().count();
    }

    private byte[] readAllBytes(InputStream is) {
        try {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to read resource", e);
        }
    }
}
