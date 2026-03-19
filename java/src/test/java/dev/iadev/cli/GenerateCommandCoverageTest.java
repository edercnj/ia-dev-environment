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
 * Coverage tests for {@link GenerateCommand} —
 * targeting uncovered branches in validateConfig,
 * printValidationErrors, and verbose config loading.
 */
@DisplayName("GenerateCommand — coverage")
class GenerateCommandCoverageTest {

    @TempDir
    Path tempDir;

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }

    @Nested
    @DisplayName("Stack validation failure path")
    class StackValidationFailure {

        @Test
        @DisplayName(
                "incompatible config returns validation"
                        + " error with printed errors")
        void incompatibleConfig_returnsValidation()
                throws IOException {
            String config = """
                    project:
                      name: "test"
                      purpose: "test"
                    architecture:
                      style: microservice
                    interfaces:
                      - type: rest
                    language:
                      name: python
                      version: "3.10"
                    framework:
                      name: spring-boot
                      version: "3.4"
                      build_tool: maven
                    """;
            Path configFile =
                    tempDir.resolve("incompat.yaml");
            Files.writeString(configFile, config,
                    StandardCharsets.UTF_8);
            Path outputDir =
                    tempDir.resolve("out-incompat");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "-o", outputDir.toString());

            assertThat(exitCode)
                    .isEqualTo(
                            GenerateCommand.EXIT_VALIDATION);
            assertThat(sw.toString())
                    .contains("Validation failed:")
                    .contains("spring-boot");
        }

        @Test
        @DisplayName(
                "incompatible config prints each"
                        + " error with dash prefix")
        void incompatibleConfig_printsErrors()
                throws IOException {
            String config = """
                    project:
                      name: "test"
                      purpose: "test"
                    architecture:
                      style: microservice
                    interfaces:
                      - type: rest
                    language:
                      name: python
                      version: "3.10"
                    framework:
                      name: spring-boot
                      version: "3.4"
                      build_tool: maven
                    """;
            Path configFile =
                    tempDir.resolve("incompat2.yaml");
            Files.writeString(configFile, config,
                    StandardCharsets.UTF_8);
            Path outputDir =
                    tempDir.resolve("out-incompat2");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "-o", outputDir.toString());

            assertThat(sw.toString()).contains("- ");
        }
    }

    @Nested
    @DisplayName("Verbose config loading path")
    class VerboseConfigLoading {

        @Test
        @DisplayName(
                "verbose with config file shows"
                        + " loading message")
        void verboseConfig_showsLoadingMessage()
                throws IOException {
            String config = """
                    project:
                      name: "test"
                      purpose: "test"
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
            Path configFile =
                    tempDir.resolve("verbose-cfg.yaml");
            Files.writeString(configFile, config,
                    StandardCharsets.UTF_8);
            Path outputDir =
                    tempDir.resolve("out-verbose");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "--dry-run", "-v",
                    "-o", outputDir.toString());

            assertThat(sw.toString())
                    .contains("Loading config:");
        }
    }
}
