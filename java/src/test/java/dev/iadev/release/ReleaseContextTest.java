package dev.iadev.release;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReleaseContext} and
 * {@link BumpRestriction} (story-0039-0014 TASK-001/002).
 */
@DisplayName("ReleaseContext")
class ReleaseContextTest {

    @Nested
    @DisplayName("factory — release()")
    class ReleaseFactory {

        @Test
        @DisplayName("returns ANY / develop / hotfix=false")
        void release_returnsStandardDefaults() {
            ReleaseContext ctx = ReleaseContext.release();

            assertThat(ctx.restrictBumpTo())
                    .isEqualTo(BumpRestriction.ANY);
            assertThat(ctx.baseBranch()).isEqualTo("develop");
            assertThat(ctx.hotfix()).isFalse();
        }

        @Test
        @DisplayName("is cached (returns same instance)")
        void release_isCached() {
            assertThat(ReleaseContext.release())
                    .isSameAs(ReleaseContext.release());
        }
    }

    @Nested
    @DisplayName("factory — hotfix()")
    class HotfixFactory {

        @Test
        @DisplayName("returns PATCH_ONLY / main / hotfix=true")
        void hotfix_returnsHotfixDefaults() {
            ReleaseContext ctx = ReleaseContext.forHotfix();

            assertThat(ctx.restrictBumpTo())
                    .isEqualTo(BumpRestriction.PATCH_ONLY);
            assertThat(ctx.baseBranch()).isEqualTo("main");
            assertThat(ctx.hotfix()).isTrue();
        }

        @Test
        @DisplayName("is cached (returns same instance)")
        void hotfix_isCached() {
            assertThat(ReleaseContext.forHotfix())
                    .isSameAs(ReleaseContext.forHotfix());
        }

        @Test
        @DisplayName("differs from release() instance")
        void hotfix_differsFromRelease() {
            assertThat(ReleaseContext.forHotfix())
                    .isNotEqualTo(ReleaseContext.release());
        }
    }

    @Nested
    @DisplayName("constructor — validation")
    class ConstructorValidation {

        @Test
        @DisplayName("rejects null restrictBumpTo")
        void constructor_rejectsNullRestriction() {
            assertThatThrownBy(() -> new ReleaseContext(
                    null, "develop", false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("restrictBumpTo");
        }

        @Test
        @DisplayName("rejects null baseBranch")
        void constructor_rejectsNullBranch() {
            assertThatThrownBy(() -> new ReleaseContext(
                    BumpRestriction.ANY, null, false))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("baseBranch");
        }

        @Test
        @DisplayName("rejects blank baseBranch")
        void constructor_rejectsBlankBranch() {
            assertThatThrownBy(() -> new ReleaseContext(
                    BumpRestriction.ANY, "  ", false))
                    .isInstanceOf(
                            IllegalArgumentException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("custom combinations are allowed")
        void constructor_allowsCustomCombinations() {
            ReleaseContext ctx = new ReleaseContext(
                    BumpRestriction.PATCH_ONLY,
                    "custom-branch", false);

            assertThat(ctx.baseBranch())
                    .isEqualTo("custom-branch");
            assertThat(ctx.hotfix()).isFalse();
        }
    }

    @Nested
    @DisplayName("BumpRestriction enum")
    class BumpRestrictionEnum {

        @Test
        @DisplayName("declares ANY and PATCH_ONLY")
        void enum_declaresExpectedValues() {
            assertThat(BumpRestriction.values())
                    .containsExactly(
                            BumpRestriction.ANY,
                            BumpRestriction.PATCH_ONLY);
        }
    }
}
