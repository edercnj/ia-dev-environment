package dev.iadev.smoke;

import dev.iadev.application.lifecycle.LifecycleReconciler;
import dev.iadev.lifecycle.cli.StatusReconcileCli;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Diagnose-mode smoke for {@code x-status-reconcile}
 * (story-0046-0006 TASK-003). Exercises the CLI end-to-end
 * without spawning a process: the reconciler reads
 * execution-state.json + markdowns from a temp "plans/" dir
 * and the CLI prints a report + final JSON via injected
 * writers.
 */
@DisplayName("x-status-reconcile — diagnose mode smoke")
class StatusReconcileDiagnoseSmokeTest {

    @Test
    @DisplayName("diagnose mode on drifted epic returns exit "
            + "0 with divergence count N")
    void diagnose_driftedEpic(@TempDir Path plansRoot)
            throws IOException {
        Path epicDir = plansRoot.resolve("epic-0024");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"version\":\"2.0\","
                        + "\"epicId\":\"0024\","
                        + "\"stories\":{"
                        + "\"story-0024-0001\":{"
                        + "\"status\":\"SUCCESS\"},"
                        + "\"story-0024-0002\":{"
                        + "\"status\":\"SUCCESS\"}}}",
                StandardCharsets.UTF_8);
        Files.writeString(
                epicDir.resolve("story-0024-0001.md"),
                "# S1\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                epicDir.resolve("story-0024-0002.md"),
                "# S2\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);
        Files.writeString(epicDir.resolve("epic-0024.md"),
                "# E\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);

        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.epicId = "0024";

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.SUCCESS);
        String stdout = outBuf.toString();
        assertThat(stdout)
                .contains("Divergence report for epic 0024")
                .contains("story-0024-0001")
                .contains("story-0024-0002")
                .contains("epic-0024")
                .contains("Total divergences: 3");
        // Last line is JSON.
        String lastLine = lastLine(stdout);
        assertThat(lastLine)
                .contains("\"status\":\"SUCCESS\"")
                .contains("\"divergenceCount\":3")
                .contains("\"mode\":\"diagnose\"");
    }

    @Test
    @DisplayName("diagnose mode, clean epic → "
            + "divergenceCount=0 exit 0")
    void diagnose_cleanEpic(@TempDir Path plansRoot)
            throws IOException {
        Path epicDir = plansRoot.resolve("epic-0024");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"version\":\"2.0\","
                        + "\"epicId\":\"0024\","
                        + "\"stories\":{"
                        + "\"story-0024-0001\":{"
                        + "\"status\":\"SUCCESS\"}}}",
                StandardCharsets.UTF_8);
        Files.writeString(
                epicDir.resolve("story-0024-0001.md"),
                "# S1\n\n**Status:** Concluída\n",
                StandardCharsets.UTF_8);
        Files.writeString(epicDir.resolve("epic-0024.md"),
                "# E\n\n**Status:** Concluída\n",
                StandardCharsets.UTF_8);

        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.epicId = "0024";

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.SUCCESS);
        assertThat(outBuf.toString())
                .contains("Total divergences: 0");
    }

    @Test
    @DisplayName("per-story scope narrows divergences to "
            + "one artifact")
    void perStoryScope(@TempDir Path plansRoot)
            throws IOException {
        Path epicDir = plansRoot.resolve("epic-0024");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"version\":\"2.0\","
                        + "\"epicId\":\"0024\","
                        + "\"stories\":{"
                        + "\"story-0024-0001\":{"
                        + "\"status\":\"SUCCESS\"},"
                        + "\"story-0024-0002\":{"
                        + "\"status\":\"SUCCESS\"}}}",
                StandardCharsets.UTF_8);
        Files.writeString(
                epicDir.resolve("story-0024-0001.md"),
                "# S1\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);
        Files.writeString(
                epicDir.resolve("story-0024-0002.md"),
                "# S2\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);

        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.storyId = "story-0024-0001";

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.SUCCESS);
        assertThat(outBuf.toString())
                .contains("story-0024-0001")
                .doesNotContain("story-0024-0002");
    }

    @Test
    @DisplayName("missing epic dir → exit 30")
    void missingEpicDir(@TempDir Path plansRoot) {
        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.epicId = "9999";

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.STATE_FILE_INVALID);
        assertThat(errBuf.toString())
                .contains("epic dir not found");
    }

    @Test
    @DisplayName("no --epic and no --story → usage error")
    void noScope(@TempDir Path plansRoot) {
        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.USAGE_ERROR);
    }

    private static String lastLine(String s) {
        String[] lines = s.split("\\R");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].isBlank()) {
                return lines[i];
            }
        }
        return "";
    }
}
