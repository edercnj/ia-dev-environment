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
 * Tests for GenerateCommand — --platform / -p flag
 * integration with CLI parsing and pipeline execution.
 */
@DisplayName("GenerateCommand — platform flag")
class GenerateCommandPlatformTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Help text")
    class HelpText {

        @Test
        @DisplayName("help shows --platform option")
        void help_whenCalled_showsPlatformOption() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("--platform")
                    .contains("-p");
        }

        @Test
        @DisplayName("help shows accepted values")
        void help_whenCalled_showsAcceptedValues() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("claude-code")
                    .contains("copilot")
                    .contains("codex")
                    .contains("all");
        }

        @Test
        @DisplayName("help shows default behavior")
        void help_whenCalled_showsDefaultBehavior() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .containsIgnoringCase("default")
                    .containsIgnoringCase("all");
        }
    }

    @Nested
    @DisplayName("No platform flag (backward compat)")
    class NoPlatformFlag {

        @Test
        @DisplayName("without --platform generates all "
                + "artifacts")
        void noPlatform_dryRun_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode).isZero();
            assertThat(sw.toString())
                    .contains("Pipeline: Success");
        }
    }

    @Nested
    @DisplayName("Single platform")
    class SinglePlatform {

        @Test
        @DisplayName("--platform claude-code returns "
                + "success")
        void claudeCode_dryRun_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--platform", "claude-code",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode).isZero();
            assertThat(sw.toString())
                    .contains("Pipeline: Success");
        }

        @Test
        @DisplayName("-p copilot returns success")
        void copilot_shortOption_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-p", "copilot",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode).isZero();
        }

        @Test
        @DisplayName("-p codex returns success")
        void codex_shortOption_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-p", "codex",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode).isZero();
        }
    }

    @Nested
    @DisplayName("Multiple platforms (comma-separated)")
    class MultiplePlatforms {

        @Test
        @DisplayName("--platform claude-code,copilot "
                + "returns success")
        void multiplePlatforms_dryRun_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--platform", "claude-code,copilot",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode).isZero();
        }
    }

    @Nested
    @DisplayName("'all' keyword")
    class AllKeyword {

        @Test
        @DisplayName("--platform all returns success")
        void all_dryRun_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--platform", "all",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode).isZero();
        }

        @Test
        @DisplayName("'all' mixed with platform results "
                + "in all")
        void allMixedWithPlatform_dryRun_returnsZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "-p", "claude-code,all",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode).isZero();
        }
    }

    @Nested
    @DisplayName("Invalid platform values")
    class InvalidPlatformValues {

        @Test
        @DisplayName("invalid value returns non-zero "
                + "exit code")
        void invalidPlatform_returnsNonZero() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            var errSw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.setErr(new PrintWriter(errSw));

            int exitCode = cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--platform", "invalid",
                    "--dry-run",
                    "-o", tempDir.toString());

            assertThat(exitCode).isNotZero();
        }

        @Test
        @DisplayName("invalid value shows error message "
                + "with accepted values")
        void invalidPlatform_showsErrorMessage() {
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            var errSw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));
            cmd.setErr(new PrintWriter(errSw));

            cmd.execute(
                    "generate", "-s", "java-quarkus",
                    "--platform", "invalid",
                    "--dry-run",
                    "-o", tempDir.toString());

            String combined =
                    sw.toString() + errSw.toString();
            assertThat(combined)
                    .contains("Invalid platform:");
        }
    }

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }
}
