package dev.iadev.knowledge;

import dev.iadev.application.assembler.RulesConditionals;
import dev.iadev.application.assembler.ConditionalCopyContext;
import dev.iadev.config.ContextBuilder;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.stack.DatabaseSettingsMapping;
import dev.iadev.template.TemplateEngine;
import dev.iadev.testutil.TestConfigBuilder;
import dev.iadev.util.ResourceResolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Time-Series database knowledge files
 * (InfluxDB + TimescaleDB) per story-0023-0007.
 */
@DisplayName("Timeseries Knowledge (story-0023-0007)")
class TimeseriesKnowledgeTest {

    private static final int MAX_LINES = 300;

    private Path resolveResourceDir() {
        return ResourceResolver
                .resolveResourceDir("shared")
                .getParent();
    }

    @Nested
    @DisplayName("@GK-1: timeseries/common/ contains"
            + " exactly 1 principles file")
    class PrinciplesFile {

        @Test
        @DisplayName("timeseries-principles.md exists"
                + " in common directory")
        void common_principlesFile_exists() {
            Path dir = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/common");
            assertThat(dir.resolve(
                    "timeseries-principles.md"))
                    .exists()
                    .isRegularFile();
        }

        @Test
        @DisplayName("common directory contains exactly"
                + " 1 file")
        void common_directory_exactlyOneFile()
                throws IOException {
            Path dir = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/common");
            try (var stream = Files.list(dir)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .toList();
                assertThat(files).hasSize(1);
                assertThat(files.getFirst().getFileName()
                        .toString())
                        .isEqualTo(
                                "timeseries-principles.md");
            }
        }

        @Test
        @DisplayName("principles file covers retention"
                + " policies")
        void principles_content_retentionPolicies() {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/common/"
                            + "timeseries-principles.md");
            assertThat(file).content(StandardCharsets.UTF_8)
                    .containsIgnoringCase("retention");
        }

        @Test
        @DisplayName("principles file covers"
                + " downsampling")
        void principles_content_downsampling() {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/common/"
                            + "timeseries-principles.md");
            assertThat(file).content(StandardCharsets.UTF_8)
                    .containsIgnoringCase("downsampling");
        }

        @Test
        @DisplayName("principles file covers"
                + " cardinality")
        void principles_content_cardinality() {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/common/"
                            + "timeseries-principles.md");
            assertThat(file).content(StandardCharsets.UTF_8)
                    .containsIgnoringCase("cardinality");
        }

        @Test
        @DisplayName("principles file covers"
                + " compression")
        void principles_content_compression() {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/common/"
                            + "timeseries-principles.md");
            assertThat(file).content(StandardCharsets.UTF_8)
                    .containsIgnoringCase("compression");
        }
    }

    @Nested
    @DisplayName("@GK-2: InfluxDB contains 3 files")
    class InfluxDbFiles {

        @Test
        @DisplayName("influxdb directory contains exactly"
                + " 3 files")
        void influxdb_directory_exactlyThreeFiles()
                throws IOException {
            Path dir = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/"
                            + "influxdb");
            try (var stream = Files.list(dir)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .toList();
                assertThat(files).hasSize(3);
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "modeling-patterns.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("influxdb contains file: {0}")
        void influxdb_containsFile(String filename) {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/"
                            + "influxdb/" + filename);
            assertThat(file)
                    .exists()
                    .isRegularFile();
        }
    }

    @Nested
    @DisplayName("@GK-3: TimescaleDB contains 3 files")
    class TimescaleDbFiles {

        @Test
        @DisplayName("timescaledb directory contains"
                + " exactly 3 files")
        void timescaledb_directory_exactlyThreeFiles()
                throws IOException {
            Path dir = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/"
                            + "timescaledb");
            try (var stream = Files.list(dir)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .toList();
                assertThat(files).hasSize(3);
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "modeling-patterns.md",
                "migration-patterns.md",
                "query-optimization.md"
        })
        @DisplayName("timescaledb contains file: {0}")
        void timescaledb_containsFile(String filename) {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/"
                            + "timescaledb/" + filename);
            assertThat(file)
                    .exists()
                    .isRegularFile();
        }
    }

    @Nested
    @DisplayName("@GK-4: TimescaleDB references"
            + " PostgreSQL conventions")
    class TimescaleDbPostgresRef {

        @Test
        @DisplayName("types-and-conventions references"
                + " PostgreSQL")
        void types_content_referencesPostgresql() {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/"
                            + "timescaledb/"
                            + "modeling-patterns.md");
            assertThat(file).content(StandardCharsets.UTF_8)
                    .containsIgnoringCase("postgresql");
        }

        @Test
        @DisplayName("types-and-conventions mentions"
                + " hypertable")
        void types_content_hypertable() {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/"
                            + "timescaledb/"
                            + "modeling-patterns.md");
            assertThat(file).content(StandardCharsets.UTF_8)
                    .containsIgnoringCase("hypertable");
        }

        @Test
        @DisplayName("types-and-conventions mentions"
                + " continuous aggregate")
        void types_content_continuousAggregate() {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/"
                            + "timescaledb/"
                            + "modeling-patterns.md");
            assertThat(file).content(StandardCharsets.UTF_8)
                    .containsIgnoringCase(
                            "continuous aggregate");
        }
    }

    @Nested
    @DisplayName("@GK-5: File line count <= 300")
    class FileLineBudget {

        @ParameterizedTest
        @ValueSource(strings = {
                "common/timeseries-principles.md",
                "influxdb/modeling-patterns.md",
                "influxdb/migration-patterns.md",
                "influxdb/query-optimization.md",
                "timescaledb/modeling-patterns.md",
                "timescaledb/migration-patterns.md",
                "timescaledb/query-optimization.md"
        })
        @DisplayName("{0} does not exceed 300 lines")
        void file_lineCount_withinBudget(String path)
                throws IOException {
            Path file = resolveResourceDir().resolve(
                    "knowledge/databases/timeseries/"
                            + path);
            long lineCount = Files.lines(
                    file, StandardCharsets.UTF_8).count();
            assertThat(lineCount)
                    .as("Line count for %s", path)
                    .isLessThanOrEqualTo(MAX_LINES);
        }
    }

    @Nested
    @DisplayName("@GK-6: Settings files with CLI tools")
    class SettingsFiles {

        @Test
        @DisplayName("database-influxdb.json exists")
        void influxdb_settingsFile_exists() {
            Path settings = resolveResourceDir()
                    .resolve("targets/claude/settings/"
                            + "database-influxdb.json");
            assertThat(settings)
                    .exists()
                    .isRegularFile();
        }

        @Test
        @DisplayName("database-influxdb.json references"
                + " influx CLI")
        void influxdb_settingsFile_referencesInfluxCli() {
            Path settings = resolveResourceDir()
                    .resolve("targets/claude/settings/"
                            + "database-influxdb.json");
            assertThat(settings)
                    .content(StandardCharsets.UTF_8)
                    .contains("influx");
        }

        @Test
        @DisplayName("database-timescaledb.json exists")
        void timescaledb_settingsFile_exists() {
            Path settings = resolveResourceDir()
                    .resolve("targets/claude/settings/"
                            + "database-timescaledb.json");
            assertThat(settings)
                    .exists()
                    .isRegularFile();
        }

        @Test
        @DisplayName("database-timescaledb.json references"
                + " psql")
        void timescaledb_settingsFile_referencesPsql() {
            Path settings = resolveResourceDir()
                    .resolve("targets/claude/settings/"
                            + "database-timescaledb.json");
            assertThat(settings)
                    .content(StandardCharsets.UTF_8)
                    .contains("psql");
        }
    }

    @Nested
    @DisplayName("DatabaseSettingsMapping — timeseries DB entries")
    class DatabaseSettingsMappingEntries {

        @Test
        @DisplayName("DATABASE_SETTINGS_MAP contains"
                + " influxdb entry")
        void databaseSettingsMap_influxdb_present() {
            assertThat(DatabaseSettingsMapping
                    .getDatabaseSettingsKey("influxdb"))
                    .isEqualTo("database-influxdb");
        }

        @Test
        @DisplayName("DATABASE_SETTINGS_MAP contains"
                + " timescaledb entry")
        void databaseSettingsMap_timescaledb_present() {
            assertThat(DatabaseSettingsMapping
                    .getDatabaseSettingsKey("timescaledb"))
                    .isEqualTo("database-timescaledb");
        }

        @Test
        @DisplayName("DATABASE_SETTINGS_MAP has 17 entries"
                + " (all databases)")
        void databaseSettingsMap_size_seventeen() {
            assertThat(DatabaseSettingsMapping
                    .DATABASE_SETTINGS_MAP)
                    .hasSize(17);
        }
    }

    @Nested
    @DisplayName("RulesConditionals — timeseries"
            + " DB type routing")
    class ConditionalsRouting {

        @Test
        @DisplayName("influxdb copies timeseries/common"
                + " and timeseries/influxdb files")
        void copyDbRefs_influxdb_copiesTimeseriesFiles(
                @TempDir Path tempDir) {
            Path resourceDir = resolveResourceDir();
            Path skillsDir = tempDir.resolve("skills");
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("influxdb", "2.7")
                    .build();
            TemplateEngine engine = new TemplateEngine();
            Map<String, Object> context =
                    ContextBuilder.buildContext(config);
            ConditionalCopyContext ctx =
                    new ConditionalCopyContext(
                            config, resourceDir,
                            skillsDir, engine, context);
            List<String> generated =
                    RulesConditionals.copyDatabaseRefs(ctx);
            assertThat(generated).isNotEmpty();
            Path target = skillsDir.resolve(
                    "database-patterns/references");
            assertThat(target.resolve(
                    "timeseries-principles.md"))
                    .exists();
        }

        @Test
        @DisplayName("timescaledb copies"
                + " timeseries/common and"
                + " timeseries/timescaledb files")
        void copyDbRefs_timescaledb_copiesFiles(
                @TempDir Path tempDir) {
            Path resourceDir = resolveResourceDir();
            Path skillsDir = tempDir.resolve("skills");
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("timescaledb", "2.14")
                    .build();
            TemplateEngine engine = new TemplateEngine();
            Map<String, Object> context =
                    ContextBuilder.buildContext(config);
            ConditionalCopyContext ctx =
                    new ConditionalCopyContext(
                            config, resourceDir,
                            skillsDir, engine, context);
            List<String> generated =
                    RulesConditionals.copyDatabaseRefs(ctx);
            assertThat(generated).isNotEmpty();
            Path target = skillsDir.resolve(
                    "database-patterns/references");
            assertThat(target.resolve(
                    "timeseries-principles.md"))
                    .exists();
        }
    }

    @Nested
    @DisplayName("Version Matrix — timeseries section")
    class VersionMatrix {

        @Test
        @DisplayName("version-matrix.md contains"
                + " Time-Series section")
        void versionMatrix_timeseriesSection_present() {
            Path matrix = resolveResourceDir().resolve(
                    "knowledge/databases/"
                            + "version-matrix.md");
            assertThat(matrix)
                    .content(StandardCharsets.UTF_8)
                    .contains("Time-Series");
        }

        @Test
        @DisplayName("version-matrix.md lists InfluxDB")
        void versionMatrix_influxdb_present() {
            Path matrix = resolveResourceDir().resolve(
                    "knowledge/databases/"
                            + "version-matrix.md");
            assertThat(matrix)
                    .content(StandardCharsets.UTF_8)
                    .contains("InfluxDB");
        }

        @Test
        @DisplayName("version-matrix.md lists"
                + " TimescaleDB")
        void versionMatrix_timescaledb_present() {
            Path matrix = resolveResourceDir().resolve(
                    "knowledge/databases/"
                            + "version-matrix.md");
            assertThat(matrix)
                    .content(StandardCharsets.UTF_8)
                    .contains("TimescaleDB");
        }
    }
}
