package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SlowSkillTest {

    @Test
    void construct_validValues_succeeds() {
        SlowSkill s = new SlowSkill("foo", 500L, 42L);
        assertThat(s.skill()).isEqualTo("foo");
        assertThat(s.avgP95Ms()).isEqualTo(500L);
        assertThat(s.invocations()).isEqualTo(42L);
    }

    @Test
    void construct_blankSkill_throws() {
        assertThatThrownBy(() -> new SlowSkill(" ", 500L, 42L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_negativeAvg_throws() {
        assertThatThrownBy(() -> new SlowSkill("foo", -1L, 42L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_negativeInvocations_throws() {
        assertThatThrownBy(() -> new SlowSkill("foo", 500L, -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
