package dev.iadev.release.telemetry;

import dev.iadev.release.ReleaseContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coverage-driven tests for
 * {@link ReleaseTelemetryWriter} escape branches
 * (story-0039-0014 TASK-013 — raise branch coverage to
 * ≥ 90 %).
 */
@DisplayName("ReleaseTelemetryWriter — JSON escape")
class ReleaseTelemetryWriterEscapeTest {

    @Test
    @DisplayName("escapes backslash")
    void escape_backslash() {
        String line = ReleaseTelemetryWriter.format(
                "PHASE\\SLASH", "3.1.1",
                "2026-04-15T10:00:00Z",
                ReleaseContext.forHotfix());

        assertThat(line).contains("PHASE\\\\SLASH");
    }

    @Test
    @DisplayName("escapes newline, tab, CR, FF, BS")
    void escape_controlChars() {
        String line = ReleaseTelemetryWriter.format(
                "A\nB\tC\rD\fE\bF", "3.1.1",
                "2026-04-15T10:00:00Z",
                ReleaseContext.forHotfix());

        assertThat(line)
                .contains("\\n")
                .contains("\\t")
                .contains("\\r")
                .contains("\\f")
                .contains("\\b");
    }

    @Test
    @DisplayName("escapes low-range control char via "
            + "unicode")
    void escape_lowRangeUnicode() {
        // 0x01 — START OF HEADING
        String line = ReleaseTelemetryWriter.format(
                "A\u0001B", "3.1.1",
                "2026-04-15T10:00:00Z",
                ReleaseContext.forHotfix());

        assertThat(line).contains("\\u0001");
    }

    @Test
    @DisplayName("passes printable ASCII verbatim")
    void escape_printableAscii() {
        String line = ReleaseTelemetryWriter.format(
                "ABC-xyz_123", "3.1.1",
                "2026-04-15T10:00:00Z",
                ReleaseContext.forHotfix());

        assertThat(line).contains("ABC-xyz_123");
    }
}
