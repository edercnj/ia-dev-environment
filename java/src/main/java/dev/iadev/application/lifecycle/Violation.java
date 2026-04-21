package dev.iadev.application.lifecycle;

import java.nio.file.Path;

/**
 * A single lifecycle-integrity violation surfaced by
 * {@link LifecycleAuditRunner}. The three audit dimensions
 * (per Rule 22 and story-0046-0007) are:
 *
 * <ul>
 *   <li>{@code ORPHAN_PHASE_MARKER} — a {@code phase.start}
 *       marker without a matching {@code phase.end} in the
 *       same skill.</li>
 *   <li>{@code REPORT_WITHOUT_COMMIT} — a planning or
 *       implementation report was written but no corresponding
 *       git commit was produced.</li>
 *   <li>{@code SKIP_FLAG_ON_HAPPY_PATH} — a production skill
 *       references a {@code --skip-*} flag on the happy path,
 *       violating RULE-046-04.</li>
 * </ul>
 *
 * <p>The skeleton lives here; real detection is implemented
 * in story-0046-0007 (EPIC-0046 Layer 2).</p>
 */
public record Violation(
        String dimension,
        Path file,
        int line,
        String detail) {
}
