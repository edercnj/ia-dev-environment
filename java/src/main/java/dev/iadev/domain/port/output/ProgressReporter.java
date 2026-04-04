package dev.iadev.domain.port.output;

/**
 * Output port for reporting execution progress to the user.
 *
 * <p>Abstracts the progress reporting mechanism (console output,
 * progress bars, logging, etc.). The domain depends on this
 * interface; concrete implementations reside in the infrastructure
 * adapter layer.</p>
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #reportStart(String, int)} MUST be called before
 *       any {@link #reportProgress(String, int, String)} calls
 *       for the same task.</li>
 *   <li>{@link #reportComplete(String)} and
 *       {@link #reportError(String, String)} are terminal — no
 *       further progress calls should be made for that task.</li>
 *   <li>All methods MUST be safe to call from any thread.</li>
 * </ul>
 *
 * <h2>Pre-conditions</h2>
 * <ul>
 *   <li>{@code taskName} must not be null or blank.</li>
 *   <li>{@code totalSteps} must be greater than zero.</li>
 *   <li>{@code currentStep} must be between 1 and totalSteps
 *       (inclusive).</li>
 *   <li>{@code message} and {@code errorMessage} must not be null.</li>
 * </ul>
 *
 * <h2>Post-conditions</h2>
 * <ul>
 *   <li>Progress information is delivered to the configured output
 *       channel.</li>
 * </ul>
 *
 * <h2>Exceptions</h2>
 * <ul>
 *   <li>{@link IllegalArgumentException} if parameters violate
 *       pre-conditions.</li>
 * </ul>
 */
public interface ProgressReporter {

    /**
     * Reports the start of a new task with a known number of steps.
     *
     * @param taskName   the human-readable task identifier
     * @param totalSteps the total number of steps in this task
     * @throws IllegalArgumentException if taskName is null/blank
     *                                  or totalSteps is not positive
     */
    void reportStart(String taskName, int totalSteps);

    /**
     * Reports incremental progress within a task.
     *
     * @param taskName    the task identifier (must match a prior start)
     * @param currentStep the current step number (1-based)
     * @param message     a description of the current step
     * @throws IllegalArgumentException if any parameter is null/blank
     *                                  or currentStep is out of range
     */
    void reportProgress(
            String taskName, int currentStep, String message);

    /**
     * Reports successful completion of a task.
     *
     * @param taskName the task identifier
     * @throws IllegalArgumentException if taskName is null or blank
     */
    void reportComplete(String taskName);

    /**
     * Reports a task failure with an error description.
     *
     * @param taskName     the task identifier
     * @param errorMessage a description of what went wrong
     * @throws IllegalArgumentException if any parameter is null/blank
     */
    void reportError(String taskName, String errorMessage);
}
