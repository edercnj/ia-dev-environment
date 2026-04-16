package dev.iadev.release.dryrun;

import java.util.List;

/**
 * Default implementation of {@link PhaseCatalogPort}
 * exposing the canonical 13 phases documented in the
 * {@code x-release} skill (story-0039-0013 §3.1).
 *
 * <p>Each phase declares the commands that WOULD be
 * executed; the executor only previews them, never runs
 * them.
 */
public final class DefaultPhaseCatalog
        implements PhaseCatalogPort {

    private static final List<PhaseDescriptor> PHASES = List.of(
            new PhaseDescriptor("INITIALIZED", List.of(
                    "detect state file",
                    "verify gh/jq dependencies")),
            new PhaseDescriptor("DETERMINED", List.of(
                    "resolve bump type",
                    "compute target version")),
            new PhaseDescriptor("VALIDATED", List.of(
                    "mvn clean verify -Pall-tests",
                    "parse coverage report",
                    "golden file tests",
                    "hardcoded version scan",
                    "cross-file version consistency")),
            new PhaseDescriptor("BRANCHED", List.of(
                    "x-git-worktree create",
                    "git checkout -b release/X.Y.Z")),
            new PhaseDescriptor("UPDATED", List.of(
                    "update pom.xml version")),
            new PhaseDescriptor("CHANGELOG_WRITTEN", List.of(
                    "x-release-changelog")),
            new PhaseDescriptor("COMMITTED", List.of(
                    "git add pom.xml CHANGELOG.md",
                    "git commit -m 'release: vX.Y.Z'")),
            new PhaseDescriptor("PR_OPENED", List.of(
                    "git push origin release/X.Y.Z",
                    "gh pr create --base main")),
            new PhaseDescriptor("APPROVAL_PENDING", List.of(
                    "persist waitingFor=PR_MERGE",
                    "halt for operator merge")),
            new PhaseDescriptor("TAGGED", List.of(
                    "gh pr view --json state",
                    "git checkout main",
                    "git tag -a vX.Y.Z",
                    "git push origin vX.Y.Z")),
            new PhaseDescriptor("BACK_MERGED", List.of(
                    "git checkout -b chore/backmerge",
                    "git merge --no-ff origin/main",
                    "gh pr create --base develop")),
            new PhaseDescriptor("PUBLISHED", List.of(
                    "gh release create vX.Y.Z")),
            new PhaseDescriptor("CLEANED", List.of(
                    "git branch -D release/X.Y.Z",
                    "git push --delete origin release/X.Y.Z",
                    "delete state file"))
    );

    @Override
    public List<PhaseDescriptor> phases() {
        return PHASES;
    }
}
