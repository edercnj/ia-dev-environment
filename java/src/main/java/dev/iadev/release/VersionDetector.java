package dev.iadev.release;

import java.util.Objects;

/**
 * Pure-domain version detector. Derives the next release
 * version from classified {@link CommitCounts} and a
 * {@link ReleaseContext} (story-0039-0014 TASK-003/004/005/006).
 *
 * <p>Detector rules:</p>
 * <ul>
 *   <li>{@link BumpRestriction#ANY} — delegates to
 *       {@link BumpType#from(CommitCounts)} (standard flow).</li>
 *   <li>{@link BumpRestriction#PATCH_ONLY} — any {@code feat}
 *       or breaking signal triggers
 *       {@link HotfixInvalidCommitsException}; only fix/perf
 *       are accepted and yield {@link BumpType#PATCH}. Zero
 *       qualifying commits yield
 *       {@link InvalidBumpException.Code#VERSION_NO_BUMP_SIGNAL}.</li>
 * </ul>
 *
 * <p>Explicit version overrides pass through
 * {@link #validateOverride(SemVer, SemVer, ReleaseContext)}
 * which enforces the PATCH-only invariant for hotfix.</p>
 *
 * <p>Pure computation — no I/O, no logging in the detector
 * itself (Rule 06 / OWASP A09).</p>
 */
public final class VersionDetector {

    private VersionDetector() {
        // utility class
    }

    /**
     * Detects the bump implied by {@code counts} under the
     * rules of {@code ctx}.
     *
     * @throws HotfixInvalidCommitsException if {@code ctx}
     *         restricts to PATCH_ONLY and {@code counts}
     *         contains any feat or breaking
     * @throws InvalidBumpException with code
     *         {@link InvalidBumpException.Code#VERSION_NO_BUMP_SIGNAL}
     *         when no qualifying commit is present
     */
    public static BumpType detectBump(
            CommitCounts counts, ReleaseContext ctx) {
        Objects.requireNonNull(counts, "counts");
        Objects.requireNonNull(ctx, "ctx");

        if (ctx.restrictBumpTo()
                == BumpRestriction.PATCH_ONLY) {
            rejectNonPatchSignals(counts);
            if (counts.fix() + counts.perf() == 0) {
                throw new InvalidBumpException(
                        InvalidBumpException.Code
                                .VERSION_NO_BUMP_SIGNAL,
                        "Hotfix requires at least one "
                                + "fix or perf commit");
            }
            return BumpType.PATCH;
        }

        return BumpType.from(counts)
                .orElseThrow(() -> new InvalidBumpException(
                        InvalidBumpException.Code
                                .VERSION_NO_BUMP_SIGNAL,
                        "No commit justifies a release bump"));
    }

    /**
     * Validates an explicit {@code --version} override
     * against the rules of {@code ctx}. Returns the requested
     * version on success.
     *
     * @throws HotfixVersionNotPatchException if {@code ctx}
     *         restricts to PATCH_ONLY and {@code requested}
     *         is not {@code current} with {@code patch + 1}
     */
    public static SemVer validateOverride(
            SemVer current,
            SemVer requested,
            ReleaseContext ctx) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(ctx, "ctx");

        if (ctx.restrictBumpTo()
                == BumpRestriction.PATCH_ONLY
                && !isPatchBump(current, requested)) {
            throw new HotfixVersionNotPatchException(
                    current, requested);
        }
        return requested;
    }

    private static void rejectNonPatchSignals(
            CommitCounts counts) {
        if (counts.feat() > 0 || counts.breaking() > 0) {
            throw new HotfixInvalidCommitsException(
                    counts.feat(), counts.breaking());
        }
    }

    private static boolean isPatchBump(
            SemVer current, SemVer requested) {
        return current.major() == requested.major()
                && current.minor() == requested.minor()
                && requested.patch() == current.patch() + 1
                && requested.preRelease() == null;
    }
}
