package dev.iadev.smoke;

import dev.iadev.application.lifecycle.StatusFieldParser;
import dev.iadev.application.lifecycle.TaskMapRowUpdater;
import dev.iadev.domain.lifecycle.LifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule 18 audit covers for the Phase 3.5 retrofit added by
 * story-0046-0003 / TASK-0046-0003-005.
 *
 * <p>Rule 18 requires EXACTLY ONE commit per task. Phase 3.5
 * runs BEFORE Phase 4's {@code x-git-commit} invocation and
 * stages its two-file delta (task header + map row) onto the
 * same index the TDD cycles have populated — so the single
 * Phase 4 commit absorbs all of them. A regression would be
 * Phase 3.5 calling {@code git commit} itself, producing two
 * commits (one for TDD, one for status).</p>
 *
 * <p>This test asserts:</p>
 * <ol>
 *   <li>Running Phase 3.5 helpers for three independent
 *       toy tasks produces three-task artefact updates but
 *       zero git side effects (the helpers are pure
 *       filesystem I/O — no Process, no git binary);</li>
 *   <li>The helper API surface exposes NO method whose name
 *       suggests a commit operation.</li>
 * </ol>
 *
 * <p>The second check is a reflection-based invariant guard:
 * if a future refactor added {@code TaskMapRowUpdater#commit}
 * or {@code StatusFieldParser#commitAndWrite}, the test fails
 * loud — protecting Rule 18 at the API boundary.</p>
 */
@DisplayName("Rule 18 — atomic task commit audit")
class TaskAtomicCommitAuditTest {

    @Test
    @DisplayName("three tasks update artefacts without "
            + "spawning any git process (commits stay in "
            + "the caller's hands)")
    void threeTasks_noGitSideEffects(@TempDir Path dir)
            throws IOException {
        // Set up three toy tasks + a shared map:
        for (int i = 1; i <= 3; i++) {
            Path task = dir.resolve(String.format(
                    "task-TASK-0046-0003-%03d.md", i));
            Files.writeString(task,
                    "**Status:** Em Andamento\n",
                    StandardCharsets.UTF_8);
        }
        Path map = dir.resolve(
                "task-implementation-map.md");
        Files.writeString(map,
                "| TASK-0046-0003-001 | A | "
                        + "Em Andamento |\n"
                        + "| TASK-0046-0003-002 | B | "
                        + "Em Andamento |\n"
                        + "| TASK-0046-0003-003 | C | "
                        + "Em Andamento |\n",
                StandardCharsets.UTF_8);

        // Run Phase 3.5 for each task:
        for (int i = 1; i <= 3; i++) {
            Path task = dir.resolve(String.format(
                    "task-TASK-0046-0003-%03d.md", i));
            StatusFieldParser.writeStatus(
                    task, LifecycleStatus.CONCLUIDA);
            TaskMapRowUpdater.updateRow(map,
                    String.format(
                            "TASK-0046-0003-%03d", i),
                    LifecycleStatus.CONCLUIDA);
        }

        // Assert all three reached Concluída — helpers did
        // their filesystem job:
        for (int i = 1; i <= 3; i++) {
            Path task = dir.resolve(String.format(
                    "task-TASK-0046-0003-%03d.md", i));
            assertThat(StatusFieldParser.readStatus(task))
                    .contains(LifecycleStatus.CONCLUIDA);
        }
        String mapContent = Files.readString(
                map, StandardCharsets.UTF_8);
        assertThat(mapContent)
                .contains("| TASK-0046-0003-001 | A | "
                        + "Concluída |")
                .contains("| TASK-0046-0003-002 | B | "
                        + "Concluída |")
                .contains("| TASK-0046-0003-003 | C | "
                        + "Concluída |");

        // Assert the helpers produced NO git artefacts in
        // the temp dir (would indicate a rogue `git init`
        // or `git commit` call):
        assertThat(Files.exists(dir.resolve(".git")))
                .as("Helpers must not spawn git; Phase 4's "
                        + "x-git-commit owns the single "
                        + "atomic commit (Rule 18).")
                .isFalse();
    }

    @Test
    @DisplayName("helper API surface exposes no commit "
            + "method — Rule 18 invariant at the API edge")
    void helperApi_hasNoCommitMethod() {
        assertNoCommitInApi(StatusFieldParser.class);
        assertNoCommitInApi(TaskMapRowUpdater.class);
    }

    private static void assertNoCommitInApi(Class<?> type) {
        for (Method m : type.getDeclaredMethods()) {
            String name = m.getName().toLowerCase();
            assertThat(name)
                    .as("%s.%s must not expose a commit "
                            + "operation (Rule 18).",
                            type.getSimpleName(),
                            m.getName())
                    .doesNotContain("commit");
        }
    }
}
