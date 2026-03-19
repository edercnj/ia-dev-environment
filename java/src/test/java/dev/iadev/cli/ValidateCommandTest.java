package dev.iadev.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ValidateCommand}.
 *
 * <p>Tests follow TPP ordering: help → option parsing → required options.
 */
class ValidateCommandTest {

    @Test
    void help_displaysUsage() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        int exitCode = cmd.execute("validate", "--help");

        assertThat(exitCode).isZero();
        assertThat(sw.toString()).contains("validate");
    }

    @Test
    void help_showsConfigOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("validate", "--help");

        assertThat(sw.toString()).contains("-c", "--config");
    }

    @Test
    void help_showsVerboseOption() {
        var cmd = buildCommandLine();
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("validate", "--help");

        assertThat(sw.toString()).contains("-v", "--verbose");
    }

    @Test
    void call_withConfigOption_returnsZero() {
        var cmd = buildCommandLine();

        int exitCode = cmd.execute("validate", "-c", "config.yaml");

        assertThat(exitCode).isZero();
    }

    @Test
    void call_withConfigAndVerbose_returnsZero() {
        var cmd = buildCommandLine();

        int exitCode = cmd.execute(
                "validate", "--config", "config.yaml", "--verbose");

        assertThat(exitCode).isZero();
    }

    @Test
    void call_withoutRequiredConfig_returnsNonZero() {
        var cmd = buildCommandLine();
        var errSw = new java.io.StringWriter();
        cmd.setErr(new java.io.PrintWriter(errSw));

        int exitCode = cmd.execute("validate");

        assertThat(exitCode).isNotZero();
    }

    private CommandLine buildCommandLine() {
        return new CommandLine(new IaDevEnvApplication());
    }
}
