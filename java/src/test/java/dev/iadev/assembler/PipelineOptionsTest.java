package dev.iadev.assembler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

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
        void defaults_allFalseAndNullPath() {
            PipelineOptions opts = PipelineOptions.defaults();

            assertThat(opts.dryRun()).isFalse();
            assertThat(opts.force()).isFalse();
            assertThat(opts.verbose()).isFalse();
            assertThat(opts.resourcesDir()).isNull();
        }
    }

    @Nested
    @DisplayName("custom values")
    class CustomValues {

        @Test
        @DisplayName("all fields set correctly")
        void allFields_setCorrectly() {
            Path resources = Path.of("/tmp/resources");

            var opts = new PipelineOptions(
                    true, true, true, resources);

            assertThat(opts.dryRun()).isTrue();
            assertThat(opts.force()).isTrue();
            assertThat(opts.verbose()).isTrue();
            assertThat(opts.resourcesDir())
                    .isEqualTo(resources);
        }

        @Test
        @DisplayName("dryRun only")
        void dryRunOnly() {
            var opts = new PipelineOptions(
                    true, false, false, null);

            assertThat(opts.dryRun()).isTrue();
            assertThat(opts.force()).isFalse();
        }
    }

    @Nested
    @DisplayName("equality")
    class Equality {

        @Test
        @DisplayName("equal options are equal")
        void equalOptions() {
            var a = PipelineOptions.defaults();
            var b = PipelineOptions.defaults();

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("different options are not equal")
        void differentOptions() {
            var a = PipelineOptions.defaults();
            var b = new PipelineOptions(
                    true, false, false, null);

            assertThat(a).isNotEqualTo(b);
        }
    }
}
