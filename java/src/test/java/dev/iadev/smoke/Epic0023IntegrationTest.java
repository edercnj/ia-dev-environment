package dev.iadev.smoke;

import dev.iadev.application.assembler.AssemblerPipeline;
import dev.iadev.application.assembler.KnowledgePackSelection;
import dev.iadev.application.assembler.PipelineOptions;
import dev.iadev.application.assembler.RulesConditionals;
import dev.iadev.config.ConfigProfiles;
import dev.iadev.domain.model.PipelineResult;
import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.stack.StackMapping;
import dev.iadev.testutil.TestConfigBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Epic-0023 integration verification test validating
 * the complete database architecture standards expansion.
 *
 * <p>Covers all acceptance criteria from story-0023-0013:
 * StackMapping completeness, RulesConditionals routing,
 * KnowledgePackSelection, review checklist expansion,
 * new config profiles, and backward compatibility.</p>
 */
@DisplayName("Epic-0023 Integration Verification")
class Epic0023IntegrationTest {

    @Nested
    @DisplayName("StackMapping — 17 databases")
    class StackMappingCompleteness {

        private static final Set<String> ALL_DATABASES =
                Set.of(
                        "postgresql", "mysql", "oracle",
                        "mongodb", "cassandra",
                        "neo4j", "neptune",
                        "clickhouse", "druid",
                        "yugabytedb", "cockroachdb",
                        "tidb",
                        "influxdb", "timescaledb",
                        "elasticsearch", "opensearch",
                        "eventstoredb");

        @Test
        @DisplayName("DATABASE_SETTINGS_MAP has 17 entries")
        void databaseSettingsMap_size_seventeen() {
            assertThat(StackMapping.DATABASE_SETTINGS_MAP)
                    .hasSize(17);
        }

        @Test
        @DisplayName("all 17 databases are registered")
        void databaseSettingsMap_allRegistered() {
            assertThat(StackMapping.DATABASE_SETTINGS_MAP
                    .keySet())
                    .containsExactlyInAnyOrderElementsOf(
                            ALL_DATABASES);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "postgresql", "mysql", "oracle",
                "mongodb", "cassandra"
        })
        @DisplayName("original 5 databases preserved")
        void databaseSettingsMap_original5_preserved(
                String db) {
            assertThat(StackMapping.DATABASE_SETTINGS_MAP)
                    .containsKey(db);
        }
    }

    @Nested
    @DisplayName("RulesConditionals — 7 categories")
    class RulesConditionalsRouting {

        @ParameterizedTest
        @CsvSource({
                "postgresql, sql",
                "mysql, sql",
                "oracle, sql"
        })
        @DisplayName("{0} routes to {1} category")
        void copyDbTypeFiles_sql_routesCorrectly(
                String db, String category,
                @TempDir Path tempDir) throws IOException {
            verifyRouting(db, category, tempDir);
        }

        @ParameterizedTest
        @CsvSource({
                "mongodb, nosql",
                "cassandra, nosql",
                "eventstoredb, nosql"
        })
        @DisplayName("{0} routes to {1} category")
        void copyDbTypeFiles_nosql_routesCorrectly(
                String db, String category,
                @TempDir Path tempDir) throws IOException {
            verifyRouting(db, category, tempDir);
        }

        @ParameterizedTest
        @CsvSource({
                "neo4j, graph",
                "neptune, graph"
        })
        @DisplayName("{0} routes to {1} category")
        void copyDbTypeFiles_graph_routesCorrectly(
                String db, String category,
                @TempDir Path tempDir) throws IOException {
            verifyRouting(db, category, tempDir);
        }

        @ParameterizedTest
        @CsvSource({
                "clickhouse, columnar",
                "druid, columnar"
        })
        @DisplayName("{0} routes to {1} category")
        void copyDbTypeFiles_columnar_routesCorrectly(
                String db, String category,
                @TempDir Path tempDir) throws IOException {
            verifyRouting(db, category, tempDir);
        }

        @ParameterizedTest
        @CsvSource({
                "yugabytedb, newsql",
                "cockroachdb, newsql",
                "tidb, newsql"
        })
        @DisplayName("{0} routes to {1} category")
        void copyDbTypeFiles_newsql_routesCorrectly(
                String db, String category,
                @TempDir Path tempDir) throws IOException {
            verifyRouting(db, category, tempDir);
        }

        @ParameterizedTest
        @CsvSource({
                "influxdb, timeseries",
                "timescaledb, timeseries"
        })
        @DisplayName("{0} routes to {1} category")
        void copyDbTypeFiles_timeseries_routesCorrectly(
                String db, String category,
                @TempDir Path tempDir) throws IOException {
            verifyRouting(db, category, tempDir);
        }

        @ParameterizedTest
        @CsvSource({
                "elasticsearch, search",
                "opensearch, search"
        })
        @DisplayName("{0} routes to {1} category")
        void copyDbTypeFiles_search_routesCorrectly(
                String db, String category,
                @TempDir Path tempDir) throws IOException {
            verifyRouting(db, category, tempDir);
        }

        private void verifyRouting(
                String db, String category,
                Path tempDir) throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path commonDir = resourceDir.resolve(
                    "knowledge/databases/"
                            + category + "/common");
            Files.createDirectories(commonDir);
            Files.writeString(
                    commonDir.resolve(
                            category + "-common.md"),
                    category + " common");
            Path dbDir = resourceDir.resolve(
                    "knowledge/databases/"
                            + category + "/" + db);
            Files.createDirectories(dbDir);
            Files.writeString(
                    dbDir.resolve(db + "-ref.md"),
                    db + " reference");

            ProjectConfig config = TestConfigBuilder
                    .builder().database(db, "1").build();
            Path skillsDir = tempDir.resolve("skills");

            var ctx = new dev.iadev.application.assembler
                    .ConditionalCopyContext(
                    config, resourceDir, skillsDir,
                    new dev.iadev.template.TemplateEngine(),
                    java.util.Map.of());
            List<String> result = RulesConditionals
                    .copyDatabaseRefs(ctx);

            assertThat(result)
                    .as("Routing for %s to %s", db,
                            category)
                    .isNotEmpty();
        }
    }

    @Nested
    @DisplayName("KnowledgePackSelection")
    class KnowledgePackSelectionTests {

        @Test
        @DisplayName("database != none includes "
                + "data-modeling")
        void selectKnowledgePacks_withDb_includesDataModeling() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "16")
                    .build();

            List<String> packs = KnowledgePackSelection
                    .selectKnowledgePacks(config);

            assertThat(packs).contains("data-modeling");
        }

        @Test
        @DisplayName("database = none excludes "
                + "data-modeling")
        void selectKnowledgePacks_noDb_excludesDataModeling() {
            ProjectConfig config = TestConfigBuilder
                    .builder().build();

            List<String> packs = KnowledgePackSelection
                    .selectKnowledgePacks(config);

            assertThat(packs)
                    .doesNotContain("data-modeling");
        }

        @Test
        @DisplayName("database != none includes "
                + "database-patterns")
        void selectKnowledgePacks_withDb_includesDbPatterns() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("neo4j", "5")
                    .build();

            List<String> packs = KnowledgePackSelection
                    .selectKnowledgePacks(config);

            assertThat(packs)
                    .contains("database-patterns");
        }
    }

    @Nested
    @DisplayName("New config profiles — pipeline")
    class NewConfigProfiles {

        @ParameterizedTest
        @ValueSource(strings = {
                "java-spring-neo4j",
                "java-spring-clickhouse",
                "python-fastapi-timescale",
                "java-spring-elasticsearch"
        })
        @DisplayName("{0} pipeline succeeds")
        void newProfile_pipelineSucceeds(
                String profile,
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve(profile);
            Files.createDirectories(outputDir);

            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options = new PipelineOptions(
                    false, true, false, null);

            PipelineResult result = pipeline.runPipeline(
                    config, outputDir, options);

            assertThat(result.success())
                    .as("Pipeline for %s must succeed",
                            profile)
                    .isTrue();
            assertThat(result.filesGenerated())
                    .as("Pipeline for %s must generate "
                            + "files", profile)
                    .hasSizeGreaterThan(10);
            assertThat(result.filesGenerated())
                    .as("Pipeline for %s generates "
                            + "settings.json", profile)
                    .anyMatch(f -> f.contains(
                            "settings.json"));
            assertThat(result.filesGenerated())
                    .as("Pipeline for %s generates "
                            + "rules", profile)
                    .anyMatch(f -> f.contains(
                            "rules/"));
        }

        @Test
        @DisplayName("java-spring-neo4j generates "
                + "database knowledge")
        void javaSpringNeo4j_generatesDbKnowledge(
                @TempDir Path tempDir) throws IOException {
            Path outputDir = tempDir.resolve("neo4j-kp");
            Files.createDirectories(outputDir);
            ProjectConfig config = ConfigProfiles.getStack(
                    "java-spring-neo4j");
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options = new PipelineOptions(
                    false, true, false, null);

            PipelineResult result = pipeline.runPipeline(
                    config, outputDir, options);

            assertThat(result.filesGenerated())
                    .as("neo4j profile generates "
                            + "database-patterns "
                            + "knowledge files")
                    .anyMatch(f -> f.contains(
                            "database-patterns"));
        }

        @Test
        @DisplayName("java-spring-neo4j has DB version 5")
        void javaSpringNeo4j_hasDbVersion() {
            ProjectConfig config = ConfigProfiles
                    .getStack("java-spring-neo4j");

            assertThat(config.data().database().version())
                    .isEqualTo("5");
        }

        @Test
        @DisplayName("java-spring-clickhouse has "
                + "DB version 24")
        void javaSpringClickhouse_hasDbVersion() {
            ProjectConfig config = ConfigProfiles
                    .getStack("java-spring-clickhouse");

            assertThat(config.data().database().version())
                    .isEqualTo("24");
        }

        @Test
        @DisplayName("python-fastapi-timescale has "
                + "DB version 2")
        void pythonFastapiTimescale_hasDbVersion() {
            ProjectConfig config = ConfigProfiles
                    .getStack("python-fastapi-timescale");

            assertThat(config.data().database().version())
                    .isEqualTo("2");
        }

        @Test
        @DisplayName("java-spring-elasticsearch has "
                + "DB version 8")
        void javaSpringElasticsearch_hasDbVersion() {
            ProjectConfig config = ConfigProfiles
                    .getStack("java-spring-elasticsearch");

            assertThat(config.data().database().version())
                    .isEqualTo("8");
        }
    }

    @Nested
    @DisplayName("x-review checklist expansion")
    class ReviewChecklistExpansion {

        @Test
        @DisplayName("Database checklist has 20 items")
        void reviewSkill_databaseChecklist_has20Items()
                throws IOException {
            String content = readReviewSkill();

            assertThat(content)
                    .contains("Database (20 items, /40)");
        }

        @Test
        @DisplayName("Data Modeling specialist has "
                + "10 items")
        void reviewSkill_dataModeling_has10Items()
                throws IOException {
            String content = readReviewSkill();

            assertThat(content)
                    .contains(
                            "Data Modeling (10 items, /20)");
        }

        @Test
        @DisplayName("Data Modeling activation condition "
                + "present")
        void reviewSkill_dataModelingActivation()
                throws IOException {
            String content = readReviewSkill();

            assertThat(content)
                    .contains("Activation condition");
        }

        private String readReviewSkill()
                throws IOException {
            var url = getClass().getClassLoader()
                    .getResource(
                            "targets/claude/skills/"
                                    + "core/x-review/"
                                    + "SKILL.md");
            assertThat(url)
                    .as("x-review SKILL.md on classpath")
                    .isNotNull();
            return Files.readString(
                    Path.of(url.getPath()),
                    StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("Knowledge files completeness")
    class KnowledgeFilesCompleteness {

        @Test
        @DisplayName("58 knowledge .md files across "
                + "7 categories")
        void knowledgeFiles_58FilesAcross7Categories()
                throws IOException {
            Path dbRoot = resolveSourcePath(
                    "knowledge/databases");
            long count = countMdFiles(dbRoot);

            assertThat(count)
                    .as("58 knowledge .md files across "
                            + "7 database categories "
                            + "(excluding cache and "
                            + "version-matrix)")
                    .isEqualTo(58);
        }

        @Test
        @DisplayName("17 settings files exist")
        void settingsFiles_17Exist() throws IOException {
            Path settingsDir = resolveSourcePath(
                    "targets/claude/settings");
            long count = Files.list(settingsDir)
                    .filter(p -> p.getFileName()
                            .toString()
                            .startsWith("database-"))
                    .count();

            assertThat(count)
                    .as("17 database settings JSON files")
                    .isEqualTo(17);
        }

        private Path resolveSourcePath(String sub) {
            var url = getClass().getClassLoader()
                    .getResource(sub);
            assertThat(url).isNotNull();
            return Path.of(url.getPath());
        }

        private long countMdFiles(Path root)
                throws IOException {
            return Files.walk(root)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString()
                            .endsWith(".md"))
                    .filter(p -> !p.toString()
                            .contains("/cache/"))
                    .filter(p -> !p.getFileName()
                            .toString()
                            .equals("version-matrix.md"))
                    .count();
        }
    }

    @Nested
    @DisplayName("Backward compatibility")
    class BackwardCompatibility {

        private static final List<String>
                ORIGINAL_8_PROFILES = List.of(
                "go-gin",
                "java-quarkus",
                "java-spring",
                "kotlin-ktor",
                "python-click-cli",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs");

        @ParameterizedTest
        @ValueSource(strings = {
                "go-gin", "java-quarkus",
                "java-spring", "kotlin-ktor",
                "python-click-cli", "python-fastapi",
                "rust-axum", "typescript-nestjs"
        })
        @DisplayName("{0} profile loads successfully")
        void originalProfile_loadsSuccessfully(
                String profile) {
            ProjectConfig config =
                    ConfigProfiles.getStack(profile);

            assertThat(config).isNotNull();
            assertThat(config.project().name())
                    .isNotBlank();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "go-gin", "java-quarkus",
                "java-spring", "kotlin-ktor",
                "python-click-cli", "python-fastapi",
                "rust-axum", "typescript-nestjs"
        })
        @DisplayName("{0} pipeline produces output")
        void originalProfile_pipelineSucceeds(
                String profile,
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve(profile);
            ProjectConfig config =
                    ConfigProfiles.getStack(profile);
            AssemblerPipeline pipeline =
                    new AssemblerPipeline(
                            AssemblerPipeline
                                    .buildAssemblers());
            PipelineOptions options = new PipelineOptions(
                    false, true, false, null);

            PipelineResult result = pipeline.runPipeline(
                    config, outputDir, options);

            assertThat(result.success())
                    .as("Pipeline for %s must succeed",
                            profile)
                    .isTrue();
        }
    }
}
