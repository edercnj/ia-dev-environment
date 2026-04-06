package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Distribution validation tests for the ia-dev-env CLI.
 *
 * <p>Verifies that all CLI commands produce correct output
 * and exit codes, matching the behavior expected from the
 * fat JAR distribution.
 *
 * <p>Tests follow TPP: help, version, validate, generate
 * with profiles, dry-run, force, verbose.
 */
@DisplayName("Distribution CLI Validation")
class DistributionTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Help output")
    class HelpOutput {

        @Test
        void help_whenCalled_containsUsageLine() {
            StringWriter sw = execute("--help");
            assertThat(sw.toString())
                    .contains("Usage: ia-dev-env");
        }

        @Test
        void help_whenCalled_listsGenerateSubcommand() {
            StringWriter sw = execute("--help");
            assertThat(sw.toString())
                    .contains("generate");
        }

        @Test
        void help_whenCalled_listsValidateSubcommand() {
            StringWriter sw = execute("--help");
            assertThat(sw.toString())
                    .contains("validate");
        }

        @Test
        void help_whenCalled_exitCodeIsZero() {
            int exitCode = executeWithCode("--help");
            assertThat(exitCode).isZero();
        }
    }

    @Nested
    @DisplayName("Version output")
    class VersionOutput {

        @Test
        void version_whenCalled_contains2dot0dot0() {
            StringWriter sw = execute("--version");
            assertThat(sw.toString().trim())
                    .contains("2.0.0");
        }

        @Test
        void version_whenCalled_exitCodeIsZero() {
            int exitCode =
                    executeWithCode("--version");
            assertThat(exitCode).isZero();
        }
    }

    @Nested
    @DisplayName("Validate command")
    class ValidateCmd {

        @Test
        void validConfig_whenCalled_exitCodeZero() {
            int exitCode = executeWithCode(
                    "validate", "-c",
                    configPath("java-quarkus"));
            assertThat(exitCode).isZero();
        }

        @Test
        void validConfig_output_containsValid() {
            StringWriter sw = execute(
                    "validate", "-c",
                    configPath("java-quarkus"));
            assertThat(sw.toString())
                    .contains("Configuration is valid");
        }

        @Test
        void missingFile_whenCalled_exitCodeOne() {
            int exitCode = executeWithCode(
                    "validate", "-c",
                    "/nonexistent/config.yaml");
            assertThat(exitCode).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Generate with all profiles")
    class GenerateProfiles {

        @ParameterizedTest
        @ValueSource(strings = {
                "go-gin",
                "java-quarkus",
                "java-spring",
                "kotlin-ktor",
                "python-click-cli",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs"
        })
        void profile_generatesSuccessfully(
                String profile) {
            Path outputDir =
                    tempDir.resolve("dist-" + profile);
            int exitCode = executeWithCode(
                    "generate", "-s", profile,
                    "-o", outputDir.toString(), "-f");
            assertThat(exitCode)
                    .as("Profile " + profile
                            + " should succeed")
                    .isZero();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "go-gin",
                "java-quarkus",
                "java-spring",
                "kotlin-ktor",
                "python-click-cli",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs"
        })
        void profile_generatesClaudeDir(
                String profile) {
            Path outputDir =
                    tempDir.resolve("claude-" + profile);
            executeWithCode(
                    "generate", "-s", profile,
                    "-o", outputDir.toString(), "-f");
            assertThat(outputDir.resolve(".claude"))
                    .isDirectory();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "go-gin",
                "java-quarkus",
                "java-spring",
                "kotlin-ktor",
                "python-click-cli",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs"
        })
        void profile_generatesGithubDir(
                String profile) {
            Path outputDir =
                    tempDir.resolve("gh-" + profile);
            executeWithCode(
                    "generate", "-s", profile,
                    "-o", outputDir.toString(), "-f");
            assertThat(outputDir.resolve(".github"))
                    .isDirectory();
        }
    }

    @Nested
    @DisplayName("Dry-run mode")
    class DryRunMode {

        @Test
        void dryRun_whenCalled_exitCodeZero() {
            int exitCode = executeWithCode(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());
            assertThat(exitCode).isZero();
        }

        @Test
        void dryRun_output_containsWarning() {
            StringWriter sw = execute(
                    "generate", "-s", "java-quarkus",
                    "--dry-run",
                    "-o", tempDir.toString());
            assertThat(sw.toString())
                    .contains("Dry run");
        }
    }

    @Nested
    @DisplayName("Force mode")
    class ForceMode {

        @Test
        void force_whenCalled_overwritesExisting() {
            Path outputDir =
                    tempDir.resolve("force-test");

            // First generation
            executeWithCode(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(), "-f");

            // Second generation with force
            int exitCode = executeWithCode(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(), "-f");

            assertThat(exitCode).isZero();
        }
    }

    @Nested
    @DisplayName("Verbose mode")
    class VerboseMode {

        @Test
        void verbose_whenCalled_showsAssemblerNames() {
            Path outputDir =
                    tempDir.resolve("verbose-test");
            StringWriter sw = execute(
                    "generate", "-s", "java-quarkus",
                    "-o", outputDir.toString(),
                    "-f", "-v");
            assertThat(sw.toString())
                    .contains("Platform filter:")
                    .contains("INCLUDED:");
        }
    }

    private StringWriter execute(String... args) {
        var cmd = new CommandLine(
                new IaDevEnvApplication());
        var sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        cmd.setErr(new PrintWriter(new StringWriter()));
        cmd.execute(args);
        return sw;
    }

    private int executeWithCode(String... args) {
        var cmd = new CommandLine(
                new IaDevEnvApplication());
        var sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        cmd.setErr(new PrintWriter(new StringWriter()));
        return cmd.execute(args);
    }

    private String configPath(String profile) {
        return "src/main/resources/shared/config-templates/"
                + "setup-config." + profile + ".yaml";
    }
}
