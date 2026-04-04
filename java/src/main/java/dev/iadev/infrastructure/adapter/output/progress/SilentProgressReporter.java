package dev.iadev.infrastructure.adapter.output.progress;

import dev.iadev.domain.port.output.ProgressReporter;

/**
 * No-op implementation of {@link ProgressReporter}.
 *
 * <p>All methods are empty — no output is produced. Useful for
 * unit tests, CI/CD pipelines, and any context where progress
 * output should be suppressed.</p>
 *
 * <p>Thread-safe: all methods are stateless no-ops.</p>
 */
public final class SilentProgressReporter
        implements ProgressReporter {

    @Override
    public void reportStart(
            String taskName, int totalSteps) {
        // intentionally empty — silent reporter
    }

    @Override
    public void reportProgress(
            String taskName,
            int currentStep,
            String message) {
        // intentionally empty — silent reporter
    }

    @Override
    public void reportComplete(String taskName) {
        // intentionally empty — silent reporter
    }

    @Override
    public void reportError(
            String taskName, String errorMessage) {
        // intentionally empty — silent reporter
    }
}
