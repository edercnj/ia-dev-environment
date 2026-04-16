package dev.iadev.release.telemetry;

import dev.iadev.release.ReleaseContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReleaseTelemetryWriter}
 * (story-0039-0014 TASK-009).
 */
@DisplayName("ReleaseTelemetryWriter")
class ReleaseTelemetryWriterTest {

    @Nested
    @DisplayName("releaseType derivation")
    class ReleaseTypeDerivation {

        @Test
        @DisplayName("emits releaseType=hotfix for "
                + "hotfix ctx")
        void format_hotfix() {
            String line = ReleaseTelemetryWriter.format(
                    "DETERMINE", "3.1.1",
                    "2026-04-15T10:00:00Z",
                    ReleaseContext.forHotfix());

            assertThat(line).contains(
                    "\"releaseType\":\"hotfix\"");
        }

        @Test
        @DisplayName("emits releaseType=release for "
                + "standard ctx")
        void format_release() {
            String line = ReleaseTelemetryWriter.format(
                    "DETERMINE", "3.2.0",
                    "2026-04-15T10:00:00Z",
                    ReleaseContext.release());

            assertThat(line).contains(
                    "\"releaseType\":\"release\"");
        }
    }

    @Nested
    @DisplayName("JSONL shape")
    class JsonlShape {

        @Test
        @DisplayName("emits single line without trailing "
                + "newline")
        void format_singleLine() {
            String line = ReleaseTelemetryWriter.format(
                    "DETERMINE", "3.1.1",
                    "2026-04-15T10:00:00Z",
                    ReleaseContext.forHotfix());

            assertThat(line).doesNotContain("\n");
            assertThat(line.charAt(line.length() - 1))
                    .isEqualTo('}');
        }

        @Test
        @DisplayName("keys appear in canonical order")
        void format_keysOrder() {
            String line = ReleaseTelemetryWriter.format(
                    "BRANCH", "3.1.1",
                    "2026-04-15T10:00:00Z",
                    ReleaseContext.forHotfix());

            int ve = line.indexOf("releaseVersion");
            int rt = line.indexOf("releaseType");
            int ph = line.indexOf("phase");
            int ts = line.indexOf("startedAt");

            assertThat(ve).isLessThan(rt);
            assertThat(rt).isLessThan(ph);
            assertThat(ph).isLessThan(ts);
        }

        @Test
        @DisplayName("escapes double quotes in fields")
        void format_escapesQuotes() {
            String line = ReleaseTelemetryWriter.format(
                    "PHASE\"QUOTE", "3.1.1",
                    "2026-04-15T10:00:00Z",
                    ReleaseContext.forHotfix());

            assertThat(line).contains("PHASE\\\"QUOTE");
        }
    }

    @Nested
    @DisplayName("input validation")
    class InputValidation {

        @Test
        @DisplayName("rejects null phase")
        void format_rejectsNullPhase() {
            assertThatThrownBy(() ->
                    ReleaseTelemetryWriter.format(
                            null, "3.1.1",
                            "2026-04-15T10:00:00Z",
                            ReleaseContext.forHotfix()))
                    .isInstanceOf(
                            NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null context")
        void format_rejectsNullContext() {
            assertThatThrownBy(() ->
                    ReleaseTelemetryWriter.format(
                            "DETERMINE", "3.1.1",
                            "2026-04-15T10:00:00Z",
                            null))
                    .isInstanceOf(
                            NullPointerException.class);
        }
    }
}
