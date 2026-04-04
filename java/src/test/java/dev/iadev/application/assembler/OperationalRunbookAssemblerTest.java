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
 * Tests for OperationalRunbookAssembler -- generates
 * results/runbooks/_TEMPLATE-OPERATIONAL-RUNBOOK.md from
 * a Pebble template with conditional sections based on
 * database, cache, and message broker configuration.
 */
@DisplayName("OperationalRunbookAssembler")
class OperationalRunbookAssemblerTest {

    private static final String OUTPUT_FILE =
            "results/runbooks/"
                    + "_TEMPLATE-OPERATIONAL-RUNBOOK.md";

    @Nested
    @DisplayName("implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssembler() {
            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble -- degenerate: no db/cache/broker")
    class DegenerateNoOptionalSections {

        @Test
        @DisplayName("generates file in results/runbooks/")
        void assemble_noDeps_generatesFile(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            Path expected =
                    outputDir.resolve(OUTPUT_FILE);
            assertThat(expected).exists();
        }

        @Test
        @DisplayName("contains Scaling Procedures section")
        void assemble_noDeps_containsScalingProcedures(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains("## Scaling Procedures");
        }

        @Test
        @DisplayName("contains Certificate Rotation section")
        void assemble_noDeps_containsCertificateRotation(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains("## Certificate Rotation");
        }

        @Test
        @DisplayName("contains Dependency Failure Handling"
                + " section")
        void assemble_noDeps_containsDependencyFailure(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains(
                            "## Dependency Failure Handling");
        }

        @Test
        @DisplayName("contains Backup & Restore Procedures"
                + " section")
        void assemble_noDeps_containsBackupRestore(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains(
                            "## Backup & Restore Procedures");
        }

        @Test
        @DisplayName("does NOT contain Database Maintenance")
        void assemble_noDeps_omitsDatabaseMaintenance(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .doesNotContain(
                            "## Database Maintenance");
        }

        @Test
        @DisplayName("does NOT contain Cache Operations")
        void assemble_noDeps_omitsCacheOperations(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .doesNotContain("## Cache Operations");
        }

        @Test
        @DisplayName("does NOT contain Message Broker"
                + " Operations")
        void assemble_noDeps_omitsBrokerOperations(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .doesNotContain(
                            "## Message Broker Operations");
        }
    }

    @Nested
    @DisplayName("assemble -- with PostgreSQL database")
    class WithPostgresql {

        @Test
        @DisplayName("includes Database Maintenance section")
        void assemble_withPostgresql_containsDbMaintenance(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains("## Database Maintenance");
        }

        @Test
        @DisplayName("includes vacuum procedures")
        void assemble_withPostgresql_containsVacuum(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("vacuum");
        }

        @Test
        @DisplayName("includes reindex operations")
        void assemble_withPostgresql_containsReindex(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("reindex");
        }

        @Test
        @DisplayName("includes failover procedures")
        void assemble_withPostgresql_containsFailover(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("failover");
        }

        @Test
        @DisplayName("unconditional sections still present")
        void assemble_withPostgresql_unconditionalPresent(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .database("postgresql", "16")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains("## Scaling Procedures");
            assertThat(content)
                    .contains("## Certificate Rotation");
            assertThat(content)
                    .contains(
                            "## Dependency Failure Handling");
            assertThat(content)
                    .contains(
                            "## Backup & Restore Procedures");
        }
    }

    @Nested
    @DisplayName("assemble -- with Redis cache")
    class WithRedisCache {

        @Test
        @DisplayName("includes Cache Operations section")
        void assemble_withRedis_containsCacheOps(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .cache("redis", "7")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains("## Cache Operations");
        }

        @Test
        @DisplayName("includes flush procedures")
        void assemble_withRedis_containsFlush(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .cache("redis", "7")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("flush");
        }

        @Test
        @DisplayName("includes warmup procedures")
        void assemble_withRedis_containsWarmup(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .cache("redis", "7")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("warmup");
        }

        @Test
        @DisplayName("includes TTL adjustment")
        void assemble_withRedis_containsTtlAdjustment(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .cache("redis", "7")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("ttl");
        }

        @Test
        @DisplayName("includes hit rate monitoring")
        void assemble_withRedis_containsHitRate(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .cache("redis", "7")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("hit rate");
        }
    }

    @Nested
    @DisplayName("assemble -- with Kafka message broker")
    class WithKafkaBroker {

        @Test
        @DisplayName("includes Message Broker Operations"
                + " section")
        void assemble_withKafka_containsBrokerOps(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface(
                                    "event", "", "kafka")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains(
                            "## Message Broker Operations");
        }

        @Test
        @DisplayName("includes queue management")
        void assemble_withKafka_containsQueueManagement(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface(
                                    "event", "", "kafka")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("topic");
        }

        @Test
        @DisplayName("includes dead letter processing")
        void assemble_withKafka_containsDeadLetter(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface(
                                    "event", "", "kafka")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("dead letter");
        }

        @Test
        @DisplayName("includes consumer lag monitoring")
        void assemble_withKafka_containsConsumerLag(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .clearInterfaces()
                            .addInterface(
                                    "event", "", "kafka")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .containsIgnoringCase("consumer lag");
        }
    }

    @Nested
    @DisplayName("assemble -- full stack (all sections)")
    class FullStack {

        @Test
        @DisplayName("all 7 sections present")
        void assemble_fullStack_containsAllSections(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config = buildFullStackConfig();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content)
                    .contains("## Scaling Procedures");
            assertThat(content)
                    .contains("## Database Maintenance");
            assertThat(content)
                    .contains("## Cache Operations");
            assertThat(content)
                    .contains(
                            "## Message Broker Operations");
            assertThat(content)
                    .contains("## Certificate Rotation");
            assertThat(content)
                    .contains(
                            "## Dependency Failure Handling");
            assertThat(content)
                    .contains(
                            "## Backup & Restore Procedures");
        }
    }

    @Nested
    @DisplayName("assemble -- placeholder resolution")
    class PlaceholderResolution {

        @Test
        @DisplayName("resolves project_name in title")
        void assemble_projectName_resolvedInTitle(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .projectName("payment-service")
                            .build();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            String content = readOutput(outputDir);
            assertThat(content).contains(
                    "# Operational Runbook"
                            + " \u2014 payment-service");
        }
    }

    @Nested
    @DisplayName("assemble -- graceful no-op")
    class GracefulNoOp {

        @Test
        @DisplayName("returns empty list when template"
                + " file absent")
        void assemble_whenAbsent_returnsEmptyList(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("does not create output directory"
                + " when template absent")
        void assemble_whenAbsent_doesNotCreateOutputDir(
                @TempDir Path tempDir) {
            Path resourcesDir =
                    tempDir.resolve("nonexistent");
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler(
                            resourcesDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            assembler.assemble(config, engine, outputDir);

            assertThat(outputDir).doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble -- returns file path")
    class ReturnsFilePath {

        @Test
        @DisplayName("returns correct file path in list")
        void assemble_whenCalled_returnsFilePath(
                @TempDir Path tempDir) {
            Path outputDir = tempDir.resolve("output");

            OperationalRunbookAssembler assembler =
                    new OperationalRunbookAssembler();
            ProjectConfig config =
                    TestConfigBuilder.minimal();
            TemplateEngine engine = new TemplateEngine();

            List<String> files = assembler.assemble(
                    config, engine, outputDir);

            assertThat(files).hasSize(1);
            assertThat(files.get(0)).endsWith(
                    "_TEMPLATE-OPERATIONAL-RUNBOOK.md");
        }
    }

    private static ProjectConfig buildFullStackConfig() {
        return TestConfigBuilder.builder()
                .database("postgresql", "16")
                .cache("redis", "7")
                .clearInterfaces()
                .addInterface("rest", "", "")
                .addInterface("event", "", "kafka")
                .build();
    }

    private static String readOutput(Path outputDir) {
        Path file = outputDir.resolve(OUTPUT_FILE);
        try {
            return Files.readString(
                    file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read: " + file, e);
        }
    }
}
