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
 *
 * <h2>Separation from the other two telemetry writers</h2>
 *
 * <p>Three NDJSON writers coexist in this code base and each
 * serves a distinct domain:</p>
 *
 * <ul>
 *   <li>{@link dev.iadev.telemetry.TelemetryWriter} —
 *       canonical writer for skill/phase/tool execution
 *       events (EPIC-0040,
 *       {@code plans/epic-*}/telemetry/events.ndjson}).
 *       Uses {@link dev.iadev.telemetry.TelemetryScrubber}
 *       (Rule 20) on every event to strip PII from
 *       free-form fields.</li>
 *   <li>{@link dev.iadev.infrastructure.adapter.output.telemetry.FileTelemetryWriter}
 *       (this package) — writes a completed
 *       {@link dev.iadev.domain.model.PhaseMetric} (version,
 *       phase, outcome, duration) to
 *       {@code plans/release-metrics.jsonl} under a
 *       cross-process {@code FileLock}.</li>
 *   <li>{@link dev.iadev.release.telemetry.ReleaseTelemetryWriter}
 *       — pure in-memory formatter (no I/O) that returns a
 *       subset-schema JSONL line used for pre-completion
 *       markers; the caller persists it to the same
 *       {@code release-metrics.jsonl} file read by
 *       {@link dev.iadev.infrastructure.adapter.output.telemetry.TelemetryJsonlReader}.</li>
 * </ul>
 *
 * <p>The {@link dev.iadev.domain.model.PhaseMetric} schema is
 * closed and typed (version string, enum phase, ISO timestamps,
 * enum outcome) with no free-form caller-supplied content, so
 * the Rule 20 scrubber is not required here (Rule 20 targets
 * {@link dev.iadev.telemetry.TelemetryEvent} streams).</p>
 */
package dev.iadev.infrastructure.adapter.output.telemetry;
