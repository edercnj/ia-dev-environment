package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GenerateCommand — help, options,
 * mutual exclusivity, no input, config not found.
 */
@DisplayName("GenerateCommand — help + validation")
class GenerateCommandHelpTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Help and Options")
    class HelpAndOptions {

        @Test
        void help_whenCalled_displaysUsage() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "--help");

            assertThat(exitCode).isZero();
            assertThat(sw.toString()).contains("generate");
        }

        @Test
        void help_whenCalled_showsConfigOption() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("-c", "--config");
        }

        @Test
        void help_whenCalled_showsInteractiveOption() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("-i", "--interactive");
        }

        @Test
        void help_whenCalled_showsOutputOption() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("-o", "--output");
        }

        @Test
        void help_whenCalled_showsStackOption() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("-s", "--stack");
        }

        @Test
        void help_whenCalled_showsVerboseOption() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("-v", "--verbose");
        }

        @Test
        void help_whenCalled_showsDryRunOption() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute("generate", "--help");

            assertThat(sw.toString()).contains("--dry-run");
        }

        @Test
        void help_whenCalled_showsForceOption() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("-f", "--force");
        }
    }

    @Nested
    @DisplayName("Mutual exclusivity")
    class MutualExclusivity {

        @Test
        void configAndInteractive_whenCalled_returnsNonZero() {
            var cmd = buildCommandLine();
            var errSw = new StringWriter();
            cmd.setErr(new PrintWriter(errSw));

            int exitCode = cmd.execute(
                    "generate", "--config", "x.yaml",
                    "--interactive");

            assertThat(exitCode).isNotZero();
        }

        @Test
        void configAndInteractive_whenCalled_showsMutuallyExclusiveMsg() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            var errSw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.setErr(new PrintWriter(errSw));

            cmd.execute("generate", "--config", "x.yaml",
                    "--interactive");

            String combined =
                    sw.toString() + errSw.toString();
            assertThat(combined)
                    .containsIgnoringCase(
                            "mutually exclusive");
        }
    }

    @Nested
    @DisplayName("No input provided")
    class NoInput {

        @Test
        void noOptions_whenCalled_returnsValidationError() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute("generate");

            assertThat(exitCode).isEqualTo(
                    GenerateCommand.EXIT_VALIDATION);
        }

        @Test
        void noOptions_whenCalled_showsMissingInputMessage() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.execute("generate");

            assertThat(sw.toString())
                    .contains("--config", "--interactive",
                            "--stack");
        }
    }

    @Nested
    @DisplayName("Config file not found")
    class ConfigNotFound {

        @Test
        void nonExistentConfig_whenCalled_returnsValidationError() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-c",
                    "/nonexistent/config.yaml");

            assertThat(exitCode).isNotZero();
        }

        @Test
        void nonExistentConfig_whenCalled_showsErrorMessage() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-c",
                    "/nonexistent/config.yaml");

            assertThat(sw.toString()).contains("Error:");
        }
    }

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }
}
