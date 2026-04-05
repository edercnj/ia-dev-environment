package dev.iadev.domain.scopeassessment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScopeMetricsTest {

    @Nested
    class Construction {

        @Test
        void create_allZeros_succeeds() {
            var metrics = new ScopeMetrics(0, 0, false, false, 0);

            assertThat(metrics.componentCount()).isZero();
            assertThat(metrics.newEndpointCount()).isZero();
            assertThat(metrics.hasSchemaChanges()).isFalse();
            assertThat(metrics.hasCompliance()).isFalse();
            assertThat(metrics.dependentCount()).isZero();
        }

        @Test
        void create_typicalValues_storesAllFields() {
            var metrics = new ScopeMetrics(3, 2, true, true, 5);

            assertThat(metrics.componentCount()).isEqualTo(3);
            assertThat(metrics.newEndpointCount()).isEqualTo(2);
            assertThat(metrics.hasSchemaChanges()).isTrue();
            assertThat(metrics.hasCompliance()).isTrue();
            assertThat(metrics.dependentCount()).isEqualTo(5);
        }
    }

    @Nested
    class Validation {

        @Test
        void create_negativeComponentCount_throwsIllegalArgument() {
            assertThatThrownBy(
                    () -> new ScopeMetrics(-1, 0, false, false, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("componentCount")
                    .hasMessageContaining("-1");
        }

        @Test
        void create_negativeEndpointCount_throwsIllegalArgument() {
            assertThatThrownBy(
                    () -> new ScopeMetrics(0, -1, false, false, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("newEndpointCount")
                    .hasMessageContaining("-1");
        }

        @Test
        void create_negativeDependentCount_throwsIllegalArgument() {
            assertThatThrownBy(
                    () -> new ScopeMetrics(0, 0, false, false, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dependentCount")
                    .hasMessageContaining("-1");
        }
    }

    @Nested
    class Equality {

        @Test
        void equals_sameValues_areEqual() {
            var a = new ScopeMetrics(2, 1, true, false, 3);
            var b = new ScopeMetrics(2, 1, true, false, 3);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        void equals_differentValues_areNotEqual() {
            var a = new ScopeMetrics(2, 1, true, false, 3);
            var b = new ScopeMetrics(3, 1, true, false, 3);

            assertThat(a).isNotEqualTo(b);
        }
    }
}
