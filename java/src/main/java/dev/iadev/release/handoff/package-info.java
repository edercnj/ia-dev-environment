/**
 * Handoff orchestration for the {@code x-release}
 * APPROVAL_GATE halt point.
 *
 * <p>When the operator chooses "Rodar /x-pr-fix PR#" at the
 * APPROVAL_GATE, the
 * {@link dev.iadev.release.handoff.HandoffOrchestrator}
 * invokes the {@code x-pr-fix} skill (formerly
 * {@code x-pr-fix-comments}; renamed by EPIC-0036) via the
 * {@link dev.iadev.release.handoff.SkillInvokerPort},
 * re-checks the PR state via the
 * {@link dev.iadev.release.handoff.GhCliPort}, and derives a
 * fresh option set for the next prompt based on the decision
 * table in story-0039-0011 §5.3.
 *
 * <p>Introduced by EPIC-0039 story-0039-0011.
 */
package dev.iadev.release.handoff;
