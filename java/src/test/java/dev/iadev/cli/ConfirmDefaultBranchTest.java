package dev.iadev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfirmDefaultBranchTest {

    @Test
    void of_trueYieldsDefaultYes() {
        assertThat(ConfirmDefault.of(true))
                .isEqualTo(ConfirmDefault.DEFAULT_YES);
    }

    @Test
    void of_falseYieldsDefaultNo() {
        assertThat(ConfirmDefault.of(false))
                .isEqualTo(ConfirmDefault.DEFAULT_NO);
    }

    @Test
    void isYes_trueOnDefaultYes() {
        assertThat(ConfirmDefault.DEFAULT_YES.isYes()).isTrue();
    }

    @Test
    void isYes_falseOnDefaultNo() {
        assertThat(ConfirmDefault.DEFAULT_NO.isYes()).isFalse();
    }
}
