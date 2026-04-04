package dev.iadev.application.assembler;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DataMigrationPlanAssembler -- conditional
 * generation of docs/templates/_TEMPLATE-DATA-MIGRATION-PLAN.md.
 *
 * <p>TPP order: degenerate (no DB) -> simple (PostgreSQL/Flyway)
 * -> conditional (MongoDB/Mongock) -> all sections -> cross-profile
 * comparison.</p>
 */
@DisplayName("DataMigrationPlanAssembler")
class DataMigrationPlanAssemblerTest {

    private static final String OUTPUT_PATH =
            "docs/templates/_TEMPLATE-DATA-MIGRATION-PLAN.md";

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssembler() {
            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("conditional generation -- database=none")
    class DatabaseNone {

        @Test
        @DisplayName("returns empty list when database is none")
        void assemble_databaseNone_returnsEmptyList(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create output when database none")
        void assemble_databaseNone_doesNotCreateOutput(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir.resolve(OUTPUT_PATH))
                    .doesNotExist();
        }

        @Test
        @DisplayName("returns empty for python-click-cli profile")
        void assemble_pythonClickCli_returnsEmptyList(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("click", "8.1")
                            .buildTool("pip")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("PostgreSQL + Flyway generation")
    class PostgresqlFlyway {

        @Test
        @DisplayName("generates template for PostgreSQL+Flyway")
        void assemble_postgresqlFlyway_generatesFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(outputDir.resolve(OUTPUT_PATH))
                    .exists();
        }

        @Test
        @DisplayName("contains Flyway commands")
        void assemble_postgresqlFlyway_containsFlywayCommands(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content).contains("flyway info");
            assertThat(content).contains("flyway migrate");
            assertThat(content).contains("flyway validate");
            assertThat(content)
                    .contains("Flyway Pre-Migration");
        }

        @Test
        @DisplayName("contains SQL validation queries")
        void assemble_postgresqlFlyway_containsSqlQueries(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("SQL Validation Queries");
            assertThat(content).contains("SELECT COUNT(*)");
            assertThat(content).contains("pg_constraint");
        }

        @Test
        @DisplayName("does NOT contain Alembic commands")
        void assemble_postgresqlFlyway_doesNotContainAlembic(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .doesNotContain("Alembic Pre-Migration");
            assertThat(content)
                    .doesNotContain("alembic upgrade");
        }

        @Test
        @DisplayName("does NOT contain MongoDB queries")
        void assemble_postgresqlFlyway_doesNotContainMongo(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .doesNotContain("MongoDB Validation");
            assertThat(content)
                    .doesNotContain("db.<collection>");
        }

        @Test
        @DisplayName("contains Flyway rollback commands")
        void assemble_postgresqlFlyway_containsFlywayRollback(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Flyway Rollback");
            assertThat(content)
                    .contains("flyway undo");
        }
    }

    @Nested
    @DisplayName("MongoDB + Mongock generation")
    class MongodbMongock {

        @Test
        @DisplayName("generates template for MongoDB+Mongock")
        void assemble_mongodbMongock_generatesFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildMongodbMongock();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
        }

        @Test
        @DisplayName("contains Mongock commands")
        void assemble_mongodbMongock_containsMongockCommands(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildMongodbMongock();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Mongock Pre-Migration");
            assertThat(content)
                    .contains("mongockChangeLog");
            assertThat(content)
                    .contains("mongodump");
        }

        @Test
        @DisplayName("contains MongoDB validation queries")
        void assemble_mongodbMongock_containsMongoQueries(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildMongodbMongock();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("MongoDB Validation Queries");
            assertThat(content)
                    .contains("countDocuments");
            assertThat(content)
                    .contains("$lookup");
        }

        @Test
        @DisplayName("does NOT contain Flyway commands")
        void assemble_mongodbMongock_doesNotContainFlyway(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildMongodbMongock();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .doesNotContain("Flyway Pre-Migration");
            assertThat(content)
                    .doesNotContain("flyway info");
        }

        @Test
        @DisplayName("does NOT contain SQL validation queries")
        void assemble_mongodbMongock_doesNotContainSql(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildMongodbMongock();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .doesNotContain("SQL Validation");
            assertThat(content)
                    .doesNotContain("pg_constraint");
        }

        @Test
        @DisplayName("contains Mongock rollback with mongorestore")
        void assemble_mongodbMongock_containsRollback(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildMongodbMongock();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Mongock Rollback");
            assertThat(content)
                    .contains("mongorestore");
        }
    }

    @Nested
    @DisplayName("all mandatory sections present")
    class MandatorySections {

        @Test
        @DisplayName("contains all 9 mandatory sections")
        void assemble_postgresql_hasAllMandatorySections(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            for (String section
                    : DataMigrationPlanAssembler
                    .MANDATORY_SECTIONS) {
                assertThat(content)
                        .as("Missing section: %s", section)
                        .contains(section);
            }
        }

        @Test
        @DisplayName("contains project name in header")
        void assemble_postgresql_containsProjectName(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("my-service")
                            .database("postgresql", "16")
                            .migration("flyway", "9")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Data Migration Plan — my-service");
        }

        @Test
        @DisplayName("contains database type in summary")
        void assemble_postgresql_containsDbTypeInSummary(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content).contains("postgresql");
        }

        @Test
        @DisplayName("contains migration tool in summary")
        void assemble_postgresql_containsMigrationToolInSummary(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content).contains("| flyway |");
        }
    }

    @Nested
    @DisplayName("PostgreSQL + Alembic generation")
    class PostgresqlAlembic {

        @Test
        @DisplayName("contains Alembic commands")
        void assemble_postgresqlAlembic_containsAlembic(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.110")
                            .database("postgresql", "16")
                            .migration("alembic", "1.13")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Alembic Pre-Migration");
            assertThat(content)
                    .contains("alembic current");
            assertThat(content)
                    .contains("alembic upgrade head");
        }

        @Test
        @DisplayName("does NOT contain Flyway commands")
        void assemble_postgresqlAlembic_doesNotContainFlyway(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.110")
                            .database("postgresql", "16")
                            .migration("alembic", "1.13")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .doesNotContain("Flyway Pre-Migration");
        }
    }

    @Nested
    @DisplayName("PostgreSQL + Prisma generation")
    class PostgresqlPrisma {

        @Test
        @DisplayName("contains Prisma commands")
        void assemble_postgresqlPrisma_containsPrisma(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("typescript", "5.3")
                            .framework("nestjs", "10")
                            .database("postgresql", "16")
                            .migration("prisma", "5")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Prisma Pre-Migration");
            assertThat(content)
                    .contains("npx prisma migrate status");
            assertThat(content)
                    .contains("npx prisma validate");
        }
    }

    @Nested
    @DisplayName("graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty when template absent")
        void assemble_templateAbsent_returnsEmpty(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler(
                            resourcesDir);
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create output dir when absent")
        void assemble_templateAbsent_doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler(
                            resourcesDir);
            ProjectConfig config = buildPostgresqlFlyway();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("multi-profile generation")
    class MultiProfile {

        @Test
        @DisplayName("generates for java-spring profile")
        void assemble_javaSpring_generatesTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring-boot", "3.2")
                            .database("postgresql", "16")
                            .migration("flyway", "9")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0)).endsWith(
                    "_TEMPLATE-DATA-MIGRATION-PLAN.md");
        }

        @Test
        @DisplayName("generates for kotlin-ktor profile")
        void assemble_kotlinKtor_generatesTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("kotlin", "1.9")
                            .framework("ktor", "2.3")
                            .database("postgresql", "16")
                            .migration("flyway", "9")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
        }

        @Test
        @DisplayName("generates for go-gin profile")
        void assemble_goGin_generatesTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("go", "1.22")
                            .framework("gin", "1.9")
                            .database("postgresql", "16")
                            .migration("golang-migrate", "4")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content).contains(
                    "golang-migrate Pre-Migration");
        }

        @Test
        @DisplayName("generates for rust-axum profile")
        void assemble_rustAxum_generatesTemplate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("rust", "1.75")
                            .framework("axum", "0.7")
                            .database("postgresql", "16")
                            .migration("diesel", "2")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Diesel Pre-Migration");
            assertThat(content)
                    .contains("diesel migration pending");
        }
    }

    @Nested
    @DisplayName("cross-profile tool exclusivity")
    class CrossProfileExclusivity {

        @Test
        @DisplayName("java-spring has Flyway but not Alembic")
        void assemble_javaSpring_flywayNotAlembic(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("java", "21")
                            .framework("spring-boot", "3.2")
                            .database("postgresql", "16")
                            .migration("flyway", "9")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Flyway Pre-Migration");
            assertThat(content)
                    .doesNotContain("Alembic Pre-Migration");
            assertThat(content)
                    .doesNotContain("Mongock Pre-Migration");
            assertThat(content)
                    .doesNotContain("Prisma Pre-Migration");
        }

        @Test
        @DisplayName("python-fastapi has Alembic but not Flyway")
        void assemble_pythonFastapi_alembicNotFlyway(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            DataMigrationPlanAssembler assembler =
                    new DataMigrationPlanAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .language("python", "3.12")
                            .framework("fastapi", "0.110")
                            .database("postgresql", "16")
                            .migration("alembic", "1.13")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readFile(
                    outputDir.resolve(OUTPUT_PATH));
            assertThat(content)
                    .contains("Alembic Pre-Migration");
            assertThat(content)
                    .doesNotContain("Flyway Pre-Migration");
            assertThat(content)
                    .doesNotContain("Mongock Pre-Migration");
            assertThat(content)
                    .doesNotContain("Prisma Pre-Migration");
        }
    }

    // --- Test helpers ---

    private static ProjectConfig buildPostgresqlFlyway() {
        return TestConfigBuilder.builder()
                .language("java", "21")
                .framework("spring-boot", "3.4")
                .database("postgresql", "16")
                .migration("flyway", "9")
                .build();
    }

    private static ProjectConfig buildMongodbMongock() {
        return TestConfigBuilder.builder()
                .language("java", "21")
                .framework("spring-boot", "3.4")
                .database("mongodb", "7")
                .migration("mongock", "5")
                .build();
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(
                    path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read: " + path, e);
        }
    }
}
