package dev.iadev.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GenerateCommand}.
 *
 * <p>Tests follow TPP ordering: help → option parsing → mutual exclusivity.
 */
class GenerateCommandTest {

    @Test
    void help_displaysUsage() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        int exitCode = cmd.execute("generate", "--help");

        assertThat(exitCode).isZero();
        assertThat(sw.toString()).contains("generate");
    }

    @Test
    void help_showsConfigOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("generate", "--help");

        assertThat(sw.toString()).contains("-c", "--config");
    }

    @Test
    void help_showsInteractiveOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("generate", "--help");

        assertThat(sw.toString()).contains("-i", "--interactive");
    }

    @Test
    void help_showsOutputOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("generate", "--help");

        assertThat(sw.toString()).contains("-o", "--output");
    }

    @Test
    void help_showsStackOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("generate", "--help");

        assertThat(sw.toString()).contains("-s", "--stack");
    }

    @Test
    void help_showsVerboseOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("generate", "--help");

        assertThat(sw.toString()).contains("-v", "--verbose");
    }

    @Test
    void help_showsDryRunOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("generate", "--help");

        assertThat(sw.toString()).contains("--dry-run");
    }

    @Test
    void help_showsForceOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("generate", "--help");

        assertThat(sw.toString()).contains("-f", "--force");
    }

    @Test
    void call_withConfigOption_returnsZero() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        int exitCode = cmd.execute("generate", "-c", "config.yaml");

        assertThat(exitCode).isZero();
    }

    @Test
    void call_withInteractiveOption_returnsZero() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        int exitCode = cmd.execute("generate", "-i");

        assertThat(exitCode).isZero();
    }

    @Test
    void call_withConfigAndInteractive_returnsNonZero() {
        var cmd = buildCommandLine();
        var errSw = new java.io.StringWriter();
        cmd.setErr(new java.io.PrintWriter(errSw));

        int exitCode = cmd.execute(
                "generate", "--config", "x.yaml", "--interactive");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void call_withConfigAndInteractive_showsMutuallyExclusiveMessage() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        var errSw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));
        cmd.setErr(new java.io.PrintWriter(errSw));

        cmd.execute("generate", "--config", "x.yaml", "--interactive");

        String combinedOutput = sw.toString() + errSw.toString();
        assertThat(combinedOutput).containsIgnoringCase("mutually exclusive");
    }

    @Test
    void call_withAllOptions_returnsZero() {
        var cmd = buildCommandLine();

        int exitCode = cmd.execute("generate",
                "-c", "config.yaml",
                "-o", "/tmp/output",
                "-s", "java-spring",
                "-v",
                "--dry-run",
                "-f");

        assertThat(exitCode).isZero();
    }

    @Test
    void call_withNoOptions_returnsZero() {
        var cmd = buildCommandLine();

        int exitCode = cmd.execute("generate");

        assertThat(exitCode).isZero();
    }

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }
}
