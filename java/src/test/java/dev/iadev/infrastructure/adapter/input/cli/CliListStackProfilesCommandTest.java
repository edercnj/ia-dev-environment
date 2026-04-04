package dev.iadev.infrastructure.adapter.input.cli;

import dev.iadev.domain.model.StackProfile;
import dev.iadev.domain.port.input.ListStackProfilesUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions
        .assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CliListStackProfilesCommand}.
 *
 * <p>Tests follow TPP ordering: degenerate (null use case)
 * → happy path (empty list) → happy path (non-empty)
 * → boundary cases.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CliListStackProfilesCommand")
class CliListStackProfilesCommandTest {

    @Mock
    private ListStackProfilesUseCase listUseCase;

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("constructor_nullUseCase"
                + "_throwsNullPointerException")
        void constructor_nullUseCase_throwsNpe() {
            assertThatThrownBy(
                    () -> new CliListStackProfilesCommand(
                            null))
                    .isInstanceOf(
                            NullPointerException.class)
                    .hasMessageContaining(
                            "listStackProfilesUseCase");
        }
    }

    @Nested
    @DisplayName("Happy path — empty profiles list")
    class EmptyProfilesList {

        @Test
        @DisplayName("run_noProfiles"
                + "_returnsZeroWithMessage")
        void run_noProfiles_returnsZeroWithMessage() {
            when(listUseCase.listProfiles())
                    .thenReturn(List.of());

            var result = executeCommand();

            assertThat(result.exitCode()).isZero();
            verify(listUseCase).listProfiles();
            assertThat(result.stdout())
                    .contains("No stack profiles available");
        }
    }

    @Nested
    @DisplayName("Happy path — profiles listed")
    class ProfilesListed {

        @Test
        @DisplayName("run_withProfiles"
                + "_invokesUseCaseAndDisplaysProfiles")
        void run_withProfiles_displaysProfiles() {
            when(listUseCase.listProfiles())
                    .thenReturn(List.of(
                            new StackProfile("java-spring",
                                    "java", "spring-boot",
                                    "maven", Map.of()),
                            new StackProfile("python-fastapi",
                                    "python", "fastapi",
                                    "pip", Map.of())));

            var result = executeCommand();

            assertThat(result.exitCode()).isZero();
            verify(listUseCase).listProfiles();
            assertThat(result.stdout())
                    .contains("java-spring");
            assertThat(result.stdout())
                    .contains("python-fastapi");
        }

        @Test
        @DisplayName("run_withProfiles"
                + "_displaysLanguageAndFramework")
        void run_withProfiles_displaysDetails() {
            when(listUseCase.listProfiles())
                    .thenReturn(List.of(
                            new StackProfile("java-spring",
                                    "java", "spring-boot",
                                    "maven", Map.of())));

            var result = executeCommand();

            assertThat(result.stdout())
                    .contains("java");
            assertThat(result.stdout())
                    .contains("spring-boot");
            assertThat(result.stdout())
                    .contains("maven");
        }
    }

    @Nested
    @DisplayName("Interface contract")
    class InterfaceContract {

        @Test
        @DisplayName("command_implementsCallable")
        void command_implementsCallable() {
            var cmd = new CliListStackProfilesCommand(
                    listUseCase);
            assertThat(cmd).isInstanceOf(
                    java.util.concurrent.Callable.class);
        }
    }

    // --- Helper methods ---

    private ExecutionResult executeCommand(
            String... args) {
        var cmd = new CliListStackProfilesCommand(
                listUseCase);
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
