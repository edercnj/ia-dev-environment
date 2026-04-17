/**
 * Pure formatters for release / hotfix phase telemetry events
 * (story-0039-0012 §5.1 — original schema and writer; made
 * release-type aware in story-0039-0014).
 *
 * <p>No I/O — the writer builds the JSONL line, the caller is
 * responsible for persisting it.</p>
 *
 * <h2>Separation from the other two telemetry writers</h2>
 *
 * <p>This package intentionally does NOT overlap with the two sibling
 * NDJSON writers. The separation is:</p>
 *
 * <ul>
 *   <li>{@link dev.iadev.telemetry.TelemetryWriter} — canonical
 *       NDJSON writer for skill/phase/tool execution events
 *       (EPIC-0040, {@code plans/epic-*}/telemetry/events.ndjson}).
 *       Carries free-form fields subject to
 *       {@link dev.iadev.telemetry.TelemetryScrubber} (Rule 20).</li>
 *   <li>{@link dev.iadev.infrastructure.adapter.output.telemetry.FileTelemetryWriter}
 *       — adapter-layer {@code TelemetrySink} implementation that
 *       persists a full {@link dev.iadev.domain.model.PhaseMetric}
 *       (with {@code endedAt}, {@code durationSec}, and
 *       {@code outcome}) to {@code plans/release-metrics.jsonl}
 *       under a cross-process {@code FileLock}.</li>
 *   <li>{@link dev.iadev.release.telemetry.ReleaseTelemetryWriter}
 *       (this package) — pure, synchronous formatter that returns
 *       the JSONL string for an <em>early-phase, pre-completion</em>
 *       marker (subset of the {@code PhaseMetric} schema). Used by
 *       the interactive release/hotfix flow to emit a marker BEFORE
 *       the phase completes, when {@code endedAt} / {@code outcome}
 *       are not yet known. The caller (not this class) is
 *       responsible for appending the returned line to the same
 *       {@code plans/release-metrics.jsonl} file consumed by
 *       {@link dev.iadev.infrastructure.adapter.output.telemetry.TelemetryJsonlReader}.</li>
 * </ul>
 *
 * <p>Rule 20 (telemetry PII scrubbing) targets
 * {@link dev.iadev.telemetry.TelemetryEvent} streams exclusively —
 * this formatter handles a closed, typed schema (version strings,
 * enum phases, ISO timestamps) with no free-form PII surface.</p>
 */
package dev.iadev.release.telemetry;
