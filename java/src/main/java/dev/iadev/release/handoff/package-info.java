/**
 * Handoff orchestration for {@code x-release} halt points that
 * support {@code HANDOFF}, including {@code APPROVAL_GATE} and
 * {@code BACKMERGE_MERGE}.
 *
 * <p>When the prompt flow returns {@code HANDOFF} for a
 * supported halt point (operator chose "Rodar /x-pr-fix PR#"),
 * the
 * {@link dev.iadev.release.handoff.HandoffOrchestrator}
 * invokes the {@code x-pr-fix} skill (renamed by EPIC-0036) via the
 * {@link dev.iadev.release.handoff.SkillInvokerPort},
 * re-checks the PR state via the
 * {@link dev.iadev.release.handoff.GhCliPort}, and derives a
 * fresh option set for the next prompt according to the active
 * halt point's decision table (story-0039-0011 §5.3).
 *
 * <p>Introduced by EPIC-0039 story-0039-0011.
 */
package dev.iadev.release.handoff;
