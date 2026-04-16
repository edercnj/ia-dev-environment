package dev.iadev.infrastructure.adapter.output.telemetry;

import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.port.output.TelemetrySink;

/**
 * {@link TelemetrySink} implementation that discards every
 * metric — used when the operator passes
 * {@code --telemetry off} (story-0039-0012 §3.4).
 */
public final class NoopTelemetrySink
        implements TelemetrySink {

    @Override
    public void emit(PhaseMetric metric) {
        if (metric == null) {
            throw new IllegalArgumentException(
                    "metric must not be null");
        }
        // intentional no-op: --telemetry off
    }
}
