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
 * Smoke test for the end-to-end finalize contract introduced
 * by story-0046-0004 in {@code x-epic-implement} SKILL.md:
 * Phase 1.7 (per-story) wired into the Core Loop step 6e, plus
 * Phase 5 (epic finalize) after all stories reach SUCCESS.
 *
 * <p>Models a toy v2 epic with 2 stories and walks the happy
 * path exactly as the SKILL.md documents, exercising every
 * helper the skill calls. The git commit step is out of scope
 * (verified by the SKILL.md contract).</p>
 */
@DisplayName("x-epic-implement Phase 1.7 + Phase 5 — smoke")
class EpicImplementFinalizeSmokeTest {

    private static final String MAP_HEADER =
            "| Story | Título | Chave Jira | Blocked By | "
            + "Blocks | Status |\n"
            + "| :--- | :--- | :--- | :--- | :--- | :--- |\n";

    @Test
    @DisplayName("End-to-end v2 epic finalize: 2 stories + "
            + "epic reach Concluída/Concluído")
    void phase1_7_and_phase5_happyPath(
            @TempDir Path dir) throws IOException {
        // Arrange — epic file, 2 story files, map.
        Path epic = dir.resolve("epic-9999.md");
        Files.writeString(epic,
                "# Epic Smoke\n\n**Status:** Em Andamento\n",
                StandardCharsets.UTF_8);
        Path s1 = dir.resolve("story-9999-0001.md");
        Path s2 = dir.resolve("story-9999-0002.md");
        Files.writeString(s1,
                "# Story 1\n\n**Status:** Em Andamento\n",
                StandardCharsets.UTF_8);
        Files.writeString(s2,
                "# Story 2\n\n**Status:** Em Andamento\n",
                StandardCharsets.UTF_8);
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        Files.writeString(map, MAP_HEADER
                + "| story-9999-0001 | S1 | — | — | — | Em Andamento |\n"
                + "| story-9999-0002 | S2 | — | — | — | Em Andamento |\n",
                StandardCharsets.UTF_8);

        // Phase 1.7 wave 1 — story-0001 finalize.
        finalizeStory(s1, map, "story-9999-0001");
        // Phase 1.7 wave 2 — story-0002 finalize.
        finalizeStory(s2, map, "story-9999-0002");

        // Phase 5 — epic finalize.
        LifecycleStatus current = StatusFieldParser
                .readStatus(epic).orElseThrow();
        LifecycleTransitionMatrix.validateOrThrow(
                current, LifecycleStatus.CONCLUIDA);
        StatusFieldParser.writeStatus(
                epic, LifecycleStatus.CONCLUIDA);

        // Assert — all 4 artifacts propagated.
        assertThat(StatusFieldParser.readStatus(s1))
                .contains(LifecycleStatus.CONCLUIDA);
        assertThat(StatusFieldParser.readStatus(s2))
                .contains(LifecycleStatus.CONCLUIDA);
        assertThat(StatusFieldParser.readStatus(epic))
                .contains(LifecycleStatus.CONCLUIDA);
        String mapContent = Files.readString(map,
                StandardCharsets.UTF_8);
        assertThat(mapContent).contains(
                "| story-9999-0001 | S1 | — | — | — "
                + "| Concluída |");
        assertThat(mapContent).contains(
                "| story-9999-0002 | S2 | — | — | — "
                + "| Concluída |");
    }

    private static void finalizeStory(Path story, Path map,
            String storyId) {
        LifecycleStatus current = StatusFieldParser
                .readStatus(story).orElseThrow();
        LifecycleTransitionMatrix.validateOrThrow(
                current, LifecycleStatus.CONCLUIDA);
        StatusFieldParser.writeStatus(
                story, LifecycleStatus.CONCLUIDA);
        EpicMapRowUpdater.updateRow(map, storyId,
                LifecycleStatus.CONCLUIDA);
    }
}
