/**
 * File-based driven adapter implementing
 * {@link dev.iadev.domain.port.output.TelemetrySink} for
 * release-phase telemetry (story-0039-0012).
 *
 * <p>RULE-001 (hexagonal): this package is an outbound
 * adapter — it depends on the domain port
 * ({@code domain.port.output.TelemetrySink}) and the
 * domain model ({@code domain.model.PhaseMetric}), but
 * never the other direction. The pure benchmark
 * computation lives in {@code domain.telemetry}.
 */
package dev.iadev.infrastructure.adapter.output.telemetry;
