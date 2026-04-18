/**
 * Java domain for Claude Code execution telemetry (EPIC-0040).
 *
 * <p>This package owns the immutable event model
 * ({@link dev.iadev.telemetry.TelemetryEvent}), the discrete taxonomies
 * ({@link dev.iadev.telemetry.EventType},
 * {@link dev.iadev.telemetry.EventStatus}), the PII scrubber
 * ({@link dev.iadev.telemetry.TelemetryScrubber} — Rule 20), and the NDJSON
 * persistence adapters ({@link dev.iadev.telemetry.TelemetryWriter},
 * {@link dev.iadev.telemetry.TelemetryReader}).</p>
 *
 * <p>All types mirror the canonical schema published by story-0040-0001
 * under {@code shared/templates/_TEMPLATE-TELEMETRY-EVENT.json}. The writer
 * guarantees single-line append-only semantics with a cross-process
 * {@code FileChannel.tryLock()} so hooks written in shell and Java analysis
 * skills can safely share the same {@code events.ndjson}.</p>
 *
 * <h2>Separation from the two other telemetry writers in the code base</h2>
 *
 * <p>There are three NDJSON writers in this project; they are deliberately
 * distinct (different domains, different files, different schemas) — NOT
 * redundant:</p>
 *
 * <table>
 *   <caption>Telemetry writer separation of concerns</caption>
 *   <tr><th>Writer</th><th>Domain</th><th>File</th><th>Schema</th></tr>
 *   <tr>
 *     <td>{@link dev.iadev.telemetry.TelemetryWriter} (this package)</td>
 *     <td>Skill / phase / tool execution events (EPIC-0040)</td>
 *     <td>{@code plans/epic-*}/telemetry/events.ndjson}</td>
 *     <td>{@link dev.iadev.telemetry.TelemetryEvent}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code infrastructure.adapter.output.telemetry.FileTelemetryWriter}</td>
 *     <td>Release-phase metrics (story-0039-0012)</td>
 *     <td>{@code plans/release-metrics.jsonl}</td>
 *     <td>{@link dev.iadev.domain.model.PhaseMetric}</td>
 *   </tr>
 *   <tr>
 *     <td>{@code release.telemetry.ReleaseTelemetryWriter}</td>
 *     <td>Release-phase pre-completion markers (pure formatter, no I/O)</td>
 *     <td>(caller persists the returned line into {@code release-metrics.jsonl})</td>
 *     <td>Subset of {@link dev.iadev.domain.model.PhaseMetric} fields</td>
 *   </tr>
 * </table>
 *
 * <p>Rule 20 scrubbing applies only to {@link dev.iadev.telemetry.TelemetryEvent}
 * streams (which may carry free-form {@code failureReason} /
 * {@code metadata} values that can contain PII). The release-phase
 * writers operate on a closed, typed schema (version strings, enum phases,
 * ISO timestamps) with no free-form caller-supplied content, so they
 * do NOT require the scrubber.</p>
 */
package dev.iadev.telemetry;
