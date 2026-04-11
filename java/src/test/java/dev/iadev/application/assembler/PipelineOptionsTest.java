package dev.iadev.application.assembler;

import dev.iadev.domain.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the PipelineOptions record.
 */
@DisplayName("PipelineOptions")
class PipelineOptionsTest {

    @Nested
    @DisplayName("defaults")
    class Defaults {

        @Test
        @DisplayName("factory method creates defaults")
        void defaults_whenCalled_allFalseAndNullPath() {
            PipelineOptions opts = PipelineOptions.defaults();

            assertThat(opts.dryRun()).isFalse();
            assertThat(opts.force()).isFalse();
            assertThat(opts.verbose()).isFalse();
            assertThat(opts.overwriteConstitution())
                    .isFalse();
            assertThat(opts.resourcesDir()).isNull();
            assertThat(opts.platforms()).isEmpty();
        }
    }

    @Nested
    @DisplayName("custom values")
    class CustomValues {

        @Test
        @DisplayName("all fields set correctly")
        void allFields_whenCalled_setCorrectly() {
            Path resources = Path.of("/tmp/resources");

            var opts = new PipelineOptions(
                    true, true, true, resources);

            assertThat(opts.dryRun()).isTrue();
            assertThat(opts.force()).isTrue();
            assertThat(opts.verbose()).isTrue();
            assertThat(opts.overwriteConstitution())
                    .isFalse();
            assertThat(opts.resourcesDir())
                    .isEqualTo(resources);
            assertThat(opts.platforms()).isEmpty();
        }

        @Test
        @DisplayName("dryRun only")
        void custom_whenCalled_dryRunOnly() {
            var opts = new PipelineOptions(
                    true, false, false, null);

            assertThat(opts.dryRun()).isTrue();
            assertThat(opts.force()).isFalse();
            assertThat(opts.platforms()).isEmpty();
        }

        @Test
        @DisplayName("overwriteConstitution set via"
                + " 5-arg constructor")
        void fiveArgs_overwriteConstitution_setsCorrectly() {
            var opts = new PipelineOptions(
                    false, false, false, true, null);

            assertThat(opts.overwriteConstitution())
                    .isTrue();
            assertThat(opts.platforms()).isEmpty();
        }

        @Test
        @DisplayName("4-arg constructor defaults"
                + " overwriteConstitution to false")
        void fourArgs_overwriteConstitution_defaultsFalse() {
            var opts = new PipelineOptions(
                    false, false, false, null);

            assertThat(opts.overwriteConstitution())
                    .isFalse();
            assertThat(opts.platforms()).isEmpty();
        }
    }

    @Nested
    @DisplayName("platforms field")
    class PlatformsField {

        @Test
        @DisplayName("6-arg constructor with platforms")
        void sixArgs_platforms_setsCorrectly() {
            var opts = new PipelineOptions(
                    false, false, false, false, null,
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(opts.platforms())
                    .containsExactly(Platform.CLAUDE_CODE);
        }

        @Test
        @DisplayName("platforms is immutable defensive copy")
        void platforms_whenSet_isImmutableCopy() {
            var mutable = new java.util.HashSet<>(
                    Set.of(Platform.CLAUDE_CODE));
            var opts = new PipelineOptions(
                    false, false, false, false,
                    null, mutable);

            mutable.add(Platform.SHARED);

            assertThat(opts.platforms()).hasSize(1);
            assertThat(opts.platforms())
                    .contains(Platform.CLAUDE_CODE);
            assertThat(opts.platforms())
                    .doesNotContain(Platform.SHARED);
            assertThatThrownBy(() ->
                    opts.platforms().add(Platform.SHARED))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }

        @Test
        @DisplayName("null platforms defaults to empty")
        void platforms_whenNull_defaultsToEmpty() {
            var opts = new PipelineOptions(
                    false, false, false, false,
                    null, null);

            assertThat(opts.platforms()).isEmpty();
        }

        @Test
        @DisplayName("5-arg constructor defaults "
                + "platforms to empty")
        void fiveArgs_platforms_defaultsToEmpty() {
            var opts = new PipelineOptions(
                    false, false, false, true, null);

            assertThat(opts.platforms()).isEmpty();
        }

        @Test
        @DisplayName("4-arg constructor defaults "
                + "platforms to empty")
        void fourArgs_platforms_defaultsToEmpty() {
            var opts = new PipelineOptions(
                    true, false, false, null);

            assertThat(opts.platforms()).isEmpty();
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("equal options are equal")
        void create_whenCalled_equalOptions() {
            var a = PipelineOptions.defaults();
            var b = PipelineOptions.defaults();

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("different options are not equal")
        void create_whenCalled_differentOptions() {
            var a = PipelineOptions.defaults();
            var b = new PipelineOptions(
                    true, false, false, null);

            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("options with different platforms "
                + "are not equal")
        void create_differentPlatforms_notEqual() {
            var a = new PipelineOptions(
                    false, false, false, false,
                    null, Set.of());
            var b = new PipelineOptions(
                    false, false, false, false,
                    null,
                    Set.of(Platform.CLAUDE_CODE));

            assertThat(a).isNotEqualTo(b);
        }
    }
}
