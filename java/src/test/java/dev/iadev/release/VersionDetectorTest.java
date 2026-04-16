package dev.iadev.release;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VersionDetector}
 * (story-0039-0014 TASK-003/004/005/006).
 */
@DisplayName("VersionDetector")
class VersionDetectorTest {

    @Nested
    @DisplayName("detectBump — hotfix rejects feat")
    class HotfixRejectsFeat {

        @Test
        @DisplayName("throws HOTFIX_INVALID_COMMITS when "
                + "feat present")
        void detectBump_hotfixRejectsFeat() {
            CommitCounts counts =
                    new CommitCounts(1, 1, 0, 0, 0);

            assertThatThrownBy(() ->
                    VersionDetector.detectBump(
                            counts,
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            HotfixInvalidCommitsException
                                    .class)
                    .satisfies(e -> assertThat(
                            ((HotfixInvalidCommitsException) e)
                                    .code())
                            .isEqualTo(
                                    "HOTFIX_INVALID_COMMITS"));
        }

        @Test
        @DisplayName("throws HOTFIX_INVALID_COMMITS when "
                + "breaking present")
        void detectBump_hotfixRejectsBreaking() {
            CommitCounts counts =
                    new CommitCounts(0, 1, 0, 1, 0);

            assertThatThrownBy(() ->
                    VersionDetector.detectBump(
                            counts,
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            HotfixInvalidCommitsException
                                    .class);
        }
    }

    @Nested
    @DisplayName("detectBump — hotfix accepts fix/perf")
    class HotfixAcceptsFixPerf {

        @Test
        @DisplayName("returns PATCH for fix-only commits")
        void detectBump_hotfixAcceptsFix() {
            CommitCounts counts =
                    new CommitCounts(0, 2, 0, 0, 0);

            BumpType bump = VersionDetector.detectBump(
                    counts, ReleaseContext.forHotfix());

            assertThat(bump).isEqualTo(BumpType.PATCH);
        }

        @Test
        @DisplayName("returns PATCH for perf-only commits")
        void detectBump_hotfixAcceptsPerf() {
            CommitCounts counts =
                    new CommitCounts(0, 0, 3, 0, 0);

            BumpType bump = VersionDetector.detectBump(
                    counts, ReleaseContext.forHotfix());

            assertThat(bump).isEqualTo(BumpType.PATCH);
        }

        @Test
        @DisplayName("throws VERSION_NO_BUMP_SIGNAL when "
                + "only ignored commits")
        void detectBump_hotfixNoBumpSignal() {
            CommitCounts counts =
                    new CommitCounts(0, 0, 0, 0, 5);

            assertThatThrownBy(() ->
                    VersionDetector.detectBump(
                            counts,
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            InvalidBumpException.class)
                    .satisfies(e -> assertThat(
                            ((InvalidBumpException) e)
                                    .code())
                            .isEqualTo(
                                    InvalidBumpException
                                            .Code
                                            .VERSION_NO_BUMP_SIGNAL));
        }
    }

    @Nested
    @DisplayName("detectBump — standard flow")
    class StandardFlow {

        @Test
        @DisplayName("release with feat returns MINOR")
        void detectBump_releaseFeatMinor() {
            CommitCounts counts =
                    new CommitCounts(1, 0, 0, 0, 0);

            BumpType bump = VersionDetector.detectBump(
                    counts, ReleaseContext.release());

            assertThat(bump).isEqualTo(BumpType.MINOR);
        }

        @Test
        @DisplayName("release with breaking returns MAJOR")
        void detectBump_releaseBreakingMajor() {
            CommitCounts counts =
                    new CommitCounts(0, 0, 0, 1, 0);

            BumpType bump = VersionDetector.detectBump(
                    counts, ReleaseContext.release());

            assertThat(bump).isEqualTo(BumpType.MAJOR);
        }

        @Test
        @DisplayName("release with only ignored throws "
                + "VERSION_NO_BUMP_SIGNAL")
        void detectBump_releaseNoSignal() {
            CommitCounts counts =
                    new CommitCounts(0, 0, 0, 0, 3);

            assertThatThrownBy(() ->
                    VersionDetector.detectBump(
                            counts,
                            ReleaseContext.release()))
                    .isInstanceOf(
                            InvalidBumpException.class);
        }
    }

    @Nested
    @DisplayName("validateOverride — hotfix PATCH guard")
    class HotfixOverrideGuard {

        @Test
        @DisplayName("rejects MINOR bump in hotfix")
        void validateOverride_rejectsMinor() {
            SemVer current = SemVer.parse("3.1.0");
            SemVer requested = SemVer.parse("3.2.0");

            assertThatThrownBy(() ->
                    VersionDetector.validateOverride(
                            current, requested,
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            HotfixVersionNotPatchException
                                    .class)
                    .satisfies(e -> assertThat(
                            ((HotfixVersionNotPatchException) e)
                                    .code())
                            .isEqualTo(
                                    "HOTFIX_VERSION_NOT_PATCH"));
        }

        @Test
        @DisplayName("rejects MAJOR bump in hotfix")
        void validateOverride_rejectsMajor() {
            SemVer current = SemVer.parse("3.1.0");
            SemVer requested = SemVer.parse("4.0.0");

            assertThatThrownBy(() ->
                    VersionDetector.validateOverride(
                            current, requested,
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            HotfixVersionNotPatchException
                                    .class);
        }

        @Test
        @DisplayName("rejects non-sequential PATCH")
        void validateOverride_rejectsNonSequentialPatch() {
            SemVer current = SemVer.parse("3.1.0");
            SemVer requested = SemVer.parse("3.1.5");

            assertThatThrownBy(() ->
                    VersionDetector.validateOverride(
                            current, requested,
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            HotfixVersionNotPatchException
                                    .class);
        }

        @Test
        @DisplayName("rejects pre-release in hotfix bump")
        void validateOverride_rejectsPreRelease() {
            SemVer current = SemVer.parse("3.1.0");
            SemVer requested = SemVer.parse("3.1.1-rc.1");

            assertThatThrownBy(() ->
                    VersionDetector.validateOverride(
                            current, requested,
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            HotfixVersionNotPatchException
                                    .class);
        }

        @Test
        @DisplayName("accepts valid PATCH bump")
        void validateOverride_acceptsPatch() {
            SemVer current = SemVer.parse("3.1.0");
            SemVer requested = SemVer.parse("3.1.1");

            SemVer result = VersionDetector
                    .validateOverride(
                            current, requested,
                            ReleaseContext.forHotfix());

            assertThat(result).isEqualTo(requested);
        }

        @Test
        @DisplayName("standard flow accepts any override")
        void validateOverride_standardAnyBump() {
            SemVer current = SemVer.parse("3.1.0");
            SemVer requested = SemVer.parse("4.0.0");

            SemVer result = VersionDetector
                    .validateOverride(
                            current, requested,
                            ReleaseContext.release());

            assertThat(result).isEqualTo(requested);
        }
    }
}
