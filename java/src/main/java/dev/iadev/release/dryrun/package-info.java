/**
 * Interactive dry-run mode for the {@code /x-release}
 * skill (story-0039-0013).
 *
 * <p>This package hosts the {@code --dry-run --interactive}
 * sub-modality: a use case ({@link
 * dev.iadev.release.dryrun.DryRunInteractiveExecutor})
 * that loops over a 13-phase catalog
 * ({@link dev.iadev.release.dryrun.DefaultPhaseCatalog}),
 * pausing before each phase via
 * {@link dev.iadev.release.dryrun.PromptPort} and
 * recording outcomes without invoking any real
 * {@code git}, {@code mvn}, or {@code gh} command.
 *
 * <p>Side-effect isolation is enforced via ports: the
 * only filesystem write is a dummy state file created
 * with owner-only POSIX permissions via
 * {@link dev.iadev.release.dryrun.TempFileDryRunStateWriter},
 * always cleaned up in a {@code finally} block
 * (RULE-006, CWE-22 defensive temp-file creation).
 *
 * <p>Cross-cutting rules enforced: RULE-001 (source of
 * truth is the SKILL.md generator under
 * {@code resources/targets/claude/}) and RULE-004
 * (prompts have a non-interactive equivalent — the
 * {@link dev.iadev.release.dryrun.PromptPort} abstraction
 * lets CI wire a scripted responder).
 */
package dev.iadev.release.dryrun;
