package dev.iadev.infrastructure.adapter.output.telemetry;

import dev.iadev.domain.model.PhaseMetric;
import dev.iadev.domain.model.PhaseOutcome;
import dev.iadev.domain.model.ReleaseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link NoopTelemetrySink} covering the null guard
 * and the happy-path no-op.
 */
@DisplayName("NoopTelemetrySink")
class NoopTelemetrySinkTest {

    @Test
    @DisplayName("emit_nullMetric_throwsIllegalArgument")
    void emit_nullMetric_throwsIllegalArgument() {
        NoopTelemetrySink sink = new NoopTelemetrySink();

        assertThatThrownBy(() -> sink.emit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metric");
    }

    @Test
    @DisplayName("emit_validMetric_doesNotThrow")
    void emit_validMetric_doesNotThrow() {
        NoopTelemetrySink sink = new NoopTelemetrySink();
        Instant now = Instant.parse(
                "2026-04-17T10:00:00Z");
        PhaseMetric metric = new PhaseMetric(
                "1.0.0",
                ReleaseType.RELEASE,
                "VALIDATE",
                now, now.plusSeconds(1),
                1L, PhaseOutcome.SUCCESS);

        assertThatCode(() -> sink.emit(metric))
                .doesNotThrowAnyException();
    }
}
