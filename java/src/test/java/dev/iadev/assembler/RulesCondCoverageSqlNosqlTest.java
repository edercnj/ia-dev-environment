package dev.iadev.assembler;

import dev.iadev.model.ProjectConfig;
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
 * SQL types, NoSQL types, version matrix, cache.
 */
@DisplayName("RulesConditionals — SQL + NoSQL + cache")
class RulesCondCoverageSqlNosqlTest {

    @Nested
    @DisplayName("copyDatabaseRefs — SQL types")
    class CopyDatabaseRefsSql {

        @Test
        @DisplayName("mysql copies SQL common + mysql")
        void assemble_mysql_copiesSqlFiles(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("mysql", "8")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path sqlCommon = resourceDir.resolve(
                    "databases/sql/common");
            Files.createDirectories(sqlCommon);
            Files.writeString(
                    sqlCommon.resolve("sql-common.md"),
                    "SQL common");
            Path sqlMysql = resourceDir.resolve(
                    "databases/sql/mysql");
            Files.createDirectories(sqlMysql);
            Files.writeString(
                    sqlMysql.resolve("mysql-types.md"),
                    "MySQL types");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .contains("sql-common.md");
            assertThat(result.get(1))
                    .contains("mysql-types.md");
        }

        @Test
        @DisplayName("oracle copies SQL common + oracle")
        void assemble_oracle_copiesSqlFiles(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("oracle", "19")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path sqlCommon = resourceDir.resolve(
                    "databases/sql/common");
            Files.createDirectories(sqlCommon);
            Files.writeString(
                    sqlCommon.resolve("sql-base.md"),
                    "SQL base");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("copyDatabaseRefs — NoSQL types")
    class CopyDatabaseRefsNosql {

        @Test
        @DisplayName("cassandra copies NoSQL common")
        void copyDatabaseRefs_cassandra_copies(
                @TempDir Path tempDir) throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("cassandra", "4")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path nosqlCommon = resourceDir.resolve(
                    "databases/nosql/common");
            Files.createDirectories(nosqlCommon);
            Files.writeString(
                    nosqlCommon.resolve("nosql-base.md"),
                    "NoSQL base");
            Path cassandra = resourceDir.resolve(
                    "databases/nosql/cassandra");
            Files.createDirectories(cassandra);
            Files.writeString(
                    cassandra.resolve(
                            "cassandra-patterns.md"),
                    "Cassandra patterns");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("unknown db type copies no files")
        void copyDatabaseRefs_unknownDb_noCopy(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("h2", "2")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(
                    resourceDir.resolve("databases"));
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

    @Nested
    @DisplayName("copyDatabaseRefs — version matrix")
    class VersionMatrix {

        @Test
        @DisplayName("no version matrix returns empty")
        void copyDatabaseRefs_noVersionMatrix(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(
                    resourceDir.resolve("databases"));
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).noneMatch(
                    f -> f.contains("version-matrix"));
        }
    }

    @Nested
    @DisplayName("copyCacheRefs — edge cases")
    class CopyCacheRefsEdgeCases {

        @Test
        @DisplayName("common missing returns specific")
        void copyCacheRefs_commonMissing(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cache("redis", "7.4")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path cacheRedis = resourceDir.resolve(
                    "databases/cache/redis");
            Files.createDirectories(cacheRedis);
            Files.writeString(
                    cacheRedis.resolve("redis.md"),
                    "Redis content");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyCacheRefs(
                            config, resourceDir,
                            skillsDir);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("specific missing returns common")
        void copyCacheRefs_specificMissing(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cache("memcached", "1.6")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path common = resourceDir.resolve(
                    "databases/cache/common");
            Files.createDirectories(common);
            Files.writeString(
                    common.resolve("cache-basics.md"),
                    "Cache basics");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyCacheRefs(
                            config, resourceDir,
                            skillsDir);
            assertThat(result).hasSize(1);
        }
    }
}
