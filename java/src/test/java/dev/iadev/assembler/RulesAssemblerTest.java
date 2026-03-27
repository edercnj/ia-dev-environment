package dev.iadev.assembler;

import dev.iadev.config.ContextBuilder;
import dev.iadev.model.ProjectConfig;
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
 * Tests for RulesAssembler — the first assembler in the
 * pipeline, generating .claude/rules/ and skills/.
 */
@DisplayName("RulesAssembler")
class RulesAssemblerTest {

    @Nested
    @DisplayName("assemble — core rules generation")
    class CoreRules {

        @Test
        @DisplayName("generates 8 core rule files for"
                + " minimal config (no database)")
        void assemble_whenCalled_generatesEightCoreRules(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path rulesDir = outputDir.resolve("rules");
            assertThat(rulesDir.resolve(
                    "01-project-identity.md"))
                    .exists();
            assertThat(rulesDir.resolve(
                    "02-domain.md")).exists();
            assertThat(rulesDir.resolve(
                    "03-coding-standards.md"))
                    .exists();
            assertThat(rulesDir.resolve(
                    "04-architecture-summary.md"))
                    .exists();
            assertThat(rulesDir.resolve(
                    "05-quality-gates.md")).exists();
            assertThat(rulesDir.resolve(
                    "06-security-baseline.md"))
                    .exists();
            assertThat(rulesDir.resolve(
                    "07-operations-baseline.md"))
                    .exists();
            assertThat(rulesDir.resolve(
                    "08-release-process.md"))
                    .exists();
            assertThat(rulesDir.resolve(
                    "09-data-management.md"))
                    .doesNotExist();
        }

        @Test
        @DisplayName("generates 9 rule files when database"
                + " is configured")
        void assemble_withDb_generatesNineRules(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "17")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path rulesDir = outputDir.resolve("rules");
            assertThat(rulesDir.resolve(
                    "07-operations-baseline.md"))
                    .exists();
            assertThat(rulesDir.resolve(
                    "08-release-process.md"))
                    .exists();
            assertThat(rulesDir.resolve(
                    "09-data-management.md"))
                    .exists();
        }

        @Test
        @DisplayName("01-project-identity.md contains"
                + " project data")
        void assemble_identity_containsProjectData(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("my-api")
                    .language("java", "21")
                    .framework("spring-boot", "3.4")
                    .archStyle("microservice")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "rules/01-project-identity.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("my-api")
                    .contains("java 21")
                    .contains("spring-boot 3.4")
                    .contains("microservice");
        }

        @Test
        @DisplayName("02-domain.md generated from template")
        void assemble_whenCalled_domainFromTemplate(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "rules/02-domain.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("{DOMAIN_NAME}");
        }

        @Test
        @DisplayName("02-domain.md uses fallback when"
                + " template missing")
        void assemble_whenTemplateMissing_domainFallback(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreRules = resourceDir.resolve(
                    "core-rules");
            Files.createDirectories(coreRules);

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("fallback-project")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "rules/02-domain.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("fallback-project")
                    .contains("{DOMAIN_NAME}");
        }

        @Test
        @DisplayName("core rules have placeholders replaced")
        void assemble_whenCalled_coreRulesPlaceholdersReplaced(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = tempDir.resolve("res");
            Path coreRules = resourceDir.resolve(
                    "core-rules");
            Files.createDirectories(coreRules);
            Files.writeString(
                    coreRules.resolve("03-test.md"),
                    "Project: {PROJECT_NAME}\n",
                    StandardCharsets.UTF_8);

            Path templates = resourceDir.resolve(
                    "templates");
            Files.createDirectories(templates);
            Files.writeString(
                    templates.resolve("domain-template.md"),
                    "Domain\n");

            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("replaced-name")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "rules/03-test.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("replaced-name")
                    .doesNotContain("{{PROJECT_NAME}}");
        }
    }

    @Nested
    @DisplayName("assemble — no database generates"
            + " only core rules")
    class NoDatabaseConfig {

        @Test
        @DisplayName("config without database generates"
                + " exactly core rules without data rule")
        void assemble_noDatabase_generatesOnlyCoreRules(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            Path rulesDir = outputDir.resolve("rules");
            try (var stream = Files.list(rulesDir)) {
                List<String> ruleFiles = stream
                        .map(p -> p.getFileName()
                                .toString())
                        .sorted()
                        .toList();

                assertThat(ruleFiles).doesNotContain(
                        "06-database-conventions.md");
                assertThat(ruleFiles).doesNotContain(
                        "09-data-management.md");
            }
        }
    }

    @Nested
    @DisplayName("assemble — rule 07 operations baseline")
    class Rule07Operations {

        @Test
        @DisplayName("rule 07 contains health check sections")
        void assemble_rule07_containsHealthChecks(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "rules/07-operations-baseline.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("Health Checks")
                    .contains("liveness")
                    .contains("readiness")
                    .contains("startup");
        }

        @Test
        @DisplayName("rule 07 contains structured logging")
        void assemble_rule07_containsStructuredLogging(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "rules/07-operations-baseline.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("Structured Logging")
                    .contains("Graceful Shutdown")
                    .contains("Correlation ID");
        }
    }

    @Nested
    @DisplayName("assemble — rule 08 release process")
    class Rule08Release {

        @Test
        @DisplayName("rule 08 contains semantic versioning")
        void assemble_rule08_containsSemVer(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "rules/08-release-process.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("Semantic Versioning")
                    .contains("Conventional Commits")
                    .contains("CHANGELOG");
        }
    }

    @Nested
    @DisplayName("assemble — rule 09 data management")
    class Rule09DataManagement {

        @Test
        @DisplayName("rule 09 generated with database"
                + " reference when db is configured")
        void assemble_rule09_containsDbReference(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .database("postgresql", "17")
                    .migration("flyway", "10.0")
                    .build();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            String content = Files.readString(
                    outputDir.resolve(
                            "rules/09-data-management.md"),
                    StandardCharsets.UTF_8);

            assertThat(content)
                    .contains("Data Management")
                    .contains("postgresql");
        }

        @Test
        @DisplayName("rule 09 not generated when db is none")
        void assemble_rule09_notGeneratedWithoutDb(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(outputDir.resolve(
                    "rules/09-data-management.md"))
                    .doesNotExist();
        }
    }

    @Nested
    @DisplayName("assemble — returns generated file paths")
    class ReturnsPaths {

        @Test
        @DisplayName("returned list is not empty")
        void assemble_whenCalled_returnedListNotEmpty(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).isNotEmpty();
        }

        @Test
        @DisplayName("returned paths include identity rule")
        void assemble_whenCalled_includesIdentityRule(
                @TempDir Path tempDir)
                throws IOException {
            Path resourceDir = createMinimalResources(
                    tempDir);
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            RulesAssembler assembler =
                    new RulesAssembler(resourceDir);
            ProjectConfig config = TestConfigBuilder
                    .minimal();

            List<String> files = assembler.assemble(
                    config, new TemplateEngine(), outputDir);

            assertThat(files).anyMatch(f ->
                    f.contains("01-project-identity.md"));
        }
    }

    @Nested
    @DisplayName("assemble — implements Assembler interface")
    class ImplementsAssembler {

        @Test
        @DisplayName("is instance of Assembler")
        void assemble_whenCalled_isAssemblerInstance() {
            RulesAssembler assembler = new RulesAssembler();

            assertThat(assembler)
                    .isInstanceOf(Assembler.class);
        }
    }

    @Nested
    @DisplayName("context via ContextBuilder")
    class BuildContext {

        @Test
        @DisplayName("contains all expected keys")
        void context_whenCalled_containsAllKeys() {
            ProjectConfig config = TestConfigBuilder
                    .builder()
                    .projectName("ctx-test")
                    .language("java", "21")
                    .framework("quarkus", "3.17")
                    .archStyle("microservice")
                    .buildTool("maven")
                    .build();

            Map<String, Object> context =
                    ContextBuilder.buildContext(config);

            assertThat(context)
                    .containsEntry("project_name",
                            "ctx-test")
                    .containsEntry("language_name",
                            "java")
                    .containsEntry("language_version",
                            "21")
                    .containsEntry("framework_name",
                            "quarkus")
                    .containsEntry("framework_version",
                            "3.17")
                    .containsEntry("build_tool",
                            "maven")
                    .containsEntry(
                            "architecture_style",
                            "microservice");
        }
    }

    @Nested
    @DisplayName("golden file — byte-for-byte parity")
    class GoldenFile {

        @Test
        @DisplayName("01-project-identity matches golden"
                + " file for java-quarkus profile")
        void golden_identity_matchesGoldenFile()
                throws IOException {
            ProjectConfig config = buildQuarkusConfig();

            String content =
                    RulesIdentity.buildContent(config);

            String expected = loadGoldenFile(
                    "01-project-identity.md");

            assertThat(content).isEqualTo(expected);
        }

        @Test
        @DisplayName("02-domain matches golden file")
        void golden_domain_matchesGoldenFile()
                throws IOException {
            String expected = loadGoldenFile(
                    "02-domain.md");

            assertThat(expected)
                    .contains("{DOMAIN_NAME}")
                    .contains("{DOMAIN_OVERVIEW}");
        }

        @Test
        @DisplayName("03-coding-standards matches golden"
                + " file (unreplaced placeholders)")
        void golden_codingStandards_matchesGoldenFile()
                throws IOException {
            String expected = loadGoldenFile(
                    "03-coding-standards.md");

            assertThat(expected)
                    .contains("{{LANGUAGE}}")
                    .contains("{{LANGUAGE_VERSION}}");
        }

        @Test
        @DisplayName("04-architecture matches golden file"
                + " (unreplaced placeholders)")
        void golden_architecture_matchesGoldenFile()
                throws IOException {
            String expected = loadGoldenFile(
                    "04-architecture-summary.md");

            assertThat(expected)
                    .contains("{{ARCHITECTURE}}")
                    .contains("{{ARCH_STYLE}}");
        }

        @Test
        @DisplayName("05-quality-gates matches golden file")
        void golden_qualityGates_matchesGoldenFile()
                throws IOException {
            String expected = loadGoldenFile(
                    "05-quality-gates.md");

            assertThat(expected)
                    .contains("95%")
                    .contains("90%");
        }

        @Test
        @DisplayName("06-security-baseline matches golden"
                + " file")
        void golden_securityBaseline_matchesGoldenFile()
                throws IOException {
            String expected = loadGoldenFile(
                    "06-security-baseline.md");

            assertThat(expected)
                    .contains("Security Baseline")
                    .contains("Secure Defaults");
        }

        @Test
        @DisplayName("07-operations-baseline matches golden"
                + " file")
        void golden_operationsBaseline_matchesGoldenFile()
                throws IOException {
            String expected = loadGoldenFile(
                    "07-operations-baseline.md");

            assertThat(expected)
                    .contains("Operations Baseline")
                    .contains("Health Checks");
        }

        @Test
        @DisplayName("08-release-process matches golden"
                + " file")
        void golden_releaseProcess_matchesGoldenFile()
                throws IOException {
            String expected = loadGoldenFile(
                    "08-release-process.md");

            assertThat(expected)
                    .contains("Release Process")
                    .contains("Semantic Versioning");
        }

        @Test
        @DisplayName("09-data-management matches golden"
                + " file (java-quarkus has database)")
        void golden_dataManagement_matchesGoldenFile()
                throws IOException {
            String expected = loadGoldenFile(
                    "09-data-management.md");

            assertThat(expected)
                    .contains("Data Management")
                    .contains("Migration");
        }

        private String loadGoldenFile(String filename)
                throws IOException {
            var url = getClass().getClassLoader()
                    .getResource(
                            "golden/java-quarkus/"
                                    + ".claude/rules/"
                                    + filename);
            assertThat(url)
                    .as("Golden file %s must exist",
                            filename)
                    .satisfies(u -> assertThat(
                            u.toString())
                            .contains(filename));
            return Files.readString(
                    Path.of(url.getPath()),
                    StandardCharsets.UTF_8);
        }
    }

    private static ProjectConfig buildQuarkusConfig() {
        return TestConfigBuilder.builder()
                .projectName("my-quarkus-service")
                .purpose(
                        "Describe your service purpose here")
                .archStyle("microservice")
                .domainDriven(true)
                .eventDriven(true)
                .language("java", "21")
                .framework("quarkus", "3.17")
                .buildTool("maven")
                .nativeBuild(true)
                .contractTests(true)
                .orchestrator("kubernetes")
                .clearInterfaces()
                .addInterface("rest")
                .addInterface("grpc")
                .addInterface("event-consumer")
                .addInterface("event-producer")
                .build();
    }

    private static Path createMinimalResources(
            Path tempDir) throws IOException {
        Path resourceDir = tempDir.resolve("res");
        createCoreRules(resourceDir);
        createTemplatesDir(resourceDir);
        return resourceDir;
    }

    private static void createCoreRules(
            Path resourceDir) throws IOException {
        Path coreRules =
                resourceDir.resolve("core-rules");
        Files.createDirectories(coreRules);
        Files.writeString(
                coreRules.resolve(
                        "03-coding-standards.md"),
                "# Coding Standards\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                coreRules.resolve(
                        "04-architecture-summary.md"),
                "# Architecture\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                coreRules.resolve("05-quality-gates.md"),
                "# Quality Gates\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                coreRules.resolve(
                        "06-security-baseline.md"),
                "# Security Baseline\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                coreRules.resolve(
                        "07-operations-baseline.md"),
                "# Rule 07 — Operations Baseline\n"
                        + "## Health Checks\n"
                        + "liveness, readiness, startup\n"
                        + "## Structured Logging\n"
                        + "## Graceful Shutdown\n"
                        + "## Correlation ID\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                coreRules.resolve(
                        "08-release-process.md"),
                "# Rule 08 — Release Process\n"
                        + "## Semantic Versioning\n"
                        + "## Conventional Commits\n"
                        + "## CHANGELOG\n",
                StandardCharsets.UTF_8);
        Path condRules = coreRules.resolve("conditional");
        Files.createDirectories(condRules);
        Files.writeString(
                condRules.resolve(
                        "09-data-management.md"),
                "# Rule 09 — Data Management\n"
                        + "DB: {DATABASE_NAME}\n"
                        + "## Migration\n",
                StandardCharsets.UTF_8);
    }

    private static void createTemplatesDir(
            Path resourceDir) throws IOException {
        Path templates =
                resourceDir.resolve("templates");
        Files.createDirectories(templates);
        Files.writeString(
                templates.resolve("domain-template.md"),
                "# Rule — {DOMAIN_NAME} Domain\n",
                StandardCharsets.UTF_8);
    }
}
