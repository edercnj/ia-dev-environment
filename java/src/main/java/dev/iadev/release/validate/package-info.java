/**
 * Release VALIDATE-DEEP parallel check execution.
 *
 * <p>Provides a domain-pure executor that dispatches independent
 * validation checks in parallel and aggregates results sorted by
 * severity. Used by the {@code x-release} skill's VALIDATE-DEEP
 * phase to cut wall-clock time by running the 7 independent
 * post-build checks concurrently (see story-0039-0004).</p>
 *
 * <p>All {@code VALIDATE_*} error codes produced by the original
 * sequential flow are preserved (RULE-005). No dependencies on
 * framework or adapter code — belongs to the application layer
 * and is invoked only from orchestration skills.</p>
 */
package dev.iadev.release.validate;
