package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RulesConditionals — new database categories
 * (graph, columnar, newsql, timeseries, search).
 */
@DisplayName("RulesConditionals — new DB categories")
class RulesCondNewCategoriesTest {

    @Nested
    @DisplayName("copyDatabaseRefs — graph databases")
    class GraphDatabases {

        @Test
        @DisplayName("neo4j copies graph common + neo4j")
        void copyDatabaseRefs_neo4j_copiesGraphFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "graph", "neo4j");
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neo4j", "5")
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("graph-common.md");
            assertThat(result.get(1))
                    .contains("neo4j-patterns.md");
        }

        @Test
        @DisplayName("neptune copies graph common + neptune")
        void copyDatabaseRefs_neptune_copiesGraphFiles(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "graph", "neptune");
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neptune", "1")
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("graph-common.md");
            assertThat(result.get(1))
                    .contains("neptune-patterns.md");
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — columnar databases")
    class ColumnarDatabases {

        @Test
        @DisplayName("clickhouse copies columnar files")
        void copyDatabaseRefs_clickhouse_copies(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "columnar", "clickhouse");
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("clickhouse", "24")
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("columnar-common.md");
            assertThat(result.get(1))
                    .contains("clickhouse-patterns.md");
        }

        @Test
        @DisplayName("druid copies columnar files")
        void copyDatabaseRefs_druid_copies(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "columnar", "druid");
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("druid", "30")
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("columnar-common.md");
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — newsql databases")
    class NewSqlDatabases {

        @ParameterizedTest
        @CsvSource({
                "yugabytedb, 2",
                "cockroachdb, 24",
                "tidb, 8"
        })
        @DisplayName("{0} copies newsql common + specific")
        void copyDatabaseRefs_newsql_copies(
                String dbName, String version,
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "newsql", dbName);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database(dbName, version)
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("newsql-common.md");
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — timeseries databases")
    class TimeseriesDatabases {

        @ParameterizedTest
        @CsvSource({
                "influxdb, 3",
                "timescaledb, 2"
        })
        @DisplayName("{0} copies timeseries files")
        void copyDatabaseRefs_timeseries_copies(
                String dbName, String version,
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "timeseries", dbName);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database(dbName, version)
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("timeseries-common.md");
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — search databases")
    class SearchDatabases {

        @ParameterizedTest
        @CsvSource({
                "elasticsearch, 8",
                "opensearch, 2"
        })
        @DisplayName("{0} copies search files")
        void copyDatabaseRefs_search_copies(
                String dbName, String version,
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "search", dbName);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database(dbName, version)
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("search-common.md");
        }
    }

    @Nested
    @DisplayName("backward compatibility")
    class BackwardCompatibility {

        @Test
        @DisplayName("postgresql still routes to sql")
        void copyDatabaseRefs_postgresql_stillSql(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "sql", "postgresql");
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("sql-common.md");
        }

        @Test
        @DisplayName("mongodb still routes to nosql")
        void copyDatabaseRefs_mongodb_stillNosql(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = setupDbFiles(
                    tempDir, "nosql", "mongodb");
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("mongodb", "7")
                    .build();
            Path skillsDir = tempDir.resolve("skills");

            List<String> result = invokeCopyDbRefs(
                    config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("nosql-common.md");
        }
    }

    private static Path setupDbFiles(
            Path tempDir, String category,
            String dbName) throws IOException {
        Path resourceDir = tempDir.resolve("res");
        Path commonDir = resourceDir.resolve(
                "knowledge/databases/"
                        + category + "/common");
        Files.createDirectories(commonDir);
        Files.writeString(
                commonDir.resolve(
                        category + "-common.md"),
                category + " common content");
        Path specificDir = resourceDir.resolve(
                "knowledge/databases/"
                        + category + "/" + dbName);
        Files.createDirectories(specificDir);
        Files.writeString(
                specificDir.resolve(
                        dbName + "-patterns.md"),
                dbName + " patterns content");
        return resourceDir;
    }

    private static List<String> invokeCopyDbRefs(
            ProjectConfig config, Path resourceDir,
            Path skillsDir) {
        return RulesConditionals.copyDatabaseRefs(
                new ConditionalCopyContext(
                        config, resourceDir, skillsDir,
                        new TemplateEngine(), Map.of()));
    }
}
