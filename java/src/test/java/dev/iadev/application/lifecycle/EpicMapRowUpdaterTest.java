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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EpicMapRowUpdater}. Covers the
 * canonical row schema of {@code IMPLEMENTATION-MAP.md} for an
 * epic (6 columns: Story | Título | Chave Jira | Blocked By |
 * Blocks | Status). Distinct from the task-map updater, which
 * operates on a different shape.
 */
@DisplayName("EpicMapRowUpdater — update Status column")
class EpicMapRowUpdaterTest {

    private static final String MAP_HEADER =
            "| Story | Título | Chave Jira | Blocked By | "
            + "Blocks | Status |\n"
            + "| :--- | :--- | :--- | :--- | :--- | :--- |\n";

    @Test
    @DisplayName("updateRow rewrites only the target row "
            + "(happy)")
    void updateRow_targetRow_onlyThatRowChanges(
            @TempDir Path dir) throws IOException {
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        String body = MAP_HEADER
                + "| story-0046-0001 | Foo | — | — | story-0046-0002 | Pendente |\n"
                + "| story-0046-0002 | Bar | — | story-0046-0001 | — | Pendente |\n";
        Files.writeString(map, body, StandardCharsets.UTF_8);

        EpicMapRowUpdater.updateRow(
                map, "story-0046-0001",
                LifecycleStatus.CONCLUIDA);

        String updated = Files.readString(
                map, StandardCharsets.UTF_8);
        assertThat(updated).contains(
                "| story-0046-0001 | Foo | — | — | "
                + "story-0046-0002 | Concluída |");
        assertThat(updated).contains(
                "| story-0046-0002 | Bar | — | "
                + "story-0046-0001 | — | Pendente |");
    }

    @Test
    @DisplayName("updateRow preserves header and separator "
            + "(boundary)")
    void updateRow_preservesHeaderAndSeparator(
            @TempDir Path dir) throws IOException {
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        String body = MAP_HEADER
                + "| story-0046-0001 | Foo | — | — | — | Pendente |\n";
        Files.writeString(map, body, StandardCharsets.UTF_8);

        EpicMapRowUpdater.updateRow(
                map, "story-0046-0001",
                LifecycleStatus.CONCLUIDA);

        String updated = Files.readString(
                map, StandardCharsets.UTF_8);
        assertThat(updated).startsWith(MAP_HEADER);
    }

    @Test
    @DisplayName("updateRow throws when file missing (error)")
    void updateRow_fileMissing_throws(@TempDir Path dir) {
        Path map = dir.resolve("missing.md");
        assertThatThrownBy(() -> EpicMapRowUpdater.updateRow(
                map, "story-0046-0001",
                LifecycleStatus.CONCLUIDA))
                .isInstanceOf(StatusSyncException.class);
    }

    @Test
    @DisplayName("updateRow throws when row not found "
            + "(fail-loud)")
    void updateRow_rowNotFound_throws(@TempDir Path dir)
            throws IOException {
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        Files.writeString(map, MAP_HEADER,
                StandardCharsets.UTF_8);

        assertThatThrownBy(() -> EpicMapRowUpdater.updateRow(
                map, "story-9999-9999",
                LifecycleStatus.CONCLUIDA))
                .isInstanceOf(StatusSyncException.class)
                .hasMessageContaining("row not found");
    }

    @Test
    @DisplayName("updateRow rejects null args")
    void updateRow_nullArgs_throws(@TempDir Path dir) {
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        assertThatThrownBy(() -> EpicMapRowUpdater.updateRow(
                null, "story-0046-0001",
                LifecycleStatus.CONCLUIDA))
                .isInstanceOf(StatusSyncException.class);
        assertThatThrownBy(() -> EpicMapRowUpdater.updateRow(
                map, null, LifecycleStatus.CONCLUIDA))
                .isInstanceOf(StatusSyncException.class);
        assertThatThrownBy(() -> EpicMapRowUpdater.updateRow(
                map, "story-0046-0001", null))
                .isInstanceOf(StatusSyncException.class);
    }

    @Test
    @DisplayName("updateRow is idempotent — same value "
            + "rewrite (boundary)")
    void updateRow_idempotent_sameStatus(
            @TempDir Path dir) throws IOException {
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        String body = MAP_HEADER
                + "| story-0046-0001 | Foo | — | — | — | Concluída |\n";
        Files.writeString(map, body, StandardCharsets.UTF_8);

        EpicMapRowUpdater.updateRow(
                map, "story-0046-0001",
                LifecycleStatus.CONCLUIDA);

        String updated = Files.readString(
                map, StandardCharsets.UTF_8);
        assertThat(updated).isEqualTo(body);
    }

    @Test
    @DisplayName("updateRow tolerates story id with hyphens "
            + "and varying column widths")
    void updateRow_flexibleWhitespace(@TempDir Path dir)
            throws IOException {
        Path map = dir.resolve("IMPLEMENTATION-MAP.md");
        String body = MAP_HEADER
                + "|story-0046-0007|Enforcement|—|"
                + "story-0046-0002, story-0046-0003|—|"
                + "Pendente|\n";
        Files.writeString(map, body, StandardCharsets.UTF_8);

        EpicMapRowUpdater.updateRow(
                map, "story-0046-0007",
                LifecycleStatus.EM_ANDAMENTO);

        String updated = Files.readString(
                map, StandardCharsets.UTF_8);
        assertThat(updated).contains("Em Andamento");
        assertThat(updated).doesNotContain(
                "|Pendente|");
    }
}
