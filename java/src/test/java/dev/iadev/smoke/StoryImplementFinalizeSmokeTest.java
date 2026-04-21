package dev.iadev.smoke;

import dev.iadev.application.lifecycle.EpicMapRowUpdater;
import dev.iadev.application.lifecycle.LifecycleTransitionMatrix;
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
 * Smoke test for the Phase 3.8.1–3.8.5 status finalize block
 * wired into {@code x-story-implement} SKILL.md by
 * story-0046-0004 / TASK-0046-0004-002. Exercises the helpers
 * end-to-end in the order the skill documents:
 * read → validate transition → write story → update map row.
 *
 * <p>Step 3.8.5 (x-git-commit) is intentionally not exercised
 * here — that is covered by the SKILL.md contract and by the
 * actual git invocation during a real story run.</p>
 */
@DisplayName("x-story-implement Phase 3.8 finalize — smoke")
class StoryImplementFinalizeSmokeTest {

    @Test
    @DisplayName("Phase 3.8.1..3.8.4 flow: Em Andamento -> "
            + "Concluída + map row updated")
    void phase3_8_finalize_happyPath(
            @TempDir Path dir) throws IOException {
        // Arrange — a minimal toy story + IMPLEMENTATION-MAP.
        Path story = dir.resolve("story-0046-9999.md");
        Files.writeString(story,
                "# Story Smoke\n\n"
                        + "**Status:** Em Andamento\n\n"
                        + "## Body\nContent.\n",
                StandardCharsets.UTF_8);
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        String header =
                "| Story | Título | Chave Jira | Blocked By | "
                + "Blocks | Status |\n"
                + "| :--- | :--- | :--- | :--- | :--- | :--- |\n";
        Files.writeString(map, header
                + "| story-0046-9999 | Smoke | — | — | — | "
                + "Em Andamento |\n",
                StandardCharsets.UTF_8);

        // Step 3.8.1 — read Status.
        LifecycleStatus current = StatusFieldParser
                .readStatus(story).orElseThrow();
        assertThat(current)
                .isEqualTo(LifecycleStatus.EM_ANDAMENTO);

        // Step 3.8.2 — validate transition.
        LifecycleTransitionMatrix.validateOrThrow(
                current, LifecycleStatus.CONCLUIDA);

        // Step 3.8.3 — write story status.
        StatusFieldParser.writeStatus(
                story, LifecycleStatus.CONCLUIDA);

        // Step 3.8.4 — update map row.
        EpicMapRowUpdater.updateRow(map,
                "story-0046-9999",
                LifecycleStatus.CONCLUIDA);

        // Assert — both artifacts propagate.
        assertThat(StatusFieldParser.readStatus(story))
                .contains(LifecycleStatus.CONCLUIDA);
        assertThat(Files.readString(map,
                StandardCharsets.UTF_8))
                .contains("| story-0046-9999 | Smoke | — "
                        + "| — | — | Concluída |");
    }
}
