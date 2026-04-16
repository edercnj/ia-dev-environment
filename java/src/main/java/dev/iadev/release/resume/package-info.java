/**
 * Smart resume support for the {@code x-release} skill.
 *
 * <p>This package provides detection of in-flight release
 * state files and the decision logic for the smart resume
 * prompt introduced by story-0039-0008. It replaces the
 * hard {@code STATE_CONFLICT} abort with an interactive
 * 3-option prompt (resume / abort / start new) when a
 * non-completed state file is found.
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link dev.iadev.release.resume.StateFileDetector}
 *       — scans {@code plans/} for active state files and
 *       calculates stale duration.</li>
 *   <li>{@link dev.iadev.release.resume.SmartResumeOrchestrator}
 *       — decision logic: no-prompt → STATE_CONFLICT (legacy),
 *       interactive → PROMPT_USER with options.</li>
 *   <li>{@link dev.iadev.release.resume.DetectedState}
 *       — immutable value object for detected state metadata.</li>
 *   <li>{@link dev.iadev.release.resume.ResumeDecision}
 *       — immutable result of the resolution with action
 *       and available options.</li>
 * </ul>
 *
 * @see <a href="../../../../resources/targets/claude/skills/core/ops/x-release/SKILL.md">x-release SKILL.md</a>
 */
package dev.iadev.release.resume;
