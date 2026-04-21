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
 * Backward-compatibility smoke for
 * {@code x-status-reconcile} against Rule 19
 * (RULE-045-02 / backward-compat of planning schema).
 * story-0046-0006 TASK-005.
 *
 * <p>Legacy v1 epics — identified by absent or {@code "1.0"}
 * {@code version} field in {@code execution-state.json}, OR by
 * the absence of the file entirely — MUST skip silently with
 * exit 0 and a single log line, never triggering a write.</p>
 */
@DisplayName("x-status-reconcile — Rule 19 v1 compat")
class StatusReconcileV1CompatTest {

    @Test
    @DisplayName("v1 epic (version=\"1.0\") → exit 0 with "
            + "skip message; no markdown touched")
    void v1EpicSkips(@TempDir Path plansRoot)
            throws IOException {
        Path epicDir = plansRoot.resolve("epic-0020");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"version\":\"1.0\","
                        + "\"epicId\":\"0020\","
                        + "\"stories\":{"
                        + "\"story-0020-0001\":"
                        + "{\"status\":\"SUCCESS\"}}}",
                StandardCharsets.UTF_8);
        Files.writeString(
                epicDir.resolve("story-0020-0001.md"),
                "# S\n\n**Status:** Pendente\n",
                StandardCharsets.UTF_8);
        String mdBefore = Files.readString(
                epicDir.resolve("story-0020-0001.md"));

        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.epicId = "0020";
        // Even with --apply, v1 epic must skip.
        o.apply = true;
        o.nonInteractive = true;

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.SUCCESS);
        assertThat(outBuf.toString())
                .contains("legacy epic")
                .contains("Rule 19");
        assertThat(Files.readString(
                epicDir.resolve("story-0020-0001.md")))
                .isEqualTo(mdBefore);
    }

    @Test
    @DisplayName("v1 epic (missing version field) → "
            + "exit 0 skip")
    void v1EpicMissingVersionSkips(
            @TempDir Path plansRoot) throws IOException {
        Path epicDir = plansRoot.resolve("epic-0020");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"epicId\":\"0020\","
                        + "\"stories\":{}}",
                StandardCharsets.UTF_8);

        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.epicId = "0020";

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.SUCCESS);
        assertThat(outBuf.toString())
                .contains("legacy epic")
                .contains("Rule 19");
    }
}
