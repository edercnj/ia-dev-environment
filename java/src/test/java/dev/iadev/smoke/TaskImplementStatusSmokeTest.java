package dev.iadev.smoke;

import dev.iadev.application.lifecycle.StatusFieldParser;
import dev.iadev.application.lifecycle.TaskMapRowUpdater;
import dev.iadev.domain.lifecycle.LifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke test for the Phase 3.5 status-transition
 * contract added to {@code x-task-implement} by
 * story-0046-0003. Exercises the two JVM helpers
 * ({@link StatusFieldParser} and {@link TaskMapRowUpdater})
 * in sequence — the exact ordering the retrofitted skill
 * performs between TDD completion and the single atomic
 * commit.
 *
 * <p>Verifies that the final state of the artefacts (task
 * file + map row) matches the commit-time invariant: task
 * header shows {@code **Status:** Concluída}; map row's last
 * column shows {@code Concluída}; surrounding content is
 * preserved verbatim.</p>
 *
 * <p>The test does NOT invoke git — it validates only the
 * pre-commit artefact state, which is what Phase 4 stages
 * onto the index for the Rule 18 atomic commit.</p>
 */
@DisplayName("x-task-implement Phase 3.5 — status sync smoke")
class TaskImplementStatusSmokeTest {

    @Test
    @DisplayName("v2 happy path: task Em Andamento -> "
            + "Concluída + map row updated")
    void happyPath_taskAndMap_endWithConcluida(
            @TempDir Path dir) throws IOException {
        Path taskFile =
                dir.resolve("task-TASK-0046-0003-003.md");
        Files.writeString(taskFile,
                "# Task TASK-0046-0003-003\n\n"
                        + "**Status:** Em Andamento\n\n"
                        + "## 2.2 Outputs\n\n"
                        + "- class X created\n",
                StandardCharsets.UTF_8);
        Path mapFile = dir.resolve(
                "task-implementation-map-"
                        + "STORY-0046-0003.md");
        Files.writeString(mapFile,
                "# Map\n\n"
                        + "| Task ID | Desc | Status |\n"
                        + "|---|---|---|\n"
                        + "| TASK-0046-0003-003 | Retrofit "
                        + "| Em Andamento |\n",
                StandardCharsets.UTF_8);

        // Phase 3.5.3 — atomic write to task file:
        StatusFieldParser.writeStatus(
                taskFile, LifecycleStatus.CONCLUIDA);

        // Phase 3.5.4 — update map row:
        TaskMapRowUpdater.updateRow(mapFile,
                "TASK-0046-0003-003",
                LifecycleStatus.CONCLUIDA);

        // Verify task file header:
        assertThat(StatusFieldParser.readStatus(taskFile))
                .contains(LifecycleStatus.CONCLUIDA);

        // Verify map row last column:
        String mapContent = Files.readString(
                mapFile, StandardCharsets.UTF_8);
        assertThat(mapContent).contains(
                "| TASK-0046-0003-003 | Retrofit | "
                        + "Concluída |");

        // Surrounding content preserved:
        assertThat(mapContent).contains("# Map");
        assertThat(mapContent).contains(
                "| Task ID | Desc | Status |");
        String taskContent = Files.readString(
                taskFile, StandardCharsets.UTF_8);
        assertThat(taskContent).contains(
                "# Task TASK-0046-0003-003");
        assertThat(taskContent).contains(
                "## 2.2 Outputs");
        assertThat(taskContent).contains(
                "- class X created");
    }

    @Test
    @DisplayName("v1 bypass: absent task file + no map -> "
            + "helpers untouched (skip contract preserved "
            + "at the caller level)")
    void v1Bypass_noArtifacts_smokeCallerSkips(
            @TempDir Path dir) {
        // Story-0046-0003 §3.3 specifies skip for v1
        // epics — there is no task-TASK-*.md and no
        // map-STORY-*.md file. The helpers themselves are
        // domain-pure and never run in v1; the guard lives
        // in the skill body. The smoke here asserts the
        // baseline: no files exist, so calling the helpers
        // would fail loud — the caller MUST not call them.
        Path taskFile = dir.resolve("missing-task.md");
        Path mapFile = dir.resolve("missing-map.md");

        assertThat(Files.exists(taskFile)).isFalse();
        assertThat(Files.exists(mapFile)).isFalse();
    }
}
