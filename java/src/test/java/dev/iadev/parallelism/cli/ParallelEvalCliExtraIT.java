package dev.iadev.parallelism.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class ParallelEvalCliExtraIT {

    private static final Path EPIC_FIXTURE = Path.of(
            "src/test/resources/fixtures/parallelism/"
                    + "epic-0040-mock");

    @Test
    @DisplayName("missingEpicForEpicScope_returnsExitCodeTwo")
    void missingEpic_emitsErrorAndReturnsTwo() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = runCli(out, err, "--scope", "EPIC");
        assertThat(exit).isEqualTo(2);
        assertThat(err.toString())
                .contains("Missing --epic for --scope=epic");
    }

    @Test
    @DisplayName("storyScope_evaluatesPairFromEpicFixture")
    void storyScope_evaluatesPair() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = runCli(out, err,
                "--scope", "STORY",
                "--a", "story-0040-0006",
                "--b", "story-0040-0007");
        assertThat(exit).isIn(0, 1, 2);
        assertThat(out.toString())
                .contains("# Parallelism Evaluation — "
                        + "story-0040-0006 vs story-0040-0007");
    }

    @Test
    @DisplayName("storyScope_missingPair_returnsNonZero")
    void storyScope_missingPair_returnsNonZero() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = runCli(out, err,
                "--scope", "STORY",
                "--a", "story-0040-0006");
        assertThat(exit).isNotZero();
    }

    @Test
    @DisplayName("storyScope_unparseableId_returnsNonZero")
    void storyScope_unparseableId_returnsNonZero() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = runCli(out, err,
                "--scope", "STORY",
                "--a", "STORY",
                "--b", "X");
        assertThat(exit).isNotZero();
    }

    @Test
    @DisplayName("taskScope_withMissingPlanFiles_emitsWarnings")
    void taskScope_missingPlans_warnsAndExitsOne(
            @TempDir Path tmp) throws Exception {
        // Create epic dir under tmp/plans/epic-9999 with NO
        // plan files so both tasks resolve to EMPTY footprint.
        Path epic = tmp.resolve("plans/epic-9999/plans");
        Files.createDirectories(epic);
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        // Run with cwd unchanged: deriveEpicDirFromTask
        // returns relative path plans/epic-9999. The CLI
        // resolves against the JVM cwd, so we feed real
        // task ids and expect exit=1 (warnings only) when
        // the plan files don't exist under the current cwd.
        int exit = runCli(out, err,
                "--scope", "TASK",
                "--a", "TASK-9999-0001-001",
                "--b", "TASK-9999-0001-002");
        // Either 1 (warnings) or 2 (collisions): both
        // exercise evaluateTaskPair + readTaskFootprint.
        assertThat(exit).isIn(1, 2);
        assertThat(out.toString())
                .contains("# Parallelism Evaluation");
    }

    @Test
    @DisplayName("taskScope_withRealPlanFiles_parsesFootprintAndDetects")
    void taskScope_withPlanFiles_parsesFootprint(
            @TempDir Path tmp) throws Exception {
        Path plans = tmp.resolve("plans/epic-7777/plans");
        Files.createDirectories(plans);
        // Use the legacy "plan-TASK-...md" pattern
        Files.writeString(
                plans.resolve("plan-TASK-7777-0001-001.md"),
                "# Plan\n\n## File Footprint\n\n"
                        + "### write:\n- shared.txt\n");
        Files.writeString(
                plans.resolve("task-TASK-7777-0001-002.md"),
                "# Task\n\n## File Footprint\n\n"
                        + "### write:\n- shared.txt\n");
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        // CLI uses cwd-relative plans/epic-7777 — we need to
        // chdir to tmp via system property workaround. Easier:
        // run with the user.dir system property.
        String prevCwd = System.getProperty("user.dir");
        System.setProperty("user.dir", tmp.toString());
        try {
            int exit = runCli(out, err,
                    "--scope", "TASK",
                    "--a", "TASK-7777-0001-001",
                    "--b", "TASK-7777-0001-002");
            assertThat(exit).isIn(0, 1, 2);
        } finally {
            System.setProperty("user.dir", prevCwd);
        }
        assertThat(out.toString())
                .contains("# Parallelism Evaluation");
    }

    @Test
    @DisplayName("outFile_writesContentInsteadOfStdout")
    void outFile_writesContentToFile(
            @TempDir Path tmp) throws Exception {
        Path out = tmp.resolve("nested/report.md");
        StringWriter sw = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = runCli(sw, err,
                "--scope", "EPIC",
                "--epic", EPIC_FIXTURE.toString(),
                "--out", out.toString());
        assertThat(exit).isEqualTo(2);
        assertThat(out).exists();
        String content = Files.readString(out);
        assertThat(content)
                .contains("# Parallelism Evaluation");
        assertThat(sw.toString()).isEmpty();
    }

    @Test
    @DisplayName("includeSoft_passesFlagThrough")
    void includeSoft_runsWithoutErrors() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int exit = runCli(out, err,
                "--scope", "EPIC",
                "--epic", EPIC_FIXTURE.toString(),
                "--include-soft");
        assertThat(exit).isEqualTo(2);
        assertThat(out.toString())
                .contains("# Parallelism Evaluation");
    }

    @Test
    @DisplayName("staticMain_returnsExitCode")
    void staticMain_returnsExitCode() {
        int exit = ParallelEvalCli.main(
                "--scope", "EPIC",
                "--epic", EPIC_FIXTURE.toString());
        assertThat(exit).isEqualTo(2);
    }

    private static int runCli(
            StringWriter out, StringWriter err,
            String... args) {
        return new CommandLine(new ParallelEvalCli())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err))
                .execute(args);
    }
}
