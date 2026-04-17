package dev.iadev.parallelism.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class ParallelEvalCliIT {

    private static final Path EPIC_FIXTURE = Path.of(
            "src/test/resources/fixtures/parallelism/"
                    + "epic-0040-mock");

    @Test
    void epicScope_detectsHardConflictsOnTelemetryHook() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = runCli(out, err,
                "--scope", "EPIC",
                "--epic", EPIC_FIXTURE.toString());
        assertThat(exit).isEqualTo(2);
        String report = out.toString();
        assertThat(report)
                .contains("# Parallelism Evaluation")
                .contains("hard")
                .contains("story-0040-0006")
                .contains("story-0040-0007")
                .contains("story-0040-0008");
    }

    @Test
    void jsonFormat_producesValidJsonAndCorrectExitCode() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = runCli(out, err,
                "--scope", "EPIC",
                "--epic", EPIC_FIXTURE.toString(),
                "--format", "JSON");
        assertThat(exit).isEqualTo(2);
        String report = out.toString();
        assertThat(report).startsWith("{\n");
        assertThat(report)
                .contains("\"exitCode\": 2")
                .contains("\"hardCount\":")
                .contains("\"collisions\":");
    }

    @Test
    void determinism_twoInvocations_sameBytes() {
        StringWriter o1 = new StringWriter();
        StringWriter o2 = new StringWriter();
        StringWriter e = new StringWriter();
        runCli(o1, e,
                "--scope", "EPIC",
                "--epic", EPIC_FIXTURE.toString());
        runCli(o2, e,
                "--scope", "EPIC",
                "--epic", EPIC_FIXTURE.toString());
        assertThat(o1.toString()).isEqualTo(o2.toString());
    }

    private static int runCli(
            StringWriter out,
            StringWriter err,
            String... args) {
        return new CommandLine(new ParallelEvalCli())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err))
                .execute(args);
    }
}
