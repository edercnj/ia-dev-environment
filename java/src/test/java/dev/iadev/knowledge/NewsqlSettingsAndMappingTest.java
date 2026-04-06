package dev.iadev.knowledge;

import dev.iadev.application.assembler.ConditionalCopyContext;
import dev.iadev.application.assembler.RulesConditionals;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.stack.StackMapping;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for NewSQL settings files, StackMapping entries,
 * and RulesConditionals NewSQL integration.
 */
@DisplayName("NewSQL Settings and Mapping")
class NewsqlSettingsAndMappingTest {

    private static final Path SETTINGS_DIR =
            Path.of("src/main/resources/"
                    + "targets/claude/settings");

    @Nested
    @DisplayName("@GK-6: Settings files")
    class SettingsFiles {

        @Test
        @DisplayName("database-yugabytedb.json exists")
        void yugaSettings_exists() {
            assertThat(SETTINGS_DIR.resolve(
                    "database-yugabytedb.json")).exists();
        }

        @Test
        @DisplayName("database-yugabytedb.json has ysqlsh")
        void yugaSettings_containsYsqlsh()
                throws IOException {
            String content = Files.readString(
                    SETTINGS_DIR.resolve(
                            "database-yugabytedb.json"));
            assertThat(content).contains("ysqlsh");
        }

        @Test
        @DisplayName("database-cockroachdb.json exists")
        void cockroachSettings_exists() {
            assertThat(SETTINGS_DIR.resolve(
                    "database-cockroachdb.json")).exists();
        }

        @Test
        @DisplayName("database-cockroachdb.json has "
                + "cockroach sql")
        void cockroachSettings_containsCockroachSql()
                throws IOException {
            String content = Files.readString(
                    SETTINGS_DIR.resolve(
                            "database-cockroachdb.json"));
            assertThat(content).contains("cockroach sql");
        }

        @Test
        @DisplayName("database-tidb.json exists")
        void tidbSettings_exists() {
            assertThat(SETTINGS_DIR.resolve(
                    "database-tidb.json")).exists();
        }

        @Test
        @DisplayName("database-tidb.json has mysql")
        void tidbSettings_containsMysql()
                throws IOException {
            String content = Files.readString(
                    SETTINGS_DIR.resolve(
                            "database-tidb.json"));
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c).contains("mysql"),
                    c -> assertThat(c)
                            .contains("tidb-client"));
        }
    }

    @Nested
    @DisplayName("StackMapping — NewSQL entries")
    class StackMappingEntries {

        @ParameterizedTest
        @CsvSource({
                "yugabytedb, database-yugabytedb",
                "cockroachdb, database-cockroachdb",
                "tidb, database-tidb"
        })
        @DisplayName("getDatabaseSettingsKey({0}) = {1}")
        void getDatabaseSettingsKey_newsql_correct(
                String dbName, String expected) {
            assertThat(
                    StackMapping.getDatabaseSettingsKey(
                            dbName))
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("DATABASE_SETTINGS_MAP has 17 entries")
        void databaseSettingsMap_hasSeventeenEntries() {
            assertThat(StackMapping.DATABASE_SETTINGS_MAP)
                    .hasSize(17);
        }
    }

    @Nested
    @DisplayName("RulesConditionals — NewSQL types")
    class RulesConditionalsNewSql {

        @ParameterizedTest
        @ValueSource(strings = {
                "yugabytedb", "cockroachdb", "tidb"
        })
        @DisplayName("copyDatabaseRefs copies NewSQL "
                + "common + {0}")
        void copyDatabaseRefs_newsql_copiesFiles(
                String dbName,
                @TempDir Path tempDir)
                throws IOException {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database(dbName, "latest")
                    .build();
            Path resourceDir = tempDir.resolve("res");
            Path newsqlCommon = resourceDir.resolve(
                    "knowledge/databases/newsql/common");
            Files.createDirectories(newsqlCommon);
            Files.writeString(
                    newsqlCommon.resolve(
                            "newsql-principles.md"),
                    "NewSQL principles");
            Path newsqlDb = resourceDir.resolve(
                    "knowledge/databases/newsql/" + dbName);
            Files.createDirectories(newsqlDb);
            Files.writeString(
                    newsqlDb.resolve(
                            "types-and-conventions.md"),
                    "Types content");
            Files.writeString(
                    newsqlDb.resolve(
                            "migration-patterns.md"),
                    "Migration content");
            Files.writeString(
                    newsqlDb.resolve(
                            "query-optimization.md"),
                    "Query content");
            Path skillsDir = tempDir.resolve("skills");
            List<String> result =
                    RulesConditionals.copyDatabaseRefs(
                            new ConditionalCopyContext(
                                    config, resourceDir,
                                    skillsDir,
                                    new TemplateEngine(),
                                    Map.of()));
            assertThat(result).hasSize(4);
            assertThat(result).anyMatch(
                    f -> f.contains(
                            "newsql-principles.md"));
            assertThat(result).anyMatch(
                    f -> f.contains(
                            "types-and-conventions.md"));
        }
    }
}
