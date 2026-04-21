package dev.iadev.smoke;

import dev.iadev.application.lifecycle.EpicMapRowUpdater;
import dev.iadev.application.lifecycle.StatusFieldParser;
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
 * Idempotency coverage for story-0046-0004 / TASK-004.
 * Covers the Gherkin scenario "Idempotência — re-rodar
 * x-epic-implement em épico já concluído": when every artifact
 * already reads Concluída, re-invoking the finalize helpers
 * leaves byte-for-byte identical files. The skill uses the
 * read-then-decide pattern documented in Phase 5 Step 5 to
 * short-circuit without creating a redundant commit.
 */
@DisplayName("Epic finalize — idempotent on re-run")
class EpicFinalizeIdempotencyTest {

    @Test
    @DisplayName("Re-writing Concluída preserves status on "
            + "re-read (idempotent from Phase 5 contract)")
    void reWrite_sameStatus_statusPreserved(
            @TempDir Path dir) throws IOException {
        Path epic = dir.resolve("epic-9999.md");
        Files.writeString(epic,
                "# Epic Smoke\n\n**Status:** Concluída\n\n"
                        + "Body.\n",
                StandardCharsets.UTF_8);

        // First write — files reach the Concluída state.
        StatusFieldParser.writeStatus(
                epic, LifecycleStatus.CONCLUIDA);
        String afterFirst = Files.readString(epic,
                StandardCharsets.UTF_8);

        // Second write — must be byte-identical with the
        // post-first-write state (true idempotency: the Phase
        // 5 short-circuit observes no drift after N rewrites).
        StatusFieldParser.writeStatus(
                epic, LifecycleStatus.CONCLUIDA);

        assertThat(Files.readString(epic,
                StandardCharsets.UTF_8))
                .isEqualTo(afterFirst);
        assertThat(StatusFieldParser.readStatus(epic))
                .contains(LifecycleStatus.CONCLUIDA);
    }

    @Test
    @DisplayName("Re-updating map row to same Status is a "
            + "byte-identical no-op")
    void reUpdate_sameStatusInMap_fileUnchanged(
            @TempDir Path dir) throws IOException {
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        String original =
                "| Story | Título | Chave Jira | Blocked By "
                + "| Blocks | Status |\n"
                + "| :--- | :--- | :--- | :--- | :--- | "
                + ":--- |\n"
                + "| story-9999-0001 | S1 | — | — | — | "
                + "Concluída |\n";
        Files.writeString(map, original,
                StandardCharsets.UTF_8);

        EpicMapRowUpdater.updateRow(map,
                "story-9999-0001",
                LifecycleStatus.CONCLUIDA);

        assertThat(Files.readString(map,
                StandardCharsets.UTF_8))
                .isEqualTo(original);
    }
}
