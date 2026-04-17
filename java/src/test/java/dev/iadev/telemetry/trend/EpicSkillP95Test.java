package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EpicSkillP95Test {

    @Test
    void construct_validValues_succeeds() {
        EpicSkillP95 row = new EpicSkillP95(
                "EPIC-0040", "x-story-implement", 1234L, 10L);
        assertThat(row.epicId()).isEqualTo("EPIC-0040");
        assertThat(row.skill()).isEqualTo("x-story-implement");
        assertThat(row.p95Ms()).isEqualTo(1234L);
        assertThat(row.invocations()).isEqualTo(10L);
    }

    @Test
    void construct_blankEpic_throws() {
        assertThatThrownBy(() -> new EpicSkillP95(
                " ", "foo", 100L, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_blankSkill_throws() {
        assertThatThrownBy(() -> new EpicSkillP95(
                "EPIC-0040", " ", 100L, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_negativeP95_throws() {
        assertThatThrownBy(() -> new EpicSkillP95(
                "EPIC-0040", "foo", -1L, 10L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_negativeInvocations_throws() {
        assertThatThrownBy(() -> new EpicSkillP95(
                "EPIC-0040", "foo", 100L, -1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
