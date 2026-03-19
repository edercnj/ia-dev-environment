package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidateCommand}.
 *
 * <p>Tests follow TPP ordering:
 * help/options → valid config (exit 0) → file not found (exit 1)
 * → invalid YAML → missing section → incompatible framework
 * → version error → verbose mode.
 */
@DisplayName("ValidateCommand")
class ValidateCommandTest {

    @TempDir
    Path tempDir;

    private static final String VALID_CONFIG = """
            project:
              name: "my-app"
              purpose: "A microservice"
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

    private static final String MISSING_LANGUAGE_CONFIG = """
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

    private static final String INCOMPATIBLE_CONFIG = """
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

    private static final String VERSION_ERROR_CONFIG = """
            project:
              name: "test"
              purpose: "test"
            architecture:
              style: microservice
            interfaces:
              - type: rest
            language:
              name: java
              version: "11"
            framework:
              name: quarkus
              version: "3.0"
              build_tool: maven
            """;

    private static final String INVALID_YAML = """
            project:
              name: "test
            invalid: [
            """;

    private Path writeYaml(String content) throws IOException {
        Path file = tempDir.resolve("config.yaml");
        Files.writeString(file, content);
        return file;
    }

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }

    private ExecutionResult execute(String... args) {
        var cmd = buildCommandLine();
        var outSw = new StringWriter();
        var errSw = new StringWriter();
        cmd.setOut(new PrintWriter(outSw));
        cmd.setErr(new PrintWriter(errSw));
        int exitCode = cmd.execute(args);
        return new ExecutionResult(
                exitCode, outSw.toString(), errSw.toString());
    }

    private record ExecutionResult(
            int exitCode, String stdout, String stderr) {

        String allOutput() {
            return stdout + stderr;
        }
    }

    @Nested
    @DisplayName("help and option parsing")
    class HelpAndOptions {

        @Test
        @DisplayName("help displays usage with validate command")
        void help_displaysUsage() {
            var result = execute("validate", "--help");

            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout()).contains("validate");
        }

        @Test
        @DisplayName("help shows --config option")
        void help_showsConfigOption() {
            var result = execute("validate", "--help");

            assertThat(result.stdout())
                    .contains("-c", "--config");
        }

        @Test
        @DisplayName("help shows --verbose option")
        void help_showsVerboseOption() {
            var result = execute("validate", "--help");

            assertThat(result.stdout())
                    .contains("-v", "--verbose");
        }

        @Test
        @DisplayName("missing --config returns non-zero exit")
        void call_withoutRequiredConfig_returnsNonZero() {
            var result = execute("validate");

            assertThat(result.exitCode()).isNotZero();
        }
    }

    @Nested
    @DisplayName("valid configuration")
    class ValidConfig {

        @Test
        @DisplayName("valid config returns exit 0 and success message")
        void call_validConfig_returnsZeroWithSuccessMessage()
                throws IOException {
            Path file = writeYaml(VALID_CONFIG);

            var result = execute(
                    "validate", "-c", file.toString());

            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout())
                    .contains("Configuration is valid");
        }

        @Test
        @DisplayName("valid config with verbose shows all PASS")
        void call_validConfigVerbose_showsAllPass()
                throws IOException {
            Path file = writeYaml(VALID_CONFIG);

            var result = execute(
                    "validate", "-c", file.toString(),
                    "--verbose");

            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout())
                    .contains("[PASS] Mandatory sections present")
                    .contains("[PASS] Language-framework compatibility")
                    .contains("[PASS] Version requirements")
                    .contains("[PASS] Architecture style")
                    .contains("[PASS] Interface types")
                    .contains("Configuration is valid");
        }
    }

    @Nested
    @DisplayName("file not found")
    class FileNotFound {

        @Test
        @DisplayName("non-existent file returns exit 1 with error")
        void call_nonExistentFile_returnsOneWithError() {
            String missing = tempDir.resolve("missing.yaml")
                    .toString();

            var result = execute(
                    "validate", "-c", missing);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout()).contains(
                    "Error: Configuration file not found:");
            assertThat(result.stdout()).contains(missing);
        }
    }

    @Nested
    @DisplayName("invalid YAML")
    class InvalidYamlContent {

        @Test
        @DisplayName("invalid YAML returns exit 1 with parse error")
        void call_invalidYaml_returnsOneWithParseError()
                throws IOException {
            Path file = writeYaml(INVALID_YAML);

            var result = execute(
                    "validate", "-c", file.toString());

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout()).contains("Error:");
        }
    }

    @Nested
    @DisplayName("missing required section")
    class MissingSection {

        @Test
        @DisplayName("missing language section returns exit 1")
        void call_missingLanguage_returnsOneWithErrors()
                throws IOException {
            Path file = writeYaml(MISSING_LANGUAGE_CONFIG);

            var result = execute(
                    "validate", "-c", file.toString());

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout())
                    .contains("Validation failed:");
        }
    }

    @Nested
    @DisplayName("incompatible framework")
    class IncompatibleFramework {

        @Test
        @DisplayName("python with spring-boot returns exit 1")
        void call_incompatibleFramework_returnsOneWithError()
                throws IOException {
            Path file = writeYaml(INCOMPATIBLE_CONFIG);

            var result = execute(
                    "validate", "-c", file.toString());

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout())
                    .contains("Validation failed:");
            assertThat(result.stdout())
                    .contains("spring-boot");
        }

        @Test
        @DisplayName("verbose mode shows FAIL for compatibility")
        void call_incompatibleVerbose_showsFail()
                throws IOException {
            Path file = writeYaml(INCOMPATIBLE_CONFIG);

            var result = execute(
                    "validate", "-c", file.toString(),
                    "--verbose");

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout()).contains(
                    "[FAIL] Language-framework compatibility:");
            assertThat(result.stdout())
                    .contains("Validation failed:");
        }
    }

    @Nested
    @DisplayName("version error")
    class VersionError {

        @Test
        @DisplayName("Java 11 + Quarkus 3 returns exit 1")
        void call_versionError_returnsOneWithError()
                throws IOException {
            Path file = writeYaml(VERSION_ERROR_CONFIG);

            var result = execute(
                    "validate", "-c", file.toString());

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout())
                    .contains("Validation failed:");
            assertThat(result.stdout()).containsIgnoringCase(
                    "java 17+");
        }

        @Test
        @DisplayName("verbose mode shows FAIL for version")
        void call_versionErrorVerbose_showsFail()
                throws IOException {
            Path file = writeYaml(VERSION_ERROR_CONFIG);

            var result = execute(
                    "validate", "-c", file.toString(),
                    "--verbose");

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout())
                    .contains("[FAIL] Version requirements:");
        }
    }

    @Nested
    @DisplayName("no stack traces visible")
    class NoStackTraces {

        @Test
        @DisplayName("error output does not contain stack traces")
        void call_error_noStackTrace() {
            String missing = tempDir.resolve("missing.yaml")
                    .toString();

            var result = execute(
                    "validate", "-c", missing);

            assertThat(result.allOutput())
                    .doesNotContain("at dev.iadev")
                    .doesNotContain("Exception");
        }

        @Test
        @DisplayName("invalid YAML error has no stack traces")
        void call_invalidYaml_noStackTrace()
                throws IOException {
            Path file = writeYaml(INVALID_YAML);

            var result = execute(
                    "validate", "-c", file.toString());

            assertThat(result.allOutput())
                    .doesNotContain("at dev.iadev");
        }
    }

    @Nested
    @DisplayName("empty file")
    class EmptyFile {

        @Test
        @DisplayName("empty YAML file returns exit 1")
        void call_emptyFile_returnsOne() throws IOException {
            Path file = writeYaml("");

            var result = execute(
                    "validate", "-c", file.toString());

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout())
                    .contains("Validation failed:");
        }
    }
}
