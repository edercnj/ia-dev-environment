/**
 * Pure formatters for release / hotfix phase telemetry events
 * (story-0039-0012 §5.1 — original schema and writer; made
 * release-type aware in story-0039-0014).
 *
 * <p>No I/O — the writer builds the JSONL line, the caller is
 * responsible for persisting it.</p>
 */
package dev.iadev.release.telemetry;
