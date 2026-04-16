package dev.iadev.release.abort;

/**
 * Port for user confirmation prompts during release abort.
 *
 * <p>Application layer abstraction — the real implementation
 * uses {@code AskUserQuestion}; tests use a fake returning
 * deterministic answers (story-0039-0010 §TASK-008
 * escalation note).
 */
public interface ConfirmationPort {

    /**
     * Asks the user a yes/no confirmation question.
     *
     * @param message the confirmation prompt to display
     * @return {@code true} if the user confirmed, {@code false}
     *         if the user cancelled
     */
    boolean confirm(String message);
}
