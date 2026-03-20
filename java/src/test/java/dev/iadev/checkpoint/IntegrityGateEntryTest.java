package dev.iadev.checkpoint;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IntegrityGateEntry} record.
 */
class IntegrityGateEntryTest {

    @Test
    void constructor_allFields_preservesValues() {
        var timestamp = Instant.parse("2026-03-19T10:00:00Z");
        var gate = new IntegrityGateEntry(
                "compilation", true, null, timestamp
        );

        assertThat(gate.gateName()).isEqualTo("compilation");
        assertThat(gate.passed()).isTrue();
        assertThat(gate.message()).isNull();
        assertThat(gate.timestamp()).isEqualTo(timestamp);
    }

    @Test
    void pass_whenCalled_createsPassingEntry() {
        var gate = IntegrityGateEntry.pass("tests");

        assertThat(gate.gateName()).isEqualTo("tests");
        assertThat(gate.passed()).isTrue();
        assertThat(gate.message()).isNull();
        assertThat(gate.timestamp())
                .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void fail_whenCalled_createsFailingEntry() {
        var gate = IntegrityGateEntry.fail(
                "coverage", "Below 95%"
        );

        assertThat(gate.gateName()).isEqualTo("coverage");
        assertThat(gate.passed()).isFalse();
        assertThat(gate.message()).isEqualTo("Below 95%");
        assertThat(gate.timestamp())
                .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void fail_whenCalled_timestampIsRecent() {
        var before = Instant.now();
        var gate = IntegrityGateEntry.fail("test", "err");
        var after = Instant.now();

        assertThat(gate.timestamp())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }
}
