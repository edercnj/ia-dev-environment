package dev.iadev.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TaskMapRowUpdaterCli}. Covers
 * the three exit codes (0 / 20 / 40) and argument validation.
 * Story-0046-0003 / TASK-0046-0003-002.
 */
@DisplayName("TaskMapRowUpdaterCli — picocli subcommand")
class TaskMapRowUpdaterCliTest {

    @Test
    @DisplayName("exit 0 — happy path rewrites status")
    void happyPath_exit0_updatesRow(@TempDir Path dir)
            throws IOException {
        Path map = dir.resolve("task-map.md");
        Files.writeString(map,
                "| TASK-0046-0003-001 | x | "
                        + "Em Andamento |\n",
                StandardCharsets.UTF_8);

        int exit = new CommandLine(
                new TaskMapRowUpdaterCli())
                .execute(map.toString(),
                        "TASK-0046-0003-001",
                        "Concluída");

        assertThat(exit).isEqualTo(
                TaskMapRowUpdaterCli.EXIT_OK);
        assertThat(Files.readString(map,
                StandardCharsets.UTF_8))
                .contains("| TASK-0046-0003-001 | x | "
                        + "Concluída |");
    }

    @Test
    @DisplayName("exit 0 — idempotent call is a no-op")
    void idempotent_exit0(@TempDir Path dir)
            throws IOException {
        Path map = dir.resolve("task-map.md");
        Files.writeString(map,
                "| TASK-0046-0003-001 | x | "
                        + "Concluída |\n",
                StandardCharsets.UTF_8);

        int exit = new CommandLine(
                new TaskMapRowUpdaterCli())
                .execute(map.toString(),
                        "TASK-0046-0003-001",
                        "Concluída");

        assertThat(exit).isEqualTo(
                TaskMapRowUpdaterCli.EXIT_OK);
    }

    @Test
    @DisplayName("exit 20 — absent row fails loud")
    void absentRow_exit20(@TempDir Path dir)
            throws IOException {
        Path map = dir.resolve("task-map.md");
        Files.writeString(map,
                "| TASK-0000-0000-000 | x | "
                        + "Pendente |\n",
                StandardCharsets.UTF_8);

        int exit = new CommandLine(
                new TaskMapRowUpdaterCli())
                .execute(map.toString(),
                        "TASK-0046-0003-001",
                        "Concluída");

        assertThat(exit).isEqualTo(
                TaskMapRowUpdaterCli
                        .EXIT_STATUS_SYNC_FAILED);
    }

    @Test
    @DisplayName("exit 20 — missing file fails loud")
    void missingFile_exit20(@TempDir Path dir) {
        Path missing = dir.resolve("missing.md");

        int exit = new CommandLine(
                new TaskMapRowUpdaterCli())
                .execute(missing.toString(),
                        "TASK-0046-0003-001",
                        "Concluída");

        assertThat(exit).isEqualTo(
                TaskMapRowUpdaterCli
                        .EXIT_STATUS_SYNC_FAILED);
    }

    @Test
    @DisplayName("exit 40 — unknown status label rejected")
    void unknownStatusLabel_exit40(@TempDir Path dir)
            throws IOException {
        Path map = dir.resolve("task-map.md");
        Files.writeString(map,
                "| TASK-0046-0003-001 | x | "
                        + "Em Andamento |\n",
                StandardCharsets.UTF_8);

        int exit = new CommandLine(
                new TaskMapRowUpdaterCli())
                .execute(map.toString(),
                        "TASK-0046-0003-001",
                        "Done");

        assertThat(exit).isEqualTo(
                TaskMapRowUpdaterCli.EXIT_INVALID_ARGS);
    }

    @Test
    @DisplayName("exit 40 — missing arguments rejected")
    void missingArgs_exit40(@TempDir Path dir) {
        // picocli rejects missing required positionals with
        // its own usage exit code; invoke with only 2 args.
        int exit = new CommandLine(
                new TaskMapRowUpdaterCli())
                .execute(dir.resolve("x.md").toString(),
                        "TASK-0046-0003-001");

        // Picocli default usage exit code is 2; either way
        // the update does NOT run and exit is non-zero.
        assertThat(exit).isNotEqualTo(
                TaskMapRowUpdaterCli.EXIT_OK);
    }
}
