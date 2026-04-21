package dev.iadev.smoke;

import dev.iadev.application.lifecycle.LifecycleTransitionMatrix;
import dev.iadev.application.lifecycle.StatusFieldParser;
import dev.iadev.application.lifecycle.StatusSyncException;
import dev.iadev.application.lifecycle.StatusTransitionInvalidException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-loud contract covers for Phase 3.5 of
 * {@code x-task-implement} (RULE-046-08). Validates that the
 * skill aborts with {@code STATUS_SYNC_FAILED} (or
 * {@code STATUS_TRANSITION_INVALID}) when:
 *
 * <ul>
 *   <li>the task file was deleted between Phase 2 and Phase
 *       3.5 (simulates a race with an operator);</li>
 *   <li>the map file is missing;</li>
 *   <li>the proposed transition is outside the matrix
 *       defined by Rule 22.</li>
 * </ul>
 *
 * <p>Story-0046-0003 / TASK-0046-0003-005.</p>
 */
@DisplayName("x-task-implement Phase 3.5 — fail-loud (RULE-046-08)")
class TaskStatusFailLoudTest {

    @Test
    @DisplayName("task file deleted mid-flight -> "
            + "STATUS_SYNC_FAILED with offending path")
    void taskFileDeleted_throwsStatusSyncFailed(
            @TempDir Path dir) throws IOException {
        Path task = dir.resolve(
                "task-TASK-0046-0003-999.md");
        Files.writeString(task,
                "**Status:** Em Andamento\n",
                StandardCharsets.UTF_8);
        // Operator deletes file between Phase 2 and 3.5:
        Files.delete(task);

        assertThatThrownBy(() ->
                StatusFieldParser.writeStatus(
                        task, LifecycleStatus.CONCLUIDA))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining(
                        StatusSyncException.CODE)
                .hasMessageContaining(
                        task.getFileName().toString());
    }

    @Test
    @DisplayName("map file missing -> STATUS_SYNC_FAILED")
    void mapFileMissing_throwsStatusSyncFailed(
            @TempDir Path dir) {
        Path missingMap = dir.resolve("no-map.md");

        assertThatThrownBy(() ->
                TaskMapRowUpdater.updateRow(
                        missingMap,
                        "TASK-0046-0003-999",
                        LifecycleStatus.CONCLUIDA))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining(
                        StatusSyncException.CODE);
    }

    @Test
    @DisplayName("invalid transition (Pendente -> "
            + "Concluída) -> "
            + "STATUS_TRANSITION_INVALID")
    void invalidTransition_throws() {
        assertThatThrownBy(() ->
                LifecycleTransitionMatrix.validateOrThrow(
                        LifecycleStatus.PENDENTE,
                        LifecycleStatus.CONCLUIDA))
                .isInstanceOf(
                        StatusTransitionInvalidException
                                .class);
    }
}
