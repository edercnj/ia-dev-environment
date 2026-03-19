package dev.iadev.cli;

/**
 * Thrown when the user cancels the interactive generation flow.
 *
 * <p>This is raised when the user presses Ctrl+C during any prompt or
 * answers "no" to the final confirmation. The process should exit with
 * code 1 and no files should be generated.</p>
 */
public class GenerationCancelledException extends RuntimeException {

    /**
     * Creates a cancellation exception with the given message.
     *
     * @param message description of the cancellation reason
     */
    public GenerationCancelledException(String message) {
        super(message);
    }
}
