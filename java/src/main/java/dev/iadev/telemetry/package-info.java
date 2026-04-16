/**
 * Java domain for Claude Code telemetry (EPIC-0040).
 *
 * <p>This package owns the immutable event model
 * ({@link dev.iadev.telemetry.TelemetryEvent}), the discrete taxonomies
 * ({@link dev.iadev.telemetry.EventType},
 * {@link dev.iadev.telemetry.EventStatus}), and the NDJSON persistence
 * adapters ({@link dev.iadev.telemetry.TelemetryWriter},
 * {@link dev.iadev.telemetry.TelemetryReader}).</p>
 *
 * <p>All types mirror the canonical schema published by story-0040-0001
 * under {@code shared/templates/_TEMPLATE-TELEMETRY-EVENT.json}. The writer
 * guarantees single-line append-only semantics with a cross-process
 * {@code FileChannel.tryLock()} so hooks written in shell and Java analysis
 * skills can safely share the same {@code events.ndjson}.</p>
 */
package dev.iadev.telemetry;
