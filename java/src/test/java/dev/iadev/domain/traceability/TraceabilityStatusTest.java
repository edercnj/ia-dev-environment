package dev.iadev.domain.traceability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraceabilityStatusTest {

    @Test
    void values_containsThreeStatuses() {
        assertThat(TraceabilityStatus.values())
                .hasSize(3)
                .containsExactly(
                        TraceabilityStatus.MAPPED,
                        TraceabilityStatus.UNMAPPED_REQUIREMENT,
                        TraceabilityStatus.UNMAPPED_TEST);
    }

    @Test
    void valueOf_mapped_returnsCorrectEnum() {
        assertThat(TraceabilityStatus.valueOf("MAPPED"))
                .isEqualTo(TraceabilityStatus.MAPPED);
    }

    @Test
    void valueOf_unmappedRequirement_returnsCorrectEnum() {
        assertThat(TraceabilityStatus
                .valueOf("UNMAPPED_REQUIREMENT"))
                .isEqualTo(
                        TraceabilityStatus.UNMAPPED_REQUIREMENT);
    }

    @Test
    void valueOf_unmappedTest_returnsCorrectEnum() {
        assertThat(TraceabilityStatus.valueOf("UNMAPPED_TEST"))
                .isEqualTo(TraceabilityStatus.UNMAPPED_TEST);
    }
}
