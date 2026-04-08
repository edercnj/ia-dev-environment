package dev.iadev.smoke;

import dev.iadev.cli.IaDevEnvApplication;
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
 * Smoke tests for CLI mode flags: dry-run, force,
 * verbose, and help.
 *
 * <p>Validates that each mode produces the expected
 * behavioral contract when invoked via the CLI.</p>
 */
@DisplayName("CLI Modes Smoke Tests")
class CliModesSmokeTest {

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_VALIDATION = 1;
    private static final String TEST_PROFILE =
            "java-quarkus";

    @Nested
    @DisplayName("Dry-Run Mode")
    class DryRunMode {

        @Test
        @DisplayName("dry-run succeeds with exit code 0")
        void dryRun_whenExecuted_returnsSuccess(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("dry-run-output");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--dry-run",
                    "-o", outputDir.toString());

            assertThat(exitCode)
                    .isEqualTo(EXIT_SUCCESS);
        }

        @Test
        @DisplayName("dry-run writes no files to output dir")
        void dryRun_whenExecuted_writesNoFiles(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("dry-run-empty");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--dry-run",
                    "-o", outputDir.toString());

            assertThat(outputDir).doesNotExist();
        }

        @Test
        @DisplayName("dry-run output contains DRY RUN label")
        void dryRun_whenExecuted_outputContainsDryRunLabel(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("dry-run-label");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--dry-run",
                    "-o", outputDir.toString());

            assertThat(sw.toString())
                    .contains("[DRY RUN]");
        }

        @Test
        @DisplayName("dry-run output reports simulated files")
        void dryRun_whenExecuted_reportsSimulatedFiles(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("dry-run-files");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--dry-run",
                    "-o", outputDir.toString());

            String output = sw.toString();
            assertThat(output)
                    .contains("Files that would be generated");
        }

        @Test
        @DisplayName("dry-run output contains Pipeline Success")
        void dryRun_whenExecuted_reportsPipelineSuccess(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("dry-run-success");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--dry-run",
                    "-o", outputDir.toString());

            assertThat(sw.toString())
                    .contains("Pipeline: Success");
        }
    }

    @Nested
    @DisplayName("Force Mode")
    class ForceMode {

        @Test
        @DisplayName("force overwrites existing artifacts")
        void force_whenExistingArtifacts_overwrites(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir =
                    tempDir.resolve("force-overwrite");

            CommandLine cmd1 = buildCommandLine();
            StringWriter sw1 = new StringWriter();
            cmd1.setOut(new PrintWriter(sw1));
            cmd1.execute(
                    "generate", "-s", TEST_PROFILE,
                    "-o", outputDir.toString());

            Path agentsMd =
                    outputDir.resolve("AGENTS.md");
            assertThat(agentsMd).exists();
            String originalContent =
                    Files.readString(agentsMd,
                            StandardCharsets.UTF_8);

            Files.writeString(agentsMd,
                    "MODIFIED_SENTINEL_CONTENT",
                    StandardCharsets.UTF_8);
            assertThat(Files.readString(agentsMd,
                    StandardCharsets.UTF_8))
                    .isEqualTo("MODIFIED_SENTINEL_CONTENT");

            CommandLine cmd2 = buildCommandLine();
            StringWriter sw2 = new StringWriter();
            cmd2.setOut(new PrintWriter(sw2));
            int exitCode = cmd2.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--force",
                    "-o", outputDir.toString());

            assertThat(exitCode)
                    .isEqualTo(EXIT_SUCCESS);
            String afterForce =
                    Files.readString(agentsMd,
                            StandardCharsets.UTF_8);
            assertThat(afterForce)
                    .isEqualTo(originalContent);
            assertThat(afterForce)
                    .doesNotContain(
                            "MODIFIED_SENTINEL_CONTENT");
        }

        @Test
        @DisplayName("force without existing files succeeds")
        void force_whenNoExistingFiles_succeeds(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("force-fresh");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--force",
                    "-o", outputDir.toString());

            assertThat(exitCode)
                    .isEqualTo(EXIT_SUCCESS);
            assertThat(outputDir.resolve(".claude"))
                    .isDirectory();
        }

        @Test
        @DisplayName("without force, existing artifacts cause"
                + " validation error")
        void noForce_whenExistingArtifacts_returnsError(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir =
                    tempDir.resolve("no-force-conflict");
            Files.createDirectories(
                    outputDir.resolve(".claude"));

            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "-o", outputDir.toString());

            assertThat(exitCode)
                    .isEqualTo(
                            EXIT_VALIDATION);
            assertThat(sw.toString())
                    .contains("--force");
        }

        @Test
        @DisplayName("force output shows overwrite message")
        void force_whenExistingArtifacts_showsOverwriteMsg(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir =
                    tempDir.resolve("force-msg");
            Files.createDirectories(
                    outputDir.resolve(".claude"));

            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--force",
                    "-o", outputDir.toString());

            assertThat(sw.toString())
                    .contains("Overwriting existing artifacts");
        }
    }

    @Nested
    @DisplayName("Verbose Mode")
    class VerboseMode {

        @Test
        @DisplayName("verbose succeeds with exit code 0")
        void verbose_whenExecuted_returnsSuccess(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("verbose-output");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--verbose", "--force",
                    "-o", outputDir.toString());

            assertThat(exitCode)
                    .isEqualTo(EXIT_SUCCESS);
        }

        @Test
        @DisplayName("verbose output contains assembler names")
        void verbose_whenExecuted_containsAssemblerNames(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("verbose-assemblers");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--verbose", "--force",
                    "-o", outputDir.toString());

            String output = sw.toString();
            assertThat(output).contains("INCLUDED:");
        }

        @Test
        @DisplayName("verbose output contains completion info")
        void verbose_whenExecuted_containsCompletionInfo(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("verbose-completion");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--verbose", "--force",
                    "-o", outputDir.toString());

            String output = sw.toString();
            assertThat(output).contains("completed in");
        }

        @Test
        @DisplayName("verbose output contains loading message")
        void verbose_whenExecuted_containsLoadingMessage(
                @TempDir Path tempDir) {
            Path outputDir =
                    tempDir.resolve("verbose-loading");
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute(
                    "generate", "-s", TEST_PROFILE,
                    "--verbose", "--force",
                    "-o", outputDir.toString());

            String output = sw.toString();
            assertThat(output)
                    .contains("Loading bundled stack profile");
        }
    }

    @Nested
    @DisplayName("Help Mode")
    class HelpMode {

        @Test
        @DisplayName("help returns exit code 0")
        void help_whenCalled_returnsSuccess() {
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "--help");

            assertThat(exitCode).isZero();
        }

        @Test
        @DisplayName("help shows --config flag")
        void help_whenCalled_showsConfigFlag() {
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("--config");
        }

        @Test
        @DisplayName("help shows --stack flag")
        void help_whenCalled_showsStackFlag() {
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("--stack");
        }

        @Test
        @DisplayName("help shows --output flag")
        void help_whenCalled_showsOutputFlag() {
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("--output");
        }

        @Test
        @DisplayName("help shows --dry-run flag")
        void help_whenCalled_showsDryRunFlag() {
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("--dry-run");
        }

        @Test
        @DisplayName("help shows --force flag")
        void help_whenCalled_showsForceFlag() {
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("--force");
        }

        @Test
        @DisplayName("help shows --verbose flag")
        void help_whenCalled_showsVerboseFlag() {
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            assertThat(sw.toString())
                    .contains("--verbose");
        }

        @Test
        @DisplayName("help shows short flags -c, -s, -o, -v, -f")
        void help_whenCalled_showsShortFlags() {
            CommandLine cmd = buildCommandLine();
            StringWriter sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            cmd.execute("generate", "--help");

            String output = sw.toString();
            assertThat(output).contains("-c");
            assertThat(output).contains("-s");
            assertThat(output).contains("-o");
            assertThat(output).contains("-v");
            assertThat(output).contains("-f");
        }
    }

    private static CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }
}
