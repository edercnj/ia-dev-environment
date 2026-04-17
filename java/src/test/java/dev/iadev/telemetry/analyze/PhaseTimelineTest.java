package dev.iadev.telemetry.analyze;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PhaseTimelineTest {

    private static final Instant T0 =
            Instant.parse("2026-04-16T12:00:00Z");

    @Test
    void constructor_nullSkill_throws() {
        assertThatThrownBy(() -> new PhaseTimeline(
                null, "Phase-1", T0, T0, 0L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullPhase_throws() {
        assertThatThrownBy(() -> new PhaseTimeline(
                "skill", null, T0, T0, 0L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullStartInstant_throws() {
        assertThatThrownBy(() -> new PhaseTimeline(
                "skill", "Phase-1", null, T0, 0L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullEndInstant_throws() {
        assertThatThrownBy(() -> new PhaseTimeline(
                "skill", "Phase-1", T0, null, 0L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_negativeDuration_throws() {
        assertThatThrownBy(() -> new PhaseTimeline(
                "skill", "Phase-1", T0, T0, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMs");
    }
}
