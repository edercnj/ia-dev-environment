package dev.iadev.release.summary;

import dev.iadev.release.ReleaseContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SummaryRenderer}
 * (story-0039-0014 TASK-009).
 */
@DisplayName("SummaryRenderer")
class SummaryRendererTest {

    @Nested
    @DisplayName("hotfix variant")
    class HotfixVariant {

        @Test
        @DisplayName("diagram shows hotfix/X.Y.Z branch "
                + "from main")
        void render_showsHotfixBranch() {
            String summary = SummaryRenderer.render(
                    "3.1.0", "3.1.1", 42,
                    ReleaseContext.forHotfix());

            assertThat(summary)
                    .contains("hotfix/3.1.1")
                    .contains("main:")
                    .contains("PR #42 merged");
        }

        @Test
        @DisplayName("hotfix diagram contains "
                + "back-merge arrow to develop")
        void render_hotfixBackMergeDevelop() {
            String summary = SummaryRenderer.render(
                    "3.1.0", "3.1.1", 42,
                    ReleaseContext.forHotfix());

            assertThat(summary)
                    .contains("back-merge")
                    .contains("develop:");
        }
    }

    @Nested
    @DisplayName("standard release variant")
    class StandardVariant {

        @Test
        @DisplayName("diagram shows release/X.Y.Z")
        void render_showsReleaseBranch() {
            String summary = SummaryRenderer.render(
                    "3.1.0", "3.2.0", 99,
                    ReleaseContext.release());

            assertThat(summary)
                    .contains("release/3.2.0")
                    .doesNotContain("hotfix/");
        }
    }

    @Nested
    @DisplayName("input validation")
    class InputValidation {

        @Test
        @DisplayName("rejects null version")
        void render_rejectsNullVersion() {
            assertThatThrownBy(() -> SummaryRenderer.render(
                    null, "3.1.1", 0,
                    ReleaseContext.forHotfix()))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects negative PR number")
        void render_rejectsNegativePr() {
            assertThatThrownBy(() -> SummaryRenderer.render(
                    "3.1.0", "3.1.1", -1,
                    ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            IllegalArgumentException.class);
        }

        @Test
        @DisplayName("sanitises ANSI escape sequences in "
                + "version")
        void render_sanitisesAnsi() {
            String poisoned = "3.1.1\u001b[31m";

            String summary = SummaryRenderer.render(
                    "3.1.0", poisoned, 1,
                    ReleaseContext.forHotfix());

            assertThat(summary).doesNotContain("\u001b");
        }
    }
}
