package dev.iadev.release;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage-driven tests for {@link HotfixInvalidCommitsException}
 * and {@link HotfixVersionNotPatchException} accessors and
 * input-validation branches (story-0039-0014 TASK-013).
 */
@DisplayName("Hotfix exception accessors")
class HotfixExceptionCoverageTest {

    @Test
    @DisplayName("HotfixInvalidCommits exposes featCount")
    void invalidCommits_featCount() {
        HotfixInvalidCommitsException e =
                new HotfixInvalidCommitsException(3, 1);

        assertThat(e.featCount()).isEqualTo(3);
        assertThat(e.breakingCount()).isEqualTo(1);
        assertThat(e.code())
                .isEqualTo("HOTFIX_INVALID_COMMITS");
        assertThat(e.getMessage())
                .contains("feat=3")
                .contains("breaking=1");
    }

    @Test
    @DisplayName("HotfixInvalidCommits rejects negative "
            + "counts")
    void invalidCommits_rejectsNegativeFeat() {
        assertThatThrownBy(() ->
                new HotfixInvalidCommitsException(-1, 0))
                .isInstanceOf(
                        IllegalArgumentException.class);
    }

    @Test
    @DisplayName("HotfixInvalidCommits rejects negative "
            + "breaking count")
    void invalidCommits_rejectsNegativeBreaking() {
        assertThatThrownBy(() ->
                new HotfixInvalidCommitsException(0, -1))
                .isInstanceOf(
                        IllegalArgumentException.class);
    }

    @Test
    @DisplayName("HotfixVersionNotPatch exposes current/"
            + "requested")
    void versionNotPatch_accessors() {
        SemVer current = SemVer.parse("3.1.0");
        SemVer requested = SemVer.parse("3.2.0");

        HotfixVersionNotPatchException e =
                new HotfixVersionNotPatchException(
                        current, requested);

        assertThat(e.current()).isEqualTo(current);
        assertThat(e.requested()).isEqualTo(requested);
        assertThat(e.code())
                .isEqualTo("HOTFIX_VERSION_NOT_PATCH");
        assertThat(e.getMessage())
                .contains("current=3.1.0")
                .contains("requested=3.2.0");
    }

    @Test
    @DisplayName("HotfixVersionNotPatch rejects null "
            + "current")
    void versionNotPatch_rejectsNullCurrent() {
        assertThatThrownBy(() ->
                new HotfixVersionNotPatchException(
                        null, SemVer.parse("3.1.1")))
                .isInstanceOf(
                        NullPointerException.class);
    }

    @Test
    @DisplayName("HotfixVersionNotPatch rejects null "
            + "requested")
    void versionNotPatch_rejectsNullRequested() {
        assertThatThrownBy(() ->
                new HotfixVersionNotPatchException(
                        SemVer.parse("3.1.0"), null))
                .isInstanceOf(
                        NullPointerException.class);
    }
}
