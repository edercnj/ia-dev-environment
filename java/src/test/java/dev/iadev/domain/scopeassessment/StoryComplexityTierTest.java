package dev.iadev.domain.scopeassessment;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoryComplexityTierTest {

    @Nested
    class EnumValues {

        @Test
        void values_returnsThreeTiers() {
            assertThat(StoryComplexityTier.values())
                    .hasSize(3);
        }

        @Test
        void values_containsSimpleStandardComplex() {
            assertThat(StoryComplexityTier.values())
                    .containsExactly(
                            StoryComplexityTier.SIMPLE,
                            StoryComplexityTier.STANDARD,
                            StoryComplexityTier.COMPLEX);
        }
    }

    @Nested
    class ValueOf {

        @Test
        void valueOf_simple_returnsSimple() {
            assertThat(StoryComplexityTier.valueOf("SIMPLE"))
                    .isEqualTo(StoryComplexityTier.SIMPLE);
        }

        @Test
        void valueOf_standard_returnsStandard() {
            assertThat(StoryComplexityTier.valueOf("STANDARD"))
                    .isEqualTo(StoryComplexityTier.STANDARD);
        }

        @Test
        void valueOf_complex_returnsComplex() {
            assertThat(StoryComplexityTier.valueOf("COMPLEX"))
                    .isEqualTo(StoryComplexityTier.COMPLEX);
        }
    }
}
