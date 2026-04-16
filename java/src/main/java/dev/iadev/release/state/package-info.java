/**
 * Release-state schema types for the {@code x-release} skill's
 * durable state file ({@code plans/release-state-X.Y.Z.json}).
 *
 * <p>This package owns the {@code schemaVersion: 2} contract
 * introduced by EPIC-0039 story-0039-0002. It provides:
 * <ul>
 *   <li>{@link dev.iadev.release.state.ReleaseState} — canonical
 *       immutable record carrying every field of the persisted
 *       state file, Jackson-annotated for serialization.</li>
 *   <li>{@link dev.iadev.release.state.NextAction} — suggested
 *       operator action pair {@code (label, command)} used for
 *       interactive prompts.</li>
 *   <li>{@link dev.iadev.release.state.WaitingFor} — enum of the
 *       halt modes the skill can be paused in.</li>
 *   <li>{@link dev.iadev.release.state.StateFileValidator} —
 *       validator enforcing RULE-003 (no silent upgrade): only
 *       {@code schemaVersion == 2} is accepted; v1 is rejected
 *       with a migration-guiding error code.</li>
 * </ul>
 *
 * <p>This package sits outside {@code dev.iadev.domain} on purpose
 * — RULE-004 (Domain Purity) forbids Jackson in the domain layer.
 * The state-file contract is a cross-cutting persistence schema
 * used by the ops adapter {@code x-release}, so it lives in its
 * own top-level package peer to {@code domain}.
 *
 * @see <a href="../../../../resources/targets/claude/skills/core/ops/x-release/references/state-file-schema.md">state-file-schema.md</a>
 */
package dev.iadev.release.state;
