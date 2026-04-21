package dev.iadev.application.lifecycle;

import dev.iadev.domain.lifecycle.LifecycleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TaskMapRowUpdater}. Covers the
 * pure-function rewrite path, I/O path, idempotency, and the
 * fail-loud absent-row case. Story-0046-0003 /
 * TASK-0046-0003-001.
 */
@DisplayName("TaskMapRowUpdater — row rewrite unit tests")
class TaskMapRowUpdaterTest {

    @Nested
    @DisplayName("rewriteRow (pure function)")
    class RewriteRow {

        @Test
        @DisplayName("rewrites the Status column of the "
                + "matching row only")
        void rewriteRow_matchingTaskId_updatesLastColumn() {
            String content = "| Task ID | Desc | Status |\n"
                    + "|---|---|---|\n"
                    + "| TASK-0046-0003-001 | Helper | "
                    + "Em Andamento |\n"
                    + "| TASK-0046-0003-002 | CLI | "
                    + "Pendente |\n";

            String out = TaskMapRowUpdater.rewriteRow(
                    content, "TASK-0046-0003-001",
                    LifecycleStatus.CONCLUIDA);

            assertThat(out).contains(
                    "| TASK-0046-0003-001 | Helper | "
                            + "Concluída |");
            // Other row untouched.
            assertThat(out).contains(
                    "| TASK-0046-0003-002 | CLI | "
                            + "Pendente |");
        }

        @Test
        @DisplayName("idempotent — twice is same as once")
        void rewriteRow_runTwice_sameResult() {
            String content =
                    "| TASK-0046-0003-001 | x | "
                            + "Em Andamento |\n";

            String once = TaskMapRowUpdater.rewriteRow(
                    content, "TASK-0046-0003-001",
                    LifecycleStatus.CONCLUIDA);
            String twice = TaskMapRowUpdater.rewriteRow(
                    once, "TASK-0046-0003-001",
                    LifecycleStatus.CONCLUIDA);

            assertThat(twice).isEqualTo(once);
        }

        @Test
        @DisplayName("returns unchanged when TASK-ID is only "
                + "mentioned in prose (not a table row)")
        void rewriteRow_proseMention_noChange() {
            String content = "See TASK-0046-0003-001 for "
                    + "details.\n";

            String out = TaskMapRowUpdater.rewriteRow(
                    content, "TASK-0046-0003-001",
                    LifecycleStatus.CONCLUIDA);

            assertThat(out).isEqualTo(content);
        }

        @Test
        @DisplayName("returns unchanged when TASK-ID is not "
                + "present at all")
        void rewriteRow_absentTaskId_noChange() {
            String content =
                    "| TASK-9999-9999-999 | x | "
                            + "Pendente |\n";

            String out = TaskMapRowUpdater.rewriteRow(
                    content, "TASK-0046-0003-001",
                    LifecycleStatus.CONCLUIDA);

            assertThat(out).isEqualTo(content);
        }

        @Test
        @DisplayName("preserves surrounding Markdown verbatim")
        void rewriteRow_preservesSurrounding() {
            String content = "# Title\n\nintro\n\n"
                    + "| TASK-0046-0003-001 | x | "
                    + "Em Andamento |\n\n"
                    + "## Footer\n";

            String out = TaskMapRowUpdater.rewriteRow(
                    content, "TASK-0046-0003-001",
                    LifecycleStatus.CONCLUIDA);

            assertThat(out).startsWith("# Title\n\nintro\n\n");
            assertThat(out).endsWith("\n\n## Footer\n");
        }
    }

    @Nested
    @DisplayName("updateRow (I/O)")
    class UpdateRow {

        @Test
        @DisplayName("writes atomically and persists the new "
                + "Status")
        void updateRow_happyPath_writesNewStatus(
                @TempDir Path dir) throws IOException {
            Path map = dir.resolve("task-map.md");
            Files.writeString(map,
                    "| TASK-0046-0003-001 | x | "
                            + "Em Andamento |\n",
                    StandardCharsets.UTF_8);

            TaskMapRowUpdater.updateRow(
                    map, "TASK-0046-0003-001",
                    LifecycleStatus.CONCLUIDA);

            String persisted = Files.readString(
                    map, StandardCharsets.UTF_8);
            assertThat(persisted).contains(
                    "| TASK-0046-0003-001 | x | "
                            + "Concluída |");
        }

        @Test
        @DisplayName("idempotent I/O call is a no-op")
        void updateRow_idempotent_noOp(@TempDir Path dir)
                throws IOException {
            Path map = dir.resolve("task-map.md");
            Files.writeString(map,
                    "| TASK-0046-0003-001 | x | "
                            + "Concluída |\n",
                    StandardCharsets.UTF_8);

            TaskMapRowUpdater.updateRow(
                    map, "TASK-0046-0003-001",
                    LifecycleStatus.CONCLUIDA);

            String persisted = Files.readString(
                    map, StandardCharsets.UTF_8);
            assertThat(persisted).isEqualTo(
                    "| TASK-0046-0003-001 | x | "
                            + "Concluída |\n");
        }

        @Test
        @DisplayName("throws STATUS_SYNC_FAILED when the row "
                + "is absent")
        void updateRow_absentRow_throws(@TempDir Path dir)
                throws IOException {
            Path map = dir.resolve("task-map.md");
            Files.writeString(map,
                    "| TASK-0000-0000-000 | x | "
                            + "Pendente |\n",
                    StandardCharsets.UTF_8);

            assertThatThrownBy(() ->
                    TaskMapRowUpdater.updateRow(
                            map, "TASK-0046-0003-001",
                            LifecycleStatus.CONCLUIDA))
                    .isInstanceOf(StatusSyncException.class)
                    .hasMessageContaining(
                            StatusSyncException.CODE)
                    .hasMessageContaining(
                            "no row matches")
                    .hasMessageContaining(
                            "TASK-0046-0003-001");
        }

        @Test
        @DisplayName("throws when mapFile is null")
        void updateRow_nullFile_throws() {
            assertThatThrownBy(() ->
                    TaskMapRowUpdater.updateRow(null,
                            "TASK-0046-0003-001",
                            LifecycleStatus.CONCLUIDA))
                    .isInstanceOf(StatusSyncException.class);
        }

        @Test
        @DisplayName("throws when taskId is blank")
        void updateRow_blankTaskId_throws(
                @TempDir Path dir) {
            Path map = dir.resolve("task-map.md");
            assertThatThrownBy(() ->
                    TaskMapRowUpdater.updateRow(map, "  ",
                            LifecycleStatus.CONCLUIDA))
                    .isInstanceOf(StatusSyncException.class)
                    .hasMessageContaining("taskId");
        }

        @Test
        @DisplayName("throws when newStatus is null")
        void updateRow_nullStatus_throws(@TempDir Path dir) {
            Path map = dir.resolve("task-map.md");
            assertThatThrownBy(() ->
                    TaskMapRowUpdater.updateRow(map,
                            "TASK-0046-0003-001", null))
                    .isInstanceOf(StatusSyncException.class)
                    .hasMessageContaining("newStatus");
        }

        @Test
        @DisplayName("throws when the file does not exist")
        void updateRow_missingFile_throws(
                @TempDir Path dir) {
            Path missing = dir.resolve("missing.md");

            assertThatThrownBy(() ->
                    TaskMapRowUpdater.updateRow(missing,
                            "TASK-0046-0003-001",
                            LifecycleStatus.CONCLUIDA))
                    .isInstanceOf(StatusSyncException.class)
                    .hasMessageContaining(
                            "failed to read map file");
        }

        @Test
        @DisplayName("coalesced partners — two rows updated "
                + "in sequence preserve each other")
        void updateRow_coalescedPair_bothUpdated(
                @TempDir Path dir) throws IOException {
            Path map = dir.resolve("task-map.md");
            Files.writeString(map,
                    "| TASK-0046-0003-003 | A | "
                            + "Em Andamento |\n"
                            + "| TASK-0046-0003-004 | B | "
                            + "Em Andamento |\n",
                    StandardCharsets.UTF_8);

            TaskMapRowUpdater.updateRow(
                    map, "TASK-0046-0003-003",
                    LifecycleStatus.CONCLUIDA);
            TaskMapRowUpdater.updateRow(
                    map, "TASK-0046-0003-004",
                    LifecycleStatus.CONCLUIDA);

            String persisted = Files.readString(
                    map, StandardCharsets.UTF_8);
            assertThat(persisted).contains(
                    "| TASK-0046-0003-003 | A | "
                            + "Concluída |");
            assertThat(persisted).contains(
                    "| TASK-0046-0003-004 | B | "
                            + "Concluída |");
        }
    }
}
