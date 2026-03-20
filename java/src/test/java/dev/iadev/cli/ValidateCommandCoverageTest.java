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
 * Coverage tests for {@link ValidateCommand} —
 * targeting uncovered branches in handleValidationException
 * (verbose missing sections, non-missing-section exception).
 */
@DisplayName("ValidateCommand — coverage")
class ValidateCommandCoverageTest {

    @TempDir
    Path tempDir;

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }

    private Path writeYaml(String content)
            throws IOException {
        Path file = tempDir.resolve("config.yaml");
        Files.writeString(file, content,
                StandardCharsets.UTF_8);
        return file;
    }

    @Nested
    @DisplayName("verbose missing section path")
    class VerboseMissingSection {

        @Test
        @DisplayName(
                "missing section with verbose shows"
                        + " FAIL marker")
        void missingSection_verbose_showsFail()
                throws IOException {
            String config = """
                    project:
                      name: "test"
                      purpose: "test"
                    architecture:
                      style: microservice
                    interfaces:
                      - type: rest
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(config);

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "validate", "-c", file.toString(),
                    "--verbose");

            assertThat(exitCode).isEqualTo(1);
            assertThat(sw.toString())
                    .contains("[FAIL] Mandatory sections"
                            + " present:");
        }

        @Test
        @DisplayName(
                "missing section with verbose lists"
                        + " missing section name")
        void missingSection_verbose_listsName()
                throws IOException {
            String config = """
                    project:
                      name: "test"
                      purpose: "test"
                    architecture:
                      style: microservice
                    interfaces:
                      - type: rest
                    framework:
                      name: spring-boot
                      version: "3.4"
                    """;
            Path file = writeYaml(config);

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "validate", "-c", file.toString(),
                    "--verbose");

            assertThat(sw.toString())
                    .contains("language");
        }
    }

    @Nested
    @DisplayName("ConfigValidationException without"
            + " missing sections")
    class NonMissingSectionException {

        @Test
        @DisplayName(
                "interfaces as scalar triggers"
                        + " field-type validation error")
        void interfacesAsScalar_whenCalled_returnsError()
                throws IOException {
            String config = """
                    project:
                      name: "test"
                      purpose: "test"
                    architecture:
                      style: microservice
                    interfaces: "not-a-list"
                    language:
                      name: java
                      version: "21"
                    framework:
                      name: spring-boot
                      version: "3.4"
                      build_tool: maven
                    """;
            Path file = writeYaml(config);

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "validate", "-c", file.toString());

            assertThat(exitCode).isEqualTo(1);
            assertThat(sw.toString())
                    .contains("Error:");
        }
    }

    @Nested
    @DisplayName("empty YAML root")
    class EmptyYamlRoot {

        @Test
        @DisplayName(
                "null YAML root with verbose shows"
                        + " missing sections")
        void nullRoot_verbose_showsMissingSections()
                throws IOException {
            Path file = writeYaml("---\n");

            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "validate", "-c", file.toString(),
                    "--verbose");

            assertThat(exitCode).isEqualTo(1);
            assertThat(sw.toString())
                    .contains("[FAIL] Mandatory sections"
                            + " present:");
        }
    }
}
