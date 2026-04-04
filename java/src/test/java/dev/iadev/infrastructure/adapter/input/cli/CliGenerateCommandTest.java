package dev.iadev.infrastructure.adapter.input.cli;

import dev.iadev.domain.model.GenerationContext;
import dev.iadev.domain.model.GenerationResult;
import dev.iadev.domain.model.ValidationResult;
import dev.iadev.domain.port.input.GenerateEnvironmentUseCase;
import dev.iadev.domain.port.input.ValidateConfigUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
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
 * Unit tests for {@link CliGenerateCommand}.
 *
 * <p>Tests follow TPP ordering: degenerate (null use case)
 * -> happy path -> error paths -> boundary cases.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CliGenerateCommand")
class CliGenerateCommandTest {

    @Mock
    private GenerateEnvironmentUseCase generateUseCase;

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
        @DisplayName("constructor_nullGenerateUseCase"
                + "_throwsNullPointerException")
        void constructor_nullGenerateUseCase_throwsNpe() {
            assertThatThrownBy(
                    () -> new CliGenerateCommand(
                            null, validateUseCase))
                    .isInstanceOf(
                            NullPointerException.class)
                    .hasMessageContaining(
                            "generateUseCase");
        }

        @Test
        @DisplayName("constructor_nullValidateUseCase"
                + "_throwsNullPointerException")
        void constructor_nullValidateUseCase_throwsNpe() {
            assertThatThrownBy(
                    () -> new CliGenerateCommand(
                            generateUseCase, null))
                    .isInstanceOf(
                            NullPointerException.class)
                    .hasMessageContaining(
                            "validateUseCase");
        }
    }

    @Nested
    @DisplayName("Happy path — generate via Input Port")
    class HappyPath {

        @Test
        @DisplayName("run_validConfig"
                + "_invokesGenerateUseCaseViaInputPort")
        void run_validConfig_invokesGenerateUseCase()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));
            when(generateUseCase.generate(any()))
                    .thenReturn(new GenerationResult(
                            true,
                            List.of("file1.md"),
                            List.of()));

            var result = executeCommand(
                    "--config",
                    configFile.toString(),
                    "--output",
                    tempDir.toString());

            assertThat(result.exitCode()).isZero();
            verify(generateUseCase).generate(any());
        }

        @Test
        @DisplayName("run_validConfig"
                + "_passesCorrectContextToUseCase")
        void run_validConfig_passesCorrectContext()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));
            var captor = ArgumentCaptor.forClass(
                    GenerationContext.class);
            when(generateUseCase.generate(
                    captor.capture()))
                    .thenReturn(new GenerationResult(
                            true,
                            List.of("file1.md"),
                            List.of()));

            executeCommand(
                    "--config",
                    configFile.toString(),
                    "--output",
                    tempDir.toString());

            GenerationContext ctx = captor.getValue();
            assertThat(ctx).isNotNull();
            assertThat(ctx.config()).isNotNull();
            assertThat(ctx.outputDirectory())
                    .isNotNull();
        }

        @Test
        @DisplayName("run_successfulGeneration"
                + "_displaysSuccessOutput")
        void run_successfulGeneration_displaysSuccess()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));
            when(generateUseCase.generate(any()))
                    .thenReturn(new GenerationResult(
                            true,
                            List.of("file1.md",
                                    "file2.md"),
                            List.of()));

            var result = executeCommand(
                    "--config",
                    configFile.toString(),
                    "--output",
                    tempDir.toString());

            assertThat(result.exitCode()).isZero();
            assertThat(result.stdout())
                    .contains("Generation completed");
            assertThat(result.stdout())
                    .contains("2 files generated");
        }
    }

    @Nested
    @DisplayName("Validation failure path")
    class ValidationFailurePath {

        @Test
        @DisplayName("run_invalidConfig"
                + "_returnsValidationExitCode")
        void run_invalidConfig_returnsValidationExit()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            false,
                            List.of("Missing field")));

            var result = executeCommand(
                    "--config",
                    configFile.toString(),
                    "--output",
                    tempDir.toString());

            assertThat(result.exitCode()).isEqualTo(1);
            verify(generateUseCase, never())
                    .generate(any());
        }

        @Test
        @DisplayName("run_invalidConfig"
                + "_displaysValidationErrors")
        void run_invalidConfig_displaysErrors()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            false,
                            List.of("Missing project",
                                    "Invalid lang")));

            var result = executeCommand(
                    "--config",
                    configFile.toString(),
                    "--output",
                    tempDir.toString());

            assertThat(result.stdout())
                    .contains("Validation failed:");
            assertThat(result.stdout())
                    .contains("Missing project");
            assertThat(result.stdout())
                    .contains("Invalid lang");
        }
    }

    @Nested
    @DisplayName("Generation failure path")
    class GenerationFailurePath {

        @Test
        @DisplayName("run_generationFails"
                + "_returnsExecutionExitCode")
        void run_generationFails_returnsExecutionExit()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));
            when(generateUseCase.generate(any()))
                    .thenReturn(new GenerationResult(
                            false,
                            List.of(),
                            List.of("Profile not found")));

            var result = executeCommand(
                    "--config",
                    configFile.toString(),
                    "--output",
                    tempDir.toString());

            assertThat(result.exitCode()).isEqualTo(2);
        }

        @Test
        @DisplayName("run_generationFails"
                + "_displaysWarnings")
        void run_generationFails_displaysWarnings()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));
            when(generateUseCase.generate(any()))
                    .thenReturn(new GenerationResult(
                            false,
                            List.of(),
                            List.of("Profile not found")));

            var result = executeCommand(
                    "--config",
                    configFile.toString(),
                    "--output",
                    tempDir.toString());

            assertThat(result.stdout())
                    .contains("Generation failed");
        }
    }

    @Nested
    @DisplayName("Verbose mode")
    class VerboseMode {

        @Test
        @DisplayName("run_verboseFlag_setsVerboseInContext")
        void run_verboseFlag_setsVerboseInContext()
                throws IOException {
            Path configFile = writeYaml(VALID_CONFIG);
            when(validateUseCase.validate(any()))
                    .thenReturn(new ValidationResult(
                            true, List.of()));
            var captor = ArgumentCaptor.forClass(
                    GenerationContext.class);
            when(generateUseCase.generate(
                    captor.capture()))
                    .thenReturn(new GenerationResult(
                            true,
                            List.of("file1.md"),
                            List.of()));

            executeCommand(
                    "--config",
                    configFile.toString(),
                    "--output",
                    tempDir.toString(),
                    "--verbose");

            assertThat(captor.getValue().verbose())
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("command_implementsCallable")
        void command_implementsCallable() {
            var cmd = new CliGenerateCommand(
                    generateUseCase, validateUseCase);
            assertThat(cmd)
                    .isInstanceOf(
                            java.util.concurrent
                                    .Callable.class);
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
        var cmd = new CliGenerateCommand(
                generateUseCase, validateUseCase);
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
