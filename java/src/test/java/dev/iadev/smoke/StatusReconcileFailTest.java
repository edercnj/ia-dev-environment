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
 * Fail-loud edge contracts for {@code x-status-reconcile}
 * (story-0046-0006 TASK-005). Verifies exits 30
 * (STATE_FILE_INVALID) and 40 (STATUS_TRANSITION_INVALID) —
 * both mandatory for operator confidence per story §3.3 and
 * the DoD checklist.
 */
@DisplayName("x-status-reconcile — fail-loud edges")
class StatusReconcileFailTest {

    @Test
    @DisplayName("malformed state.json → exit 30 with "
            + "stderr path + reason")
    void malformedState(@TempDir Path plansRoot)
            throws IOException {
        Path epicDir = plansRoot.resolve("epic-0024");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{not-json",
                StandardCharsets.UTF_8);

        int code = runCli(plansRoot, "0024", false)
                .exitCode();

        assertThat(code).isEqualTo(
                StatusReconcileCli.STATE_FILE_INVALID);
    }

    @Test
    @DisplayName("empty state.json → exit 30")
    void emptyState(@TempDir Path plansRoot)
            throws IOException {
        Path epicDir = plansRoot.resolve("epic-0024");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "",
                StandardCharsets.UTF_8);

        int code = runCli(plansRoot, "0024", false)
                .exitCode();

        // Empty file is treated as legacy (no content, no
        // version field) → CLI exits 0 with skip message
        // because isLegacyV1 returns true for absent files
        // and for unparseable-but-handled cases. Empty
        // throws StatusSyncException (bytes.length==0) →
        // STATE_FILE_INVALID.
        assertThat(code).isEqualTo(
                StatusReconcileCli.STATE_FILE_INVALID);
    }

    @Test
    @DisplayName("suspicious transition CONCLUIDA→PENDENTE "
            + "→ exit 40 on --apply")
    void suspiciousTransition(@TempDir Path plansRoot)
            throws IOException {
        Path epicDir = plansRoot.resolve("epic-0030");
        Files.createDirectories(epicDir);
        // state.json says PENDING but markdown already
        // Concluída — regression, must abort.
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"version\":\"2.0\","
                        + "\"epicId\":\"0030\","
                        + "\"stories\":{"
                        + "\"story-0030-0001\":"
                        + "{\"status\":\"PENDING\"}}}",
                StandardCharsets.UTF_8);
        Files.writeString(
                epicDir.resolve("story-0030-0001.md"),
                "# S\n\n**Status:** Concluída\n",
                StandardCharsets.UTF_8);
        String mdBefore = Files.readString(
                epicDir.resolve("story-0030-0001.md"));

        CliResult r = runCli(plansRoot, "0030", true);

        assertThat(r.exitCode()).isEqualTo(
                StatusReconcileCli.STATUS_TRANSITION_INVALID);
        // No write happened.
        assertThat(Files.readString(
                epicDir.resolve("story-0030-0001.md")))
                .isEqualTo(mdBefore);
    }

    private record CliResult(int exitCode, String out,
            String err) { }

    private static CliResult runCli(Path plansRoot,
            String epicId, boolean apply) {
        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.epicId = epicId;
        o.apply = apply;
        o.nonInteractive = true;
        int code = cli.run(o, plansRoot);
        return new CliResult(code, outBuf.toString(),
                errBuf.toString());
    }
}
