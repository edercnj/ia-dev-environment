package dev.iadev.domain.port.output;

import dev.iadev.domain.model.PhaseMetric;

/**
 * Output port for emitting release-phase telemetry records
 * (story-0039-0012). Implementations persist each metric
 * to an append-only JSONL store.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@link #emit(PhaseMetric)} MUST be non-blocking
 *       beyond a single append+flush.</li>
 *   <li>I/O failures MUST NOT propagate — the release
 *       flow continues. Implementations log a warning
 *       with the {@code TELEMETRY_WRITE_FAILED} code
 *       (§5.3).</li>
 *   <li>Implementations MUST be safe under concurrent
 *       invocation (file-lock semantics).</li>
 * </ul>
 */
public interface TelemetrySink {

    /**
     * Appends a single phase telemetry record to the
     * sink.
     *
     * @param metric the phase metric to emit; must not be
     *               {@code null}
     * @throws IllegalArgumentException if {@code metric}
     *         is {@code null}
     */
    void emit(PhaseMetric metric);
}
