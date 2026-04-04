package dev.iadev.assembler;

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
 * Tests for PatternsAssembler — the fourth assembler in
 * the pipeline, generating .claude/skills/patterns/
 * with consolidated SKILL.md and per-category reference
 * files.
 */
@DisplayName("PatternsAssembler")
class PatternsAssemblerTest {

    @Nested
    @DisplayName("assemble — implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void instanceOf_whenCreated_implementsAssemblerInterface() {
            PatternsAssembler assembler =
                    new PatternsAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("assemble — microservice architecture"
            + " includes all categories")
    class MicroserviceArchitecture {

        @Test
        @DisplayName("microservice generates architectural"
                + " pattern references")
        void assemble_microservice_includesArchitectural(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildMicroserviceConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "skills/patterns/references");
            assertThat(refsDir.resolve(
                    "architectural/cqrs.md"))
                    .exists();
            assertThat(refsDir.resolve(
                    "architectural/hexagonal-architecture.md"))
                    .exists();
        }

        @Test
        @DisplayName("microservice generates microservice"
                + " pattern references")
        void assemble_microservice_includesMicroservice(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildMicroserviceConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "skills/patterns/references");
            assertThat(refsDir.resolve(
                    "microservice/saga-pattern.md"))
                    .exists();
            assertThat(refsDir.resolve(
                    "microservice/api-gateway.md"))
                    .exists();
        }

        @Test
        @DisplayName("microservice generates resilience"
                + " pattern references")
        void assemble_microservice_includesResilience(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildMicroserviceConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "skills/patterns/references");
            assertThat(refsDir.resolve(
                    "resilience/circuit-breaker.md"))
                    .exists();
            assertThat(refsDir.resolve(
                    "resilience/retry-with-backoff.md"))
                    .exists();
        }

        @Test
        @DisplayName("microservice generates integration"
                + " pattern references")
        void assemble_microservice_includesIntegration(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildMicroserviceConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "skills/patterns/references");
            assertThat(refsDir.resolve(
                    "integration/adapter-pattern.md"))
                    .exists();
        }

        @Test
        @DisplayName("microservice generates data"
                + " pattern references")
        void assemble_microservice_includesData(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildMicroserviceConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "skills/patterns/references");
            assertThat(refsDir.resolve(
                    "data/repository-pattern.md"))
                    .exists();
        }

        @Test
        @DisplayName("microservice generates consolidated"
                + " SKILL.md")
        void assemble_microservice_generatesConsolidated(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildMicroserviceConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillMd = outputDir.resolve(
                    "skills/patterns/SKILL.md");
            assertThat(skillMd).exists();
            String content = Files.readString(
                    skillMd, StandardCharsets.UTF_8);
            assertThat(content).contains("---");
        }

        @Test
        @DisplayName("returned list includes all generated"
                + " files")
        void assemble_returnedList_includesAll(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildMicroserviceConfig();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            // 22 pattern files + 1 consolidated SKILL.md
            assertThat(files).hasSizeGreaterThan(10);
            assertThat(files).anyMatch(
                    f -> f.endsWith("SKILL.md"));
        }
    }

    @Nested
    @DisplayName("assemble — library architecture"
            + " excludes microservice patterns")
    class LibraryArchitecture {

        @Test
        @DisplayName("library includes only architectural"
                + " and data patterns")
        void assemble_library_includesOnlyArchAndData(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("library")
                            .eventDriven(false)
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path refsDir = outputDir.resolve(
                    "skills/patterns/references");
            // architectural and data are present
            assertThat(refsDir.resolve(
                    "architectural")).isDirectory();
            assertThat(refsDir.resolve(
                    "data")).isDirectory();
            // microservice, resilience, integration absent
            assertThat(refsDir.resolve(
                    "microservice")).doesNotExist();
            assertThat(refsDir.resolve(
                    "resilience")).doesNotExist();
            assertThat(refsDir.resolve(
                    "integration")).doesNotExist();
        }

        @Test
        @DisplayName("library SKILL.md does not contain"
                + " microservice-only pattern content")
        void assemble_librarySkillMd_excludesMicroservice(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("library")
                            .eventDriven(false)
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path skillMd = outputDir.resolve(
                    "skills/patterns/SKILL.md");
            assertThat(skillMd).exists();
            String content = Files.readString(
                    skillMd, StandardCharsets.UTF_8);
            // Saga Pattern is a microservice-specific file
            assertThat(content)
                    .doesNotContain("# Saga Pattern");
            // Bulkhead is a microservice-specific file
            assertThat(content)
                    .doesNotContain("# Bulkhead");
        }
    }

    @Nested
    @DisplayName("assemble — unknown architecture style")
    class UnknownArchitecture {

        @Test
        @DisplayName("unknown style returns empty list")
        void assemble_unknownStyle_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("exotic-style")
                            .eventDriven(false)
                            .container("none")
                            .orchestrator("none")
                            .iac("none")
                            .clearInterfaces()
                            .addInterface("cli")
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    @Nested
    @DisplayName("assemble — golden file parity")
    class GoldenFile {

        @Test
        @DisplayName("consolidated SKILL.md matches"
                + " golden file for go-gin profile")
        void assemble_skillMd_matchesGolden(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildGoGinConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String goldenPath =
                    "golden/go-gin/.claude/skills/patterns/"
                            + "SKILL.md";
            String expected = loadResource(goldenPath);
            if (expected != null) {
                String actual = Files.readString(
                        outputDir.resolve(
                                "skills/patterns/SKILL.md"),
                        StandardCharsets.UTF_8);
                assertThat(actual)
                        .as("patterns/SKILL.md")
                        .isEqualTo(expected);
            }
        }

        @Test
        @DisplayName("all pattern reference files match"
                + " golden files for go-gin profile")
        void assemble_allRefsMatchGolden_succeeds(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler();
            ProjectConfig config = buildGoGinConfig();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String[][] refFiles = {
                    {"architectural", "cqrs.md"},
                    {"architectural", "event-sourcing.md"},
                    {"architectural",
                            "hexagonal-architecture.md"},
                    {"architectural", "modular-monolith.md"},
                    {"data", "cache-aside.md"},
                    {"data", "event-store.md"},
                    {"data", "repository-pattern.md"},
                    {"data", "unit-of-work.md"},
                    {"integration", "adapter-pattern.md"},
                    {"integration",
                            "anti-corruption-layer.md"},
                    {"integration",
                            "backend-for-frontend.md"},
                    {"microservice", "api-gateway.md"},
                    {"microservice", "bulkhead.md"},
                    {"microservice", "idempotency.md"},
                    {"microservice", "outbox-pattern.md"},
                    {"microservice", "saga-pattern.md"},
                    {"microservice", "service-discovery.md"},
                    {"microservice", "strangler-fig.md"},
                    {"resilience", "circuit-breaker.md"},
                    {"resilience", "dead-letter-queue.md"},
                    {"resilience", "retry-with-backoff.md"},
                    {"resilience", "timeout-patterns.md"},
            };

            for (String[] pair : refFiles) {
                String relPath = pair[0] + "/" + pair[1];
                String goldenPath =
                        "golden/go-gin/.claude/skills/"
                                + "patterns/references/"
                                + relPath;
                String expected =
                        loadResource(goldenPath);
                if (expected != null) {
                    String actual = Files.readString(
                            outputDir.resolve(
                                    "skills/patterns/"
                                            + "references/"
                                            + relPath),
                            StandardCharsets.UTF_8);
                    assertThat(actual)
                            .as("Pattern ref: " + relPath)
                            .isEqualTo(expected);
                }
            }
        }

        private String loadResource(String path) {
            var url = getClass().getClassLoader()
                    .getResource(path);
            if (url == null) {
                return null;
            }
            try {
                return Files.readString(
                        Path.of(url.getPath()),
                        StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }
    }

    @Nested
    @DisplayName("assemble — edge cases")
    class EdgeCases {

        @Test
        @DisplayName("custom resourcesDir with no"
                + " patterns dir returns empty")
        void assemble_emptyResources_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Files.createDirectories(resourceDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }

        @Test
        @DisplayName("patterns dir exists but category"
                + " dir missing returns empty")
        void assemble_missingCategoryDir_returnsEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path patternsDir =
                    resourceDir.resolve("patterns");
            Files.createDirectories(patternsDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            PatternsAssembler assembler =
                    new PatternsAssembler(resourceDir);
            ProjectConfig config =
                    TestConfigBuilder.builder()
                            .archStyle("library")
                            .eventDriven(false)
                            .build();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isEmpty();
        }
    }

    private static ProjectConfig buildMicroserviceConfig() {
        return TestConfigBuilder.builder()
                .archStyle("microservice")
                .eventDriven(true)
                .build();
    }

    private static ProjectConfig buildGoGinConfig() {
        var builder = TestConfigBuilder.builder()
                .projectName("my-go-service")
                .purpose(
                        "Describe your service purpose here")
                .archStyle("microservice")
                .domainDriven(false)
                .eventDriven(true)
                .language("go", "1.22")
                .framework("gin", "")
                .buildTool("go-mod")
                .nativeBuild(false);
        return configureGoInfra(builder).build();
    }

    private static TestConfigBuilder configureGoInfra(
            TestConfigBuilder builder) {
        return builder
                .container("docker")
                .orchestrator("kubernetes")
                .iac("terraform")
                .apiGateway("kong")
                .smokeTests(true)
                .contractTests(false)
                .performanceTests(true)
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer",
                        "", "kafka")
                .addInterface("event-producer",
                        "", "kafka");
    }
}
