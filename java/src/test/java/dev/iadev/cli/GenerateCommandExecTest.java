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
 * Tests for GenerateCommand — stack, dry-run, force,
 * overwrite, dangerous path, verbose, valid config,
 * invalid config, performance.
 */
@DisplayName("GenerateCommand — execution")
class GenerateCommandExecTest {

    @TempDir
    Path tempDir;
    private static final String VALID_CONFIG = """
            project:
              name: "test-app"
              purpose: "A test microservice"
            architecture:
              style: microservice
            interfaces:
              - type: rest
            language:
              name: java
              version: "21"
            framework:
              name: spring-boot
              version: "3.4"
              build_tool: maven
            """;
    @Nested
    @DisplayName("Stack profile")
    class StackProfile {

        @Test
        void validStack_dryRun_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());
            assertThat(exitCode).isZero();
        }

        @Test
        void validStack_dryRun_showsSuccess() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());
            assertThat(sw.toString())
                    .contains("Pipeline: Success");
        }

        @Test
        void invalidStack_whenCalled_returnsNonZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-s", "invalid-stack",
                    "--dry-run",
                    "-o", tempDir.toString());
            assertThat(exitCode).isNotZero();
        }
    }

    @Nested
    @DisplayName("Dry-run mode")
    class DryRunMode {

        @Test
        void dryRun_whenCalled_showsDryRunHeader() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());
            assertThat(sw.toString())
                    .contains("[DRY RUN]");
        }

        @Test
        void dryRun_whenCalled_doesNotWriteFiles() {
            Path outputDir =
                    tempDir.resolve("dry-run-output");
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", outputDir.toString());
            assertThat(outputDir.resolve(".claude"))
                    .doesNotExist();
            assertThat(outputDir.resolve(".github"))
                    .doesNotExist();
        }

        @Test
        void dryRun_whenCalled_showsSummaryTable() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());
            assertThat(sw.toString())
                    .contains("Category")
                    .contains("Count")
                    .contains("Total");
        }

        @Test
        void dryRun_whenCalled_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());
            assertThat(exitCode).isZero();
        }
    }

    @Nested
    @DisplayName("Force mode")
    class ForceMode {

        @Test
        void force_withExisting_returnsZero()
                throws IOException {
            Path outputDir =
                    tempDir.resolve("force-output");
            Files.createDirectories(
                    outputDir.resolve(".claude"));
            Files.createDirectories(
                    outputDir.resolve(".github"));
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--force",
                    "-o", outputDir.toString());
            assertThat(exitCode).isZero();
        }

        @Test
        void force_whenCalled_showsOverwriteWarning()
                throws IOException {
            Path outputDir =
                    tempDir.resolve("force-warn");
            Files.createDirectories(
                    outputDir.resolve(".claude"));
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--force",
                    "-o", outputDir.toString());
            assertThat(sw.toString()).contains(
                    "Overwriting existing artifacts");
        }
    }

    @Nested
    @DisplayName("Overwrite detection without --force")
    class OverwriteDetection {

        @Test
        void existingArtifacts_noForce_returnsOne()
                throws IOException {
            Path outputDir =
                    tempDir.resolve("overwrite");
            Files.createDirectories(
                    outputDir.resolve(".claude"));
            Files.createDirectories(
                    outputDir.resolve(".github"));
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString());
            assertThat(exitCode).isEqualTo(
                    GenerateCommand.EXIT_VALIDATION);
        }

        @Test
        void existingArtifacts_noForce_showsConflicts()
                throws IOException {
            Path outputDir =
                    tempDir.resolve("overwrite-msg");
            Files.createDirectories(
                    outputDir.resolve(".claude"));
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString());
            assertThat(sw.toString())
                    .contains("existing")
                    .contains(".claude/")
                    .contains("--force");
        }
    }

    @Nested
    @DisplayName("Dangerous path rejection")
    class DangerousPath {

        @Test
        void homePath_whenCalled_returnsNonZero() {
            String home =
                    System.getProperty("user.home");
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", home);
            assertThat(exitCode).isNotZero();
        }

        @Test
        void homePath_whenCalled_showsRejection() {
            String home =
                    System.getProperty("user.home");
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", home);
            assertThat(sw.toString())
                    .containsIgnoringCase("dangerous")
                    .containsIgnoringCase("home");
        }

        @Test
        void rootPath_whenCalled_returnsNonZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-o", "/");
            assertThat(exitCode).isNotZero();
        }
    }

    @Nested
    @DisplayName("Verbose mode")
    class VerboseMode {

        @Test
        void verbose_whenCalled_showsAssemblerNames() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run", "-v",
                    "-o", tempDir.toString());
            assertThat(sw.toString())
                    .contains("Running RulesAssembler...");
        }

        @Test
        void verbose_whenCalled_showsCompletedTimes() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run", "-v",
                    "-o", tempDir.toString());
            assertThat(sw.toString())
                    .contains("completed in");
        }

        @Test
        void verbose_whenCalled_showsStackLoadMsg() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run", "-v",
                    "-o", tempDir.toString());
            assertThat(sw.toString()).contains(
                    "Loading bundled stack profile");
        }
    }

    @Nested
    @DisplayName("Valid config file")
    class ValidConfigFile {

        @Test
        void validConfig_dryRun_returnsZero()
                throws IOException {
            Path configFile =
                    tempDir.resolve("config.yaml");
            Files.writeString(configFile, VALID_CONFIG,
                    StandardCharsets.UTF_8);
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "--dry-run",
                    "-o", tempDir.resolve("out")
                            .toString());
            assertThat(exitCode).isZero();
        }

        @Test
        void validConfig_dryRun_showsSuccess()
                throws IOException {
            Path configFile =
                    tempDir.resolve("config2.yaml");
            Files.writeString(configFile, VALID_CONFIG,
                    StandardCharsets.UTF_8);
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "--dry-run",
                    "-o", tempDir.resolve("out2")
                            .toString());
            assertThat(sw.toString())
                    .contains("Pipeline: Success");
        }
    }

    @Nested
    @DisplayName("Invalid config")
    class InvalidConfig {

        @Test
        void missingLanguageSection_returnsError()
                throws IOException {
            String invalidConfig = """
                    project:
                      name: "test"
                      purpose: "Testing"
                    architecture:
                      style: microservice
                    interfaces:
                      - type: rest
                    framework:
                      name: spring-boot
                      version: "3.4"
                      build_tool: maven
                    """;
            Path configFile =
                    tempDir.resolve("invalid.yaml");
            Files.writeString(configFile, invalidConfig,
                    StandardCharsets.UTF_8);
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "-o", tempDir.resolve("inv")
                            .toString());
            assertThat(exitCode).isNotZero();
        }
    }

    @Nested
    @DisplayName("Performance")
    class Performance {

        @Test
        void fullPipeline_completesUnder2Seconds() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            long start = System.nanoTime();
            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());
            long durationMs =
                    (System.nanoTime() - start)
                            / 1_000_000;
            assertThat(exitCode).isZero();
            assertThat(durationMs).isLessThan(2000);
        }
    }

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }
}
