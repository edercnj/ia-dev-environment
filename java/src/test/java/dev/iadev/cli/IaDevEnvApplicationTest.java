package dev.iadev.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IaDevEnvApplication} main CLI entry point.
 *
 * <p>Tests follow TPP ordering: simplest (help) to most complex (subcommands).
 */
class IaDevEnvApplicationTest {

    @Test
    void help_whenCalled_displaysUsageWithAppName() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        int exitCode = cmd.execute("--help");

        assertThat(exitCode).isZero();
        assertThat(sw.toString()).contains("Usage: ia-dev-env");
    }

    @Test
    void help_whenCalled_listsGenerateSubcommand() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("--help");

        assertThat(sw.toString()).contains("generate");
    }

    @Test
    void help_whenCalled_listsValidateSubcommand() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute("--help");

        assertThat(sw.toString()).contains("validate");
    }

    @Test
    void version_whenCalled_displays2dot0dot0() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        int exitCode = cmd.execute("--version");

        assertThat(exitCode).isZero();
        assertThat(sw.toString().trim()).contains("2.0.0");
    }

    @Test
    void noArgs_whenCalled_returnsZeroExitCode() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        int exitCode = cmd.execute();

        assertThat(exitCode).isZero();
    }

    @Test
    void noArgs_whenCalled_displaysUsageHelp() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);
        var sw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));

        cmd.execute();

        assertThat(sw.toString()).contains("Usage: ia-dev-env");
    }

    @Test
    void unknownCommand_whenCalled_returnsNonZeroExitCode() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);
        var sw = new java.io.StringWriter();
        var errSw = new java.io.StringWriter();
        cmd.setOut(new java.io.PrintWriter(sw));
        cmd.setErr(new java.io.PrintWriter(errSw));

        int exitCode = cmd.execute("foobar");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void unknownCommand_whenCalled_showsUnmatchedArgument() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);
        var errSw = new java.io.StringWriter();
        cmd.setErr(new java.io.PrintWriter(errSw));

        cmd.execute("foobar");

        assertThat(errSw.toString()).contains("Unmatched argument");
    }

    @Test
    void subcommandsRegistered_whenCalled_generateAndValidate() {
        var app = new IaDevEnvApplication();
        var cmd = new CommandLine(app);

        assertThat(cmd.getSubcommands()).containsKeys("generate", "validate");
    }
}
