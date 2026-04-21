package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.Divergence;
import dev.iadev.domain.lifecycle.LifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the diff phase of
 * {@link LifecycleReconciler} (story-0046-0006 TASK-001).
 * Tests walk TPP order: single file, multiple files,
 * checkpoint→status mapping, legacy v1 skip, fail-loud on
 * missing / malformed state.json.
 */
@DisplayName("LifecycleReconciler — diff phase")
class LifecycleReconcilerTest {

    private final LifecycleReconciler reconciler =
            new LifecycleReconciler();

    // ---- mapCheckpointToStatus (degenerate level) ----

    @Test
    @DisplayName("mapCheckpointToStatus SUCCESS → Concluída")
    void mapCheckpointToStatus_success() {
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("SUCCESS"))
                .contains(LifecycleStatus.CONCLUIDA);
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("MERGED"))
                .contains(LifecycleStatus.CONCLUIDA);
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("COMPLETE"))
                .contains(LifecycleStatus.CONCLUIDA);
    }

    @Test
    @DisplayName("mapCheckpointToStatus canonical tokens")
    void mapCheckpointToStatus_canonicalTokens() {
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("IN_PROGRESS"))
                .contains(LifecycleStatus.EM_ANDAMENTO);
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("PENDING"))
                .contains(LifecycleStatus.PENDENTE);
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("FAILED"))
                .contains(LifecycleStatus.FALHA);
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("BLOCKED"))
                .contains(LifecycleStatus.BLOQUEADA);
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("PLANNED"))
                .contains(LifecycleStatus.PLANEJADA);
    }

    @Test
    @DisplayName("mapCheckpointToStatus unknown → empty")
    void mapCheckpointToStatus_unknown() {
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus("UNKNOWN"))
                .isEmpty();
        assertThat(LifecycleReconciler
                .mapCheckpointToStatus(null)).isEmpty();
    }

    // ---- isLegacyV1 ----

    @Test
    @DisplayName("isLegacyV1 true when no state.json present")
    void isLegacyV1_missing(@TempDir Path epic)
            throws IOException {
        // Missing file → treated as legacy (skip silently).
        assertThat(reconciler.isLegacyV1(epic)).isTrue();
    }

    @Test
    @DisplayName("isLegacyV1 true when version==1.0")
    void isLegacyV1_version1(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"1.0\","
                + "\"epicId\":\"0020\",\"stories\":{}}");
        assertThat(reconciler.isLegacyV1(epic)).isTrue();
    }

    @Test
    @DisplayName("isLegacyV1 true when version absent")
    void isLegacyV1_versionAbsent(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"epicId\":\"0020\","
                + "\"stories\":{}}");
        assertThat(reconciler.isLegacyV1(epic)).isTrue();
    }

    @Test
    @DisplayName("isLegacyV1 false when version==2.0")
    void isLegacyV1_v2(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"2.0\","
                + "\"epicId\":\"0046\",\"stories\":{}}");
        assertThat(reconciler.isLegacyV1(epic)).isFalse();
    }

    // ---- diff fail-loud ----

    @Test
    @DisplayName("diff null dir throws StatusSyncException")
    void diff_nullDir() {
        assertThatThrownBy(() -> reconciler.diff(null))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining("epicDir is null");
    }

    @Test
    @DisplayName("diff missing state.json throws")
    void diff_missingState(@TempDir Path epic) {
        assertThatThrownBy(() -> reconciler.diff(epic))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining(
                        "execution-state.json not found");
    }

    @Test
    @DisplayName("diff malformed JSON throws")
    void diff_malformedJson(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{not-json");
        assertThatThrownBy(() -> reconciler.diff(epic))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining(
                        "failed to read/parse");
    }

    // ---- diff happy paths ----

    @Test
    @DisplayName("diff legacy v1 returns empty list")
    void diff_legacyV1(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"1.0\","
                + "\"epicId\":\"0020\",\"stories\":{}}");
        assertThat(reconciler.diff(epic)).isEmpty();
    }

    @Test
    @DisplayName("diff single story divergent "
            + "SUCCESS→Concluída")
    void diff_singleStoryDivergent(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"2.0\","
                + "\"epicId\":\"0024\",\"stories\":{"
                + "\"story-0024-0001\":{"
                + "\"status\":\"SUCCESS\"}}}");
        writeMd(epic, "story-0024-0001.md",
                "# S\n\n**Status:** Pendente\n");
        List<Divergence> out = reconciler.diff(epic);
        assertThat(out).hasSize(1);
        Divergence d = out.get(0);
        assertThat(d.artifactId()).isEqualTo(
                "story-0024-0001");
        assertThat(d.from())
                .isEqualTo(LifecycleStatus.PENDENTE);
        assertThat(d.to())
                .isEqualTo(LifecycleStatus.CONCLUIDA);
    }

    @Test
    @DisplayName("diff story already matching → skipped")
    void diff_storyMatching(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"2.0\","
                + "\"epicId\":\"0024\",\"stories\":{"
                + "\"story-0024-0001\":{"
                + "\"status\":\"SUCCESS\"}}}");
        writeMd(epic, "story-0024-0001.md",
                "# S\n\n**Status:** Concluída\n");
        assertThat(reconciler.diff(epic)).isEmpty();
    }

    @Test
    @DisplayName("diff missing story markdown → skipped")
    void diff_missingStoryMd(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"2.0\","
                + "\"epicId\":\"0024\",\"stories\":{"
                + "\"story-0024-0999\":{"
                + "\"status\":\"SUCCESS\"}}}");
        // No markdown for the ghost story → no divergence.
        assertThat(reconciler.diff(epic)).isEmpty();
    }

    @Test
    @DisplayName("diff multiple stories + epic rollup")
    void diff_epicRollup(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"2.0\","
                + "\"epicId\":\"0024\",\"stories\":{"
                + "\"story-0024-0001\":{"
                + "\"status\":\"SUCCESS\"},"
                + "\"story-0024-0002\":{"
                + "\"status\":\"MERGED\"}}}");
        writeMd(epic, "story-0024-0001.md",
                "# S\n\n**Status:** Pendente\n");
        writeMd(epic, "story-0024-0002.md",
                "# S\n\n**Status:** Pendente\n");
        writeMd(epic, "epic-0024.md",
                "# E\n\n**Status:** Em Andamento\n");
        List<Divergence> out = reconciler.diff(epic);
        // 2 stories + 1 epic rollup = 3 divergences.
        assertThat(out).hasSize(3);
        assertThat(out).anyMatch(d ->
                d.artifactId().equals("epic-0024")
                        && d.to()
                        == LifecycleStatus.CONCLUIDA);
    }

    @Test
    @DisplayName("diff epic rollup suppressed when not "
            + "all stories concluded")
    void diff_epicRollupSuppressed(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"2.0\","
                + "\"epicId\":\"0024\",\"stories\":{"
                + "\"story-0024-0001\":{"
                + "\"status\":\"SUCCESS\"},"
                + "\"story-0024-0002\":{"
                + "\"status\":\"IN_PROGRESS\"}}}");
        writeMd(epic, "story-0024-0001.md",
                "# S\n\n**Status:** Pendente\n");
        writeMd(epic, "story-0024-0002.md",
                "# S\n\n**Status:** Pendente\n");
        writeMd(epic, "epic-0024.md",
                "# E\n\n**Status:** Pendente\n");
        List<Divergence> out = reconciler.diff(epic);
        // Only 2 story divergences; epic rollup suppressed
        // because one story is IN_PROGRESS (not all DONE).
        assertThat(out).hasSize(2);
        assertThat(out).noneMatch(d ->
                d.artifactId().startsWith("epic-"));
    }

    @Test
    @DisplayName("diff unknown checkpoint → story skipped")
    void diff_unknownCheckpoint(@TempDir Path epic)
            throws IOException {
        writeState(epic, "{\"version\":\"2.0\","
                + "\"epicId\":\"0024\",\"stories\":{"
                + "\"story-0024-0001\":{"
                + "\"status\":\"WEIRD_TOKEN\"}}}");
        writeMd(epic, "story-0024-0001.md",
                "# S\n\n**Status:** Pendente\n");
        assertThat(reconciler.diff(epic)).isEmpty();
    }

    // ---- Divergence invariants (degenerate) ----

    @Test
    @DisplayName("Divergence rejects same from/to")
    void divergence_rejectsSameFromTo() {
        assertThatThrownBy(() -> new Divergence(
                "x", Path.of("/tmp/x.md"),
                LifecycleStatus.PENDENTE,
                LifecycleStatus.PENDENTE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Divergence accepts null from (missing "
            + "Status header case)")
    void divergence_allowsNullFrom() {
        Divergence d = new Divergence(
                "x", Path.of("/tmp/x.md"), null,
                LifecycleStatus.CONCLUIDA);
        assertThat(d.from()).isNull();
        assertThat(d.to())
                .isEqualTo(LifecycleStatus.CONCLUIDA);
    }

    // ---- helpers ----

    private static void writeState(Path epic, String json)
            throws IOException {
        Files.writeString(
                epic.resolve("execution-state.json"),
                json, StandardCharsets.UTF_8);
    }

    private static void writeMd(Path epic, String name,
            String content) throws IOException {
        Files.writeString(epic.resolve(name), content,
                StandardCharsets.UTF_8);
    }
}
