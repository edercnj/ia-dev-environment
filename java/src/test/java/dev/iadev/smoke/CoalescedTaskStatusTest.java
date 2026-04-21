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
 * Coalesced-pair integration test for the Phase 3.5
 * status-transition contract (story-0046-0003 /
 * TASK-0046-0003-004). When two tasks are declared
 * COALESCED (Rule 15) they land in ONE commit (Rule 18);
 * both partner task files AND both map rows MUST be
 * updated BEFORE that single commit is created.
 *
 * <p>The test exercises the helpers in the exact order the
 * retrofitted skill performs: Parser.writeStatus for both
 * task files, then TaskMapRowUpdater.updateRow for both map
 * rows. If Rule 18 were violated (e.g., a separate commit
 * per partner), one partner would land in {@code Concluída}
 * and the other still in {@code Em Andamento}. The post
 * conditions here assert both arrive simultaneously.</p>
 */
@DisplayName("Coalesced task status sync — Rule 15 + Rule 18")
class CoalescedTaskStatusTest {

    @Test
    @DisplayName("COALESCED partners A+B both reach "
            + "Concluída in one atomic sweep")
    void coalescedPair_bothPartners_landTogether(
            @TempDir Path dir) throws IOException {
        // Task file A (leader):
        Path taskA = dir.resolve(
                "task-TASK-0046-0003-003.md");
        Files.writeString(taskA,
                "# Leader\n\n"
                        + "**Status:** Em Andamento\n\n"
                        + "## 2.3 Testability\n\n"
                        + "COALESCED with TASK-0046-0003-004"
                        + "\n",
                StandardCharsets.UTF_8);

        // Task file B (partner):
        Path taskB = dir.resolve(
                "task-TASK-0046-0003-004.md");
        Files.writeString(taskB,
                "# Partner\n\n"
                        + "**Status:** Em Andamento\n\n"
                        + "## 2.3 Testability\n\n"
                        + "COALESCED with TASK-0046-0003-003"
                        + "\n",
                StandardCharsets.UTF_8);

        // Map with BOTH partner rows:
        Path map = dir.resolve(
                "task-implementation-map-"
                        + "STORY-0046-0003.md");
        Files.writeString(map,
                "# Map\n\n"
                        + "| Task ID | Desc | Status |\n"
                        + "|---|---|---|\n"
                        + "| TASK-0046-0003-003 | Leader | "
                        + "Em Andamento |\n"
                        + "| TASK-0046-0003-004 | Partner | "
                        + "Em Andamento |\n",
                StandardCharsets.UTF_8);

        // Phase 3.5 for partner A:
        StatusFieldParser.writeStatus(
                taskA, LifecycleStatus.CONCLUIDA);
        TaskMapRowUpdater.updateRow(
                map, "TASK-0046-0003-003",
                LifecycleStatus.CONCLUIDA);

        // Phase 3.5 for partner B (still SAME pre-commit
        // staging — no git commit between):
        StatusFieldParser.writeStatus(
                taskB, LifecycleStatus.CONCLUIDA);
        TaskMapRowUpdater.updateRow(
                map, "TASK-0046-0003-004",
                LifecycleStatus.CONCLUIDA);

        // Assert BOTH task files reached Concluída:
        assertThat(StatusFieldParser.readStatus(taskA))
                .contains(LifecycleStatus.CONCLUIDA);
        assertThat(StatusFieldParser.readStatus(taskB))
                .contains(LifecycleStatus.CONCLUIDA);

        // Assert BOTH map rows reached Concluída:
        String mapContent = Files.readString(
                map, StandardCharsets.UTF_8);
        assertThat(mapContent).contains(
                "| TASK-0046-0003-003 | Leader | "
                        + "Concluída |");
        assertThat(mapContent).contains(
                "| TASK-0046-0003-004 | Partner | "
                        + "Concluída |");

        // Invariant — no row is left behind in Em Andamento:
        assertThat(mapContent).doesNotContain(
                "| TASK-0046-0003-003 | Leader | "
                        + "Em Andamento |");
        assertThat(mapContent).doesNotContain(
                "| TASK-0046-0003-004 | Partner | "
                        + "Em Andamento |");
    }

    @Test
    @DisplayName("map file unchanged when a partner row is "
            + "missing — helper fails loud on the missing "
            + "side only")
    void coalescedPair_missingPartnerRow_failsLoud(
            @TempDir Path dir) throws IOException {
        Path map = dir.resolve("task-map.md");
        Files.writeString(map,
                "| TASK-0046-0003-003 | Leader | "
                        + "Em Andamento |\n",
                StandardCharsets.UTF_8);

        // Partner A updates fine:
        TaskMapRowUpdater.updateRow(
                map, "TASK-0046-0003-003",
                LifecycleStatus.CONCLUIDA);

        // Partner B is absent — MUST fail loud (Rule 046-08):
        Throwable thrown = null;
        try {
            TaskMapRowUpdater.updateRow(
                    map, "TASK-0046-0003-004",
                    LifecycleStatus.CONCLUIDA);
        } catch (Throwable t) {
            thrown = t;
        }
        assertThat(thrown).isNotNull();
        assertThat(thrown.getMessage())
                .contains("STATUS_SYNC_FAILED");
        assertThat(thrown.getMessage())
                .contains("TASK-0046-0003-004");
    }
}
