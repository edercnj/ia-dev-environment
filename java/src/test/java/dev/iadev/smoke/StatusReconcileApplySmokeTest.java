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
 * End-to-end smoke test for the apply path of
 * {@code x-status-reconcile} (story-0046-0006 TASK-004).
 *
 * <p>Builds a toy "legacy epic" sandbox (execution-state.json
 * shows every story SUCCESS but every markdown Pendente), runs
 * the CLI with {@code --apply --non-interactive}, and asserts:
 * (1) exit code 10 (APPLIED); (2) every markdown is rewritten
 * in place; (3) execution-state.json is untouched per
 * RULE-046-07; (4) the final JSON reports the divergences
 * with mode=apply.</p>
 *
 * <p>The CLI itself does NOT invoke git — the
 * {@code x-git-commit} delegation happens at the skill layer
 * (documented in SKILL.md). This smoke asserts only the
 * markdown write contract, which is the piece this story
 * owns.</p>
 */
@DisplayName("x-status-reconcile — apply mode smoke")
class StatusReconcileApplySmokeTest {

    @Test
    @DisplayName("legacy epic sandbox: --apply "
            + "--non-interactive rewrites every markdown "
            + "and returns exit 10")
    void applyRewritesAllMarkdowns(
            @TempDir Path plansRoot) throws IOException {
        Path epicDir = plansRoot.resolve("epic-0024");
        Files.createDirectories(epicDir);
        // 4-story legacy epic: every SUCCESS, every Pendente.
        String state = "{\"version\":\"2.0\","
                + "\"epicId\":\"0024\",\"stories\":{"
                + "\"story-0024-0001\":"
                + "{\"status\":\"SUCCESS\"},"
                + "\"story-0024-0002\":"
                + "{\"status\":\"SUCCESS\"},"
                + "\"story-0024-0003\":"
                + "{\"status\":\"SUCCESS\"},"
                + "\"story-0024-0004\":"
                + "{\"status\":\"SUCCESS\"}}}";
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                state, StandardCharsets.UTF_8);
        byte[] stateBefore = Files.readAllBytes(
                epicDir.resolve("execution-state.json"));

        writeStory(epicDir, "story-0024-0001");
        writeStory(epicDir, "story-0024-0002");
        writeStory(epicDir, "story-0024-0003");
        writeStory(epicDir, "story-0024-0004");
        Files.writeString(epicDir.resolve("epic-0024.md"),
                "# Epic 24\n\n**Status:** Em Andamento\n",
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
        o.apply = true;
        o.nonInteractive = true;

        int code = cli.run(o, plansRoot);

        // (1) exit 10 APPLIED.
        assertThat(code).isEqualTo(
                StatusReconcileCli.APPLIED);

        // (2) every markdown is now Concluída.
        for (int i = 1; i <= 4; i++) {
            String md = Files.readString(
                    epicDir.resolve(String.format(
                            "story-0024-%04d.md", i)));
            assertThat(md).as(
                    "story-0024-%04d.md after apply", i)
                    .contains("**Status:** Concluída");
        }
        assertThat(Files.readString(
                epicDir.resolve("epic-0024.md")))
                .contains("**Status:** Concluída");

        // (3) execution-state.json untouched
        //     (RULE-046-07 — telemetry, not SoT).
        byte[] stateAfter = Files.readAllBytes(
                epicDir.resolve("execution-state.json"));
        assertThat(stateAfter).isEqualTo(stateBefore);

        // (4) final JSON reports mode=apply.
        String last = lastLine(outBuf.toString());
        assertThat(last)
                .contains("\"status\":\"APPLIED\"")
                .contains("\"mode\":\"apply\"")
                .contains("\"divergenceCount\":5");
    }

    @Test
    @DisplayName("per-story apply scope updates only the "
            + "targeted artifact")
    void perStoryApply(@TempDir Path plansRoot)
            throws IOException {
        Path epicDir = plansRoot.resolve("epic-0024");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"version\":\"2.0\","
                        + "\"epicId\":\"0024\","
                        + "\"stories\":{"
                        + "\"story-0024-0001\":"
                        + "{\"status\":\"SUCCESS\"},"
                        + "\"story-0024-0002\":"
                        + "{\"status\":\"SUCCESS\"}}}",
                StandardCharsets.UTF_8);
        writeStory(epicDir, "story-0024-0001");
        writeStory(epicDir, "story-0024-0002");

        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.storyId = "story-0024-0001";
        o.apply = true;
        o.nonInteractive = true;

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.APPLIED);
        assertThat(Files.readString(
                epicDir.resolve("story-0024-0001.md")))
                .contains("**Status:** Concluída");
        // Sibling story NOT touched.
        assertThat(Files.readString(
                epicDir.resolve("story-0024-0002.md")))
                .contains("**Status:** Pendente");
    }

    @Test
    @DisplayName("--apply --dry-run still returns exit 0 "
            + "(mode=diagnose) — no writes")
    void applyWithDryRunSkipsWrites(
            @TempDir Path plansRoot) throws IOException {
        Path epicDir = plansRoot.resolve("epic-0024");
        Files.createDirectories(epicDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"version\":\"2.0\","
                        + "\"epicId\":\"0024\","
                        + "\"stories\":{"
                        + "\"story-0024-0001\":"
                        + "{\"status\":\"SUCCESS\"}}}",
                StandardCharsets.UTF_8);
        writeStory(epicDir, "story-0024-0001");

        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        StatusReconcileCli cli = new StatusReconcileCli(
                new LifecycleReconciler(),
                new PrintWriter(outBuf),
                new PrintWriter(errBuf));
        StatusReconcileCli.Options o =
                new StatusReconcileCli.Options();
        o.epicId = "0024";
        o.apply = true;
        o.dryRun = true;
        o.nonInteractive = true;

        int code = cli.run(o, plansRoot);

        assertThat(code).isEqualTo(
                StatusReconcileCli.SUCCESS);
        assertThat(Files.readString(
                epicDir.resolve("story-0024-0001.md")))
                .contains("**Status:** Pendente");
        assertThat(lastLine(outBuf.toString()))
                .contains("\"mode\":\"diagnose\"");
    }

    private static void writeStory(Path epicDir,
            String storyId) throws IOException {
        Files.writeString(
                epicDir.resolve(storyId + ".md"),
                "# " + storyId + "\n\n"
                        + "**Status:** Pendente\n",
                StandardCharsets.UTF_8);
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
