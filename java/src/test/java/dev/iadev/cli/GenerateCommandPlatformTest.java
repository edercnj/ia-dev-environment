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
 * Tests for GenerateCommand — --platform / -p flag
 * integration with CLI parsing, pipeline execution,
 * and YAML platform precedence.
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

    @Nested
    @DisplayName("YAML platform precedence")
    class YamlPrecedence {

        @Test
        @DisplayName("YAML platform parsed, no CLI = YAML")
        void yamlPlatform_noCli_usesYaml()
                throws IOException {
            String yamlConfig = """
                    project:
                      name: "test-app"
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
                    platform: claude-code
                    """;
            Path configFile =
                    tempDir.resolve("config.yaml");
            Files.writeString(configFile, yamlConfig,
                    StandardCharsets.UTF_8);
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "--dry-run",
                    "-o", tempDir.resolve("out")
                            .toString());

            assertThat(exitCode).isZero();
            assertThat(sw.toString())
                    .contains("Pipeline: Success");
        }

        @Test
        @DisplayName("CLI overrides YAML platform")
        void cliOverridesYaml_returnsZero()
                throws IOException {
            String yamlConfig = """
                    project:
                      name: "test-app"
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
                    platform: claude-code
                    """;
            Path configFile =
                    tempDir.resolve("config2.yaml");
            Files.writeString(configFile, yamlConfig,
                    StandardCharsets.UTF_8);
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "--platform", "copilot",
                    "--dry-run",
                    "-o", tempDir.resolve("out2")
                            .toString());

            assertThat(exitCode).isZero();
        }

        @Test
        @DisplayName("YAML invalid platform fails")
        void yamlInvalidPlatform_returnsError()
                throws IOException {
            String yamlConfig = """
                    project:
                      name: "test-app"
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
                    platform: invalid-value
                    """;
            Path configFile =
                    tempDir.resolve("config3.yaml");
            Files.writeString(configFile, yamlConfig,
                    StandardCharsets.UTF_8);
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "--dry-run",
                    "-o", tempDir.resolve("out3")
                            .toString());

            assertThat(exitCode).isNotZero();
            assertThat(sw.toString())
                    .contains("Invalid platform value");
        }

        @Test
        @DisplayName("YAML with platform: all succeeds")
        void yamlPlatformAll_returnsZero()
                throws IOException {
            String yamlConfig = """
                    project:
                      name: "test-app"
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
                    platform: all
                    """;
            Path configFile =
                    tempDir.resolve("config4.yaml");
            Files.writeString(configFile, yamlConfig,
                    StandardCharsets.UTF_8);
            var cmd = buildCommandLine();
            var sw = new StringWriter();
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute(
                    "generate", "-c",
                    configFile.toString(),
                    "--dry-run",
                    "-o", tempDir.resolve("out4")
                            .toString());

            assertThat(exitCode).isZero();
        }
    }

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }
}
