package dev.iadev.release.resume;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Immutable value object representing a detected in-flight
 * release state file. Carries the fields needed by the smart
 * resume prompt (version, phase, age, previous version) without
 * exposing the full {@code ReleaseState} record.
 *
 * @param version         the release version (e.g. "3.2.0")
 * @param phase           the current phase (e.g. "APPROVAL_PENDING")
 * @param previousVersion the base tag (e.g. "v3.1.0")
 * @param staleDuration   time since {@code lastPhaseCompletedAt}
 * @param stateFilePath   path to the state file (absolute or relative)
 */
public record DetectedState(
        String version,
        String phase,
        String previousVersion,
        Duration staleDuration,
        Path stateFilePath) {
}
