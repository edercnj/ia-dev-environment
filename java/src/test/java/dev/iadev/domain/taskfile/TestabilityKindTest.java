package dev.iadev.domain.taskfile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TestabilityKindTest {

    @Test
    void values_containsExactlyThreeKinds() {
        assertThat(TestabilityKind.values())
                .containsExactly(
                        TestabilityKind.INDEPENDENT,
                        TestabilityKind.REQUIRES_MOCK,
                        TestabilityKind.COALESCED);
    }

    @Test
    void valueOf_independent_returnsEnumConstant() {
        assertThat(TestabilityKind.valueOf("INDEPENDENT")).isEqualTo(TestabilityKind.INDEPENDENT);
    }
}
