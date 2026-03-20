package dev.iadev.exception;

/**
 * Thrown when the user cancels the generation process
 * (e.g., interactive prompt abort via Ctrl+C or confirmation denial).
 *
 * <p>Application-level exception — not CLI-specific.
 * The process should exit with code 1 and no files should be
 * generated.</p>
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
