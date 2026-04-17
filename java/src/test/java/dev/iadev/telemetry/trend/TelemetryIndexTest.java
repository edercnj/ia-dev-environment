package dev.iadev.telemetry.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TelemetryIndexTest {

    @Test
    void construct_validValues_succeeds() {
        TelemetryIndex idx = new TelemetryIndex(
                TelemetryIndex.CURRENT_SCHEMA_VERSION,
                Instant.parse("2026-04-16T12:00:00Z"),
                Map.of("EPIC-0040", 100L),
                List.of(new EpicSkillP95(
                        "EPIC-0040", "foo", 100L, 10L)));
        assertThat(idx.schemaVersion()).isEqualTo("1.0.0");
        assertThat(idx.series()).hasSize(1);
    }

    @Test
    void construct_nullSchema_throws() {
        assertThatThrownBy(() -> new TelemetryIndex(
                null, Instant.now(), Map.of(), List.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void construct_blankSchema_throws() {
        assertThatThrownBy(() -> new TelemetryIndex(
                "", Instant.now(), Map.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void construct_nullMaps_replacedWithEmpty() {
        TelemetryIndex idx = new TelemetryIndex(
                "1.0.0", Instant.now(), null, null);
        assertThat(idx.epicMtimesEpochMs()).isEmpty();
        assertThat(idx.series()).isEmpty();
    }
}
