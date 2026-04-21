package dev.iadev.application.lifecycle;

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
 * End-to-end smoke test wiring the three Layer-0 helpers
 * together: {@link StatusFieldParser},
 * {@link LifecycleTransitionMatrix}, and the domain enum.
 * Story-0046-0001 / TASK-0046-0001-006.
 *
 * <p>The test creates a toy markdown artifact, reads its
 * status, validates a transition, writes the new status, and
 * re-reads — exactly the contract downstream stories rely on.
 * A regression in any helper breaks this smoke test loudly.</p>
 */
@DisplayName("Lifecycle foundation — end-to-end smoke")
class LifecycleFoundationSmokeTest {

    @Test
    @DisplayName("Pendente -> Planejada -> Em Andamento -> "
            + "Concluída flows end-to-end")
    void happyPath_fullLifecycle_flowsThroughMatrix(
            @TempDir Path dir) throws IOException {
        Path story = dir.resolve("story.md");
        Files.writeString(story,
                "# Story Smoke\n\n**Status:** Pendente\n\n"
                        + "## 1. Body\n\nContent.\n",
                StandardCharsets.UTF_8);

        // Step 1 — read initial state:
        assertThat(StatusFieldParser.readStatus(story))
                .contains(LifecycleStatus.PENDENTE);

        // Step 2 — validate + apply Pendente -> Planejada:
        LifecycleTransitionMatrix.validateOrThrow(
                LifecycleStatus.PENDENTE,
                LifecycleStatus.PLANEJADA);
        StatusFieldParser.writeStatus(
                story, LifecycleStatus.PLANEJADA);
        assertThat(StatusFieldParser.readStatus(story))
                .contains(LifecycleStatus.PLANEJADA);

        // Step 3 — Planejada -> Em Andamento:
        LifecycleTransitionMatrix.validateOrThrow(
                LifecycleStatus.PLANEJADA,
                LifecycleStatus.EM_ANDAMENTO);
        StatusFieldParser.writeStatus(
                story, LifecycleStatus.EM_ANDAMENTO);
        assertThat(StatusFieldParser.readStatus(story))
                .contains(LifecycleStatus.EM_ANDAMENTO);

        // Step 4 — Em Andamento -> Concluída (terminal):
        LifecycleTransitionMatrix.validateOrThrow(
                LifecycleStatus.EM_ANDAMENTO,
                LifecycleStatus.CONCLUIDA);
        StatusFieldParser.writeStatus(
                story, LifecycleStatus.CONCLUIDA);
        assertThat(StatusFieldParser.readStatus(story))
                .contains(LifecycleStatus.CONCLUIDA);

        // Final artifact preserved its body through the four
        // atomic writes.
        String finalContent = Files.readString(story,
                StandardCharsets.UTF_8);
        assertThat(finalContent)
                .contains("**Status:** Concluída")
                .contains("## 1. Body")
                .contains("Content.");
    }

    @Test
    @DisplayName("LifecycleAuditRunner wires with empty tree")
    void auditRunner_emptyTree_producesNoViolations(
            @TempDir Path dir) {
        LifecycleAuditRunner runner =
                new LifecycleAuditRunner();

        assertThat(runner.scan(dir)).isEmpty();
    }
}
