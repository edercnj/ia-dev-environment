package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for {@link GenerateCommand}.
 *
 * <p>Tests the full pipeline from config loading through to
 * file generation and CLI display output. Uses real config
 * profiles and verifies actual file output.
 */
@DisplayName("GenerateCommand E2E")
class GenerateCommandE2ETest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Full generation with stack profile")
    class FullGeneration {

        @Test
        void javaQuarkus_withStackProfile_generatesClaudeDir() {
            Path outputDir =
                    tempDir.resolve("full-gen");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(),
                    "-f");

            assertThat(exitCode).isZero();
            assertThat(outputDir.resolve(".claude"))
                    .isDirectory();
        }

        @Test
        void javaQuarkus_whenCalled_generatesRulesDir() {
            Path outputDir =
                    tempDir.resolve("rules-gen");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(),
                    "-f");

            assertThat(
                    outputDir.resolve(".claude/rules"))
                    .isDirectory();
        }

        @Test
        void javaQuarkus_whenCalled_generatesGithubDir() {
            Path outputDir =
                    tempDir.resolve("github-gen");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(),
                    "-f");

            assertThat(outputDir.resolve(".github"))
                    .isDirectory();
        }

        @Test
        void javaQuarkus_output_containsSuccess() {
            Path outputDir =
                    tempDir.resolve("success-gen");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(),
                    "-f");

            assertThat(sw.toString())
                    .contains("Pipeline: Success");
        }

        @Test
        void javaQuarkus_output_containsSummaryTable() {
            Path outputDir =
                    tempDir.resolve("table-gen");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(),
                    "-f");

            assertThat(sw.toString())
                    .contains("Category")
                    .contains("Count")
                    .contains("Total");
        }

        @Test
        void typescriptNestjs_whenCalled_generatesClaudeDir() {
            Path outputDir =
                    tempDir.resolve("ts-gen");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "typescript-nestjs",
                    "-o", outputDir.toString(),
                    "-f");

            assertThat(exitCode).isZero();
            assertThat(outputDir.resolve(".claude"))
                    .isDirectory();
        }
    }

    @Nested
    @DisplayName("Config file E2E")
    class ConfigFileE2E {

        @Test
        void validYamlConfig_whenCalled_generatesOutput()
                throws IOException {
            String config = """
                    project:
                      name: "my-api"
                      purpose: "REST API service"
                    architecture:
                      style: microservice
                    interfaces:
                      - type: rest
                    language:
                      name: java
                      version: "21"
                    framework:
                      name: quarkus
                      version: "3.17"
                      build_tool: maven
                    """;
            Path configFile =
                    tempDir.resolve("e2e-config.yaml");
            Files.writeString(configFile, config,
                    StandardCharsets.UTF_8);
            Path outputDir =
                    tempDir.resolve("e2e-output");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "-o", outputDir.toString());

            assertThat(exitCode).isZero();
            assertThat(outputDir.resolve(".claude"))
                    .isDirectory();
        }

        @Test
        void invalidYamlConfig_whenCalled_returnsNonZero()
                throws IOException {
            String config = """
                    project:
                      name: "test"
                      purpose: "Testing"
                    """;
            Path configFile =
                    tempDir.resolve("invalid-e2e.yaml");
            Files.writeString(configFile, config,
                    StandardCharsets.UTF_8);

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "-o",
                    tempDir.resolve("inv-out").toString());

            assertThat(exitCode).isNotZero();
        }
    }

    @Nested
    @DisplayName("Exit codes")
    class ExitCodes {

        @Test
        void success_whenCalled_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode)
                    .isEqualTo(GenerateCommand.EXIT_SUCCESS);
        }

        @Test
        void validationError_whenCalled_returnsOne() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute("generate");

            assertThat(exitCode)
                    .isEqualTo(
                            GenerateCommand.EXIT_VALIDATION);
        }

        @Test
        void dangerousPath_whenCalled_returnsOne() {
            String home =
                    System.getProperty("user.home");
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", home);

            assertThat(exitCode)
                    .isEqualTo(
                            GenerateCommand.EXIT_VALIDATION);
        }

        @Test
        void overwriteConflict_whenCalled_returnsOne()
                throws IOException {
            Path outputDir =
                    tempDir.resolve("exit-code-ow");
            Files.createDirectories(
                    outputDir.resolve(".claude"));

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString());

            assertThat(exitCode)
                    .isEqualTo(
                            GenerateCommand.EXIT_VALIDATION);
        }
    }

    @Nested
    @DisplayName("Performance")
    class Performance {

        @Test
        void javaQuarkus_fullGeneration_under2Seconds() {
            Path outputDir =
                    tempDir.resolve("perf-test");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            long start = System.nanoTime();

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(),
                    "-f");

            long durationMs =
                    (System.nanoTime() - start) / 1_000_000;

            assertThat(exitCode).isZero();
            assertThat(durationMs).isLessThan(2000);
        }

        @Test
        void typescriptNestjs_fullGeneration_under2Seconds() {
            Path outputDir =
                    tempDir.resolve("perf-test-ts");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            long start = System.nanoTime();

            int exitCode = cmd.execute(
                    "generate", "-s", "typescript-nestjs",
                    "-o", outputDir.toString(),
                    "-f");

            long durationMs =
                    (System.nanoTime() - start) / 1_000_000;

            assertThat(exitCode).isZero();
            assertThat(durationMs).isLessThan(2000);
        }
    }

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }
}
