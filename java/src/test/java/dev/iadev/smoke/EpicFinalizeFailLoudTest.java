package dev.iadev.smoke;

import dev.iadev.application.lifecycle.EpicMapRowUpdater;
import dev.iadev.application.lifecycle.StatusFieldParser;
import dev.iadev.application.lifecycle.StatusSyncException;
import dev.iadev.domain.lifecycle.LifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fail-loud acceptance coverage for story-0046-0004 / TASK-004.
 * Covers the Gherkin scenario "Falha de status update no
 * epic-finalize (fail loud)": deleting or omitting the epic
 * file before Phase 5 MUST surface as StatusSyncException,
 * which x-epic-implement translates into exit
 * STATUS_SYNC_FAILED per Rule 22 RULE-046-08.
 */
@DisplayName("Epic finalize — fail-loud on missing artifacts")
class EpicFinalizeFailLoudTest {

    @Test
    @DisplayName("Phase 5: epic file missing -> "
            + "StatusSyncException")
    void epicFileMissing_readStatus_throws(
            @TempDir Path dir) {
        Path epic = dir.resolve("epic-9999.md");
        // Intentionally NOT created.
        assertThatThrownBy(() ->
                StatusFieldParser.readStatus(epic))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining(StatusSyncException.CODE)
                .hasMessageContaining(epic.toString());
    }

    @Test
    @DisplayName("Phase 5: epic file missing on write -> "
            + "StatusSyncException")
    void epicFileMissing_writeStatus_throws(
            @TempDir Path dir) {
        Path epic = dir.resolve("epic-9999.md");
        assertThatThrownBy(() ->
                StatusFieldParser.writeStatus(epic,
                        LifecycleStatus.CONCLUIDA))
                .isInstanceOf(StatusSyncException.class);
    }

    @Test
    @DisplayName("Phase 1.7: map file missing -> "
            + "StatusSyncException")
    void mapFileMissing_updateRow_throws(
            @TempDir Path dir) {
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        assertThatThrownBy(() ->
                EpicMapRowUpdater.updateRow(map,
                        "story-9999-0001",
                        LifecycleStatus.CONCLUIDA))
                .isInstanceOf(StatusSyncException.class);
    }
}
