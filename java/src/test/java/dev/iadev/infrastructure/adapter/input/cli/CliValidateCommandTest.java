package dev.iadev.infrastructure.adapter.input.cli;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.domain.model.ValidationResult;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions
        .assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CliValidateCommand}.
 *
 * <p>Tests follow TPP ordering: degenerate (null use case)
 * → happy path → error paths → boundary cases.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CliValidateCommand")
class CliValidateCommandTest {

    @Mock
    private ValidateConfigUseCase validateUseCase;

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

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("constructor_nullUseCase"
                + "_throwsNullPointerException")
        void constructor_nullUseCase_throwsNpe() {
            assertThatThrownBy(
                    () -> new CliValidateCommand(null))
                    .isInstanceOf(
                            NullPointerException.class)
                    .hasMessageContaining(
                            "validateUseCase");
        }
    }

    @Nested
    @DisplayName("Happy path — validate via Input Port")
    class HappyPath {

        @Test
        @DisplayName("run_validConfig"
                + "_invokesUseCaseAndReturnsZero")
        void run_validConfig_invokesUseCaseReturnsZero()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));

            var result = executeCommand(
                    "--config",
                    configFile.toString());

            assertThat(result.exitCode()).isZero();
            verify(validateUseCase).validate(any());
        }

        @Test
        @DisplayName("run_validConfig"
                + "_displaysSuccessMessage")
        void run_validConfig_displaysSuccess()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));

            var result = executeCommand(
                    "--config",
                    configFile.toString());

            assertThat(result.stdout())
                    .contains("Configuration is valid");
        }
    }

    @Nested
    @DisplayName("Validation failure via Input Port")
    class ValidationFailurePath {

        @Test
        @DisplayName("run_invalidConfig"
                + "_returnsExitOneWithErrors")
        void run_invalidConfig_returnsExitOne()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            false,
                            List.of("Missing project",
                                    "Bad framework")));

            var result = executeCommand(
                    "--config",
                    configFile.toString());

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout())
                    .contains("Validation failed:");
            assertThat(result.stdout())
                    .contains("Missing project");
            assertThat(result.stdout())
                    .contains("Bad framework");
        }
    }

    @Nested
    @DisplayName("File not found")
    class FileNotFound {

        @Test
        @DisplayName("run_missingFile"
                + "_returnsExitOneWithoutCallingUseCase")
        void run_missingFile_returnsExitOneNoUseCase() {
            String missing = tempDir.resolve("missing.yaml")
                    .toString();

            var result = executeCommand(
                    "--config", missing);

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stdout()).contains(
                    "Error: Configuration file not found:");
            verify(validateUseCase, never())
                    .validate(any());
        }
    }

    @Nested
    @DisplayName("Verbose mode")
    class VerboseMode {

        @Test
        @DisplayName("run_verboseFlag"
                + "_showsAdditionalOutput")
        void run_verboseFlag_showsAdditionalOutput()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));

            var result = executeCommand(
                    "--config",
                    configFile.toString(),
                    "--verbose");

            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout())
                    .contains("Configuration is valid");
        }
    }

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("command_implementsCallable")
        void command_implementsCallable() {
            var cmd = new CliValidateCommand(
                    validateUseCase);
            assertThat(cmd).isInstanceOf(
                    java.util.concurrent.Callable.class);
        }
    }

    // --- Helper methods ---

    private Path writeYaml(String content)
            throws IOException {
        Path file = tempDir.resolve("config.yaml");
        Files.writeString(file, content);
        return file;
    }

    private ExecutionResult executeCommand(
            String... args) {
        var cmd = new CliValidateCommand(validateUseCase);
        var commandLine = new CommandLine(cmd);
        var outSw = new StringWriter();
        var errSw = new StringWriter();
        commandLine.setOut(new PrintWriter(outSw));
        commandLine.setErr(new PrintWriter(errSw));
        int exitCode = commandLine.execute(args);
        return new ExecutionResult(
                exitCode,
                outSw.toString(),
                errSw.toString());
    }

    private record ExecutionResult(
            int exitCode,
            String stdout,
            String stderr) {
    }
}
