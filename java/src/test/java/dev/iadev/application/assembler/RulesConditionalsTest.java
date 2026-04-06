package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RulesConditionals — conditional rule generation.
 */
@DisplayName("RulesConditionals")
class RulesConditionalsTest {

    @Nested
    @DisplayName("copyDatabaseRefs")
    class CopyDatabaseRefs {

        @Test
        @DisplayName("no database returns empty list")
        void create_noDatabase_returnsEmpty(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .minimal();
            Path resourceDir = tempDir.resolve("res");
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
        @DisplayName("postgresql copies SQL files")
        void create_postgresql_copiesSqlFiles(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path sqlCommon = resourceDir.resolve(
                    "knowledge/databases/sql/common");
            Files.createDirectories(sqlCommon);
            Files.writeString(
                    sqlCommon.resolve("sql-principles.md"),
                    "SQL content");
            Path sqlPg = resourceDir.resolve(
                    "knowledge/databases/sql/postgresql");
            Files.createDirectories(sqlPg);
            Files.writeString(
                    sqlPg.resolve("types.md"),
                    "PG types");

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
                    .contains("sql-principles.md");
            assertThat(result.get(1))
                    .contains("types.md");
        }

        @Test
        @DisplayName("mongodb copies NoSQL files")
        void create_mongodb_copiesNosqlFiles(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("mongodb", "7")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path nosqlCommon = resourceDir.resolve(
                    "knowledge/databases/nosql/common");
            Files.createDirectories(nosqlCommon);
            Files.writeString(
                    nosqlCommon.resolve(
                            "nosql-principles.md"),
                    "NoSQL content");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));

            assertThat(result).isNotEmpty();
            assertThat(result.get(0))
                    .contains("nosql-principles.md");
        }

        @Test
        @DisplayName("unknown database returns empty list")
        void create_unknownDatabase_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("unknowndb", "1")
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

            assertThat(result)
                    .as("unknown database should "
                            + "produce empty result")
                    .isEmpty();
        }

        @Test
        @DisplayName("copies version matrix when present")
        void create_whenCalled_copiesVersionMatrix(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path dbDir = resourceDir.resolve("knowledge/databases");
            Files.createDirectories(dbDir);
            Files.writeString(
                    dbDir.resolve("version-matrix.md"),
                    "Version matrix content");

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
                    .contains("version-matrix.md");
        }
    }

    @Nested
    @DisplayName("copyCacheRefs")
    class CopyCacheRefs {

        @Test
        @DisplayName("no cache returns empty list")
        void create_noCache_returnsEmpty(@TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .minimal();
            Path resourceDir = tempDir.resolve("res");
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyCacheRefs(
                            config, resourceDir, skillsDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("unknown cache returns empty list")
        void create_unknownCache_returnsEmpty(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cache("hazelcast", "5")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyCacheRefs(
                            config, resourceDir, skillsDir);

            assertThat(result)
                    .as("unknown cache should produce "
                            + "empty result")
                    .isEmpty();
        }

        @Test
        @DisplayName("redis copies cache files")
        void create_redis_copiesCacheFiles(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .cache("redis", "7.4")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path cacheCommon = resourceDir.resolve(
                    "knowledge/databases/cache/common");
            Files.createDirectories(cacheCommon);
            Files.writeString(
                    cacheCommon.resolve(
                            "cache-principles.md"),
                    "Cache content");
            Path cacheRedis = resourceDir.resolve(
                    "knowledge/databases/cache/redis");
            Files.createDirectories(cacheRedis);
            Files.writeString(
                    cacheRedis.resolve(
                            "redis-patterns.md"),
                    "Redis patterns");

            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.copyCacheRefs(
                            config, resourceDir, skillsDir);

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("assembleSecurityRules")
    class AssembleSecurityRules {

        @Test
        @DisplayName("no frameworks returns empty")
        void create_noFrameworks_returnsEmpty(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .minimal();
            Path resourceDir = tempDir.resolve("res");
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleSecurityRules(
                            config, resourceDir, skillsDir);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("path traversal framework skipped")
        void create_pathTraversal_skipped(
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .securityFrameworks("../../../etc")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path secDir = resourceDir.resolve(
                    "knowledge/security");
            Files.createDirectories(secDir);
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals.assembleSecurityRules(
                            config, resourceDir, skillsDir);

            assertThat(result)
                    .as("path traversal framework "
                            + "must be skipped")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("assembleCloudKnowledge")
    class AssembleCloudKnowledge {

        @Test
        @DisplayName("no cloud provider returns empty")
        void create_noProvider_returnsEmpty(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .minimal();
            Path resourceDir = tempDir.resolve("res");
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals
                            .assembleCloudKnowledge(
                                    config, resourceDir,
                                    skillsDir);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("assembleInfraKnowledge")
    class AssembleInfraKnowledge {

        @Test
        @DisplayName("default config returns empty")
        void create_defaultConfig_returnsEmpty(
                @TempDir Path tempDir) {
            ProjectConfig config = TestConfigBuilder
                    .minimal();
            Path resourceDir = tempDir.resolve("res");
            Path skillsDir = tempDir.resolve("skills");

            List<String> result =
                    RulesConditionals
                            .assembleInfraKnowledge(
                                    config, resourceDir,
                                    skillsDir);

            assertThat(result).isEmpty();
        }
    }
}
