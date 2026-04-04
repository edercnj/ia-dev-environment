package dev.iadev.infrastructure.adapter.output.progress;

import dev.iadev.domain.port.output.ProgressReporter;

/**
 * Console-based implementation of {@link ProgressReporter}.
 *
 * <p>Writes progress messages to {@link System#out} and error
 * messages to {@link System#err}. Thread-safe by virtue of
 * delegating to synchronized {@code PrintStream} methods.</p>
 *
 * <p>Message format:</p>
 * <ul>
 *   <li>Start: {@code [START] taskName (N steps)}</li>
 *   <li>Progress: {@code [N] taskName: message}</li>
 *   <li>Complete: {@code [DONE] taskName}</li>
 *   <li>Error: {@code [ERROR] taskName: errorMessage}</li>
 * </ul>
 */
public final class ConsoleProgressReporter
        implements ProgressReporter {

    @Override
    public void reportStart(
            String taskName, int totalSteps) {
        System.out.printf(
                "[START] %s (%d steps)%n",
                taskName, totalSteps);
    }

    @Override
    public void reportProgress(
            String taskName,
            int currentStep,
            String message) {
        System.out.printf(
                "[%d] %s: %s%n",
                currentStep, taskName, message);
    }

    @Override
    public void reportComplete(String taskName) {
        System.out.printf("[DONE] %s%n", taskName);
    }

    @Override
    public void reportError(
            String taskName, String errorMessage) {
        System.err.printf(
                "[ERROR] %s: %s%n",
                taskName, errorMessage);
    }
}
