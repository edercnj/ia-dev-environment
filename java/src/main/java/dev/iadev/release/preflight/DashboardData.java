package dev.iadev.release.preflight;

import dev.iadev.release.BumpType;
import dev.iadev.release.CommitCounts;
import dev.iadev.release.SemVer;
import dev.iadev.release.integrity.IntegrityReport;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable data bag carrying all inputs for the pre-flight dashboard.
 *
 * <p>Assembler or adapter code constructs this record from S01 (version
 * detection) and S03 (integrity checks) outputs. The renderer reads it
 * purely — no I/O, no side-effects.</p>
 *
 * @param targetVersion     resolved next release version (never null)
 * @param previousVersion   last tag version; empty for first-ever release
 * @param lastTagAgeDays    calendar days since last tag (0 when first release)
 * @param commitCounts      Conventional Commits classification (never null)
 * @param bumpType          auto-detected or explicit bump type (never null)
 * @param changelogLines    lines from the {@code [Unreleased]} section (never null)
 * @param integrityReport   aggregated integrity report (never null)
 * @param baseBranch        branch the release will be cut from (e.g. "develop")
 */
public record DashboardData(
        SemVer targetVersion,
        Optional<SemVer> previousVersion,
        long lastTagAgeDays,
        CommitCounts commitCounts,
        BumpType bumpType,
        List<String> changelogLines,
        IntegrityReport integrityReport,
        String baseBranch) {

    public DashboardData {
        Objects.requireNonNull(targetVersion, "targetVersion");
        Objects.requireNonNull(previousVersion, "previousVersion");
        Objects.requireNonNull(commitCounts, "commitCounts");
        Objects.requireNonNull(bumpType, "bumpType");
        Objects.requireNonNull(changelogLines, "changelogLines");
        Objects.requireNonNull(integrityReport, "integrityReport");
        Objects.requireNonNull(baseBranch, "baseBranch");
        changelogLines = List.copyOf(changelogLines);
    }
}
