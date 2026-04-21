package dev.iadev.cli;

import dev.iadev.application.lifecycle.StatusSyncException;
import dev.iadev.application.lifecycle.TaskMapRowUpdater;
import dev.iadev.domain.lifecycle.LifecycleStatus;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Picocli command: {@code task-map-row-update <map-file>
 * <TASK-ID> <new-status>}.
 *
 * <p>Thin CLI wrapper around
 * {@link TaskMapRowUpdater#updateRow} so that skills (notably
 * {@code x-task-implement} Phase 3.5) can invoke the helper
 * without a JVM-aware shell. Story-0046-0003 /
 * TASK-0046-0003-002.</p>
 *
 * <p>Exit codes (aligned with the lifecycle CLI family,
 * story-0046-0003 §DoD):</p>
 * <ul>
 *   <li>{@code 0} — update applied (or idempotent no-op).</li>
 *   <li>{@code 20} — {@code STATUS_SYNC_FAILED}: row absent,
 *       file I/O error, atomic rename failed.</li>
 *   <li>{@code 40} — invalid arguments (missing, blank, or
 *       unknown status label).</li>
 * </ul>
 */
@Command(
        name = "task-map-row-update",
        mixinStandardHelpOptions = true,
        description = "Update the Status column of a row in a "
                + "task-implementation-map-STORY-*.md file.")
public final class TaskMapRowUpdaterCli
        implements Callable<Integer> {

    /** Exit code: update applied or idempotent no-op. */
    public static final int EXIT_OK = 0;

    /** Exit code: STATUS_SYNC_FAILED (I/O, missing row). */
    public static final int EXIT_STATUS_SYNC_FAILED = 20;

    /** Exit code: invalid arguments. */
    public static final int EXIT_INVALID_ARGS = 40;

    @Parameters(index = "0",
            description = "Path to the "
                    + "task-implementation-map-STORY-*.md "
                    + "file.")
    Path mapFile;

    @Parameters(index = "1",
            description = "Canonical TASK-ID "
                    + "(TASK-XXXX-YYYY-NNN).")
    String taskId;

    @Parameters(index = "2",
            description = "New status label. One of: "
                    + "Pendente, Planejada, Em Andamento, "
                    + "Concluída, Falha, Bloqueada.")
    String newStatusLabel;

    @Override
    public Integer call() {
        if (mapFile == null || taskId == null
                || newStatusLabel == null
                || taskId.isBlank()
                || newStatusLabel.isBlank()) {
            System.err.println(
                    "INVALID_ARGS: mapFile, taskId, and "
                            + "newStatus are required.");
            return EXIT_INVALID_ARGS;
        }
        Optional<LifecycleStatus> parsed =
                LifecycleStatus.fromLabel(newStatusLabel);
        if (parsed.isEmpty()) {
            System.err.println(
                    "INVALID_ARGS: unknown status label '"
                            + newStatusLabel + "'.");
            return EXIT_INVALID_ARGS;
        }
        try {
            TaskMapRowUpdater.updateRow(
                    mapFile, taskId, parsed.get());
            return EXIT_OK;
        } catch (StatusSyncException e) {
            System.err.println(e.getMessage());
            return EXIT_STATUS_SYNC_FAILED;
        } catch (RuntimeException e) {
            System.err.println(
                    "STATUS_SYNC_FAILED: unexpected error "
                            + "while updating task map row: "
                            + e.getMessage());
            e.printStackTrace(System.err);
            return EXIT_STATUS_SYNC_FAILED;
        }
    }

    /**
     * Entry point for direct CLI invocation.
     */
    public static void main(String[] args) {
        CommandLine commandLine =
                new CommandLine(new TaskMapRowUpdaterCli());
        commandLine.setParameterExceptionHandler(
                (ex, parseResult) -> {
                    CommandLine cmd = ex.getCommandLine();
                    cmd.getErr().println(ex.getMessage());
                    cmd.usage(cmd.getErr());
                    return EXIT_INVALID_ARGS;
                });
        int exit = commandLine.execute(args);
        System.exit(exit);
    }
}
