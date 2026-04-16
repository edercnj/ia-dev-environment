package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TelemetryScrubber}. Covers the mandatory
 * blocked-pattern matrix documented in rule
 * 20-telemetry-privacy.md plus the error-path and degenerate
 * scenarios from the story Gherkin contract.
 */
@DisplayName("TelemetryScrubber")
class TelemetryScrubberTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111");
    private static final Instant TS =
            Instant.parse("2026-04-16T12:34:56.789Z");
    private static final String SESSION =
            "claude-sess-abc123";

    private final TelemetryScrubber scrubber =
            new TelemetryScrubber();

    @Nested
    @DisplayName("scrub — degenerate (no sensitive data)")
    class Degenerate {

        @Test
        @DisplayName("passes a clean event through unchanged")
        void scrub_cleanEvent_returnsEquivalent() {
            TelemetryEvent event = base()
                    .withFailureReason("timeout")
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .isEqualTo("timeout");
            assertThat(result.metadata())
                    .isEqualTo(event.metadata());
        }

        @Test
        @DisplayName("returns original when failureReason"
                + " is null")
        void scrub_nullFailureReason_returnsOriginal() {
            TelemetryEvent event = base().build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason()).isNull();
        }

        @Test
        @DisplayName("throws when event is null")
        void scrub_nullEvent_throwsNpe() {
            assertThatThrownBy(() -> scrubber.scrub(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("scrub — happy path patterns")
    class HappyPath {

        @Test
        @DisplayName("masks AWS access key")
        void scrub_awsKey_masked() {
            TelemetryEvent event = base()
                    .withFailureReason(
                            "leaked key AKIAIOSFODNN7EXAMPLE")
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("AWS_KEY_REDACTED")
                    .doesNotContain("AKIAIOSFODNN7EXAMPLE");
        }

        @Test
        @DisplayName("masks AWS secret assignment")
        void scrub_awsSecret_masked() {
            TelemetryEvent event = base()
                    .withFailureReason(
                            "aws_secret_key=wJalrXUtnFEMI/K7"
                                    + "MDENG/bPxRfiCYEXAMPLE")
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("AWS_SECRET_REDACTED")
                    .doesNotContain(
                            "wJalrXUtnFEMI/K7MDENG/bPxRfi"
                                    + "CYEXAMPLE");
        }

        @Test
        @DisplayName("masks JWT inside Bearer header")
        void scrub_bearerJwt_masked() {
            String jwt = "eyJhbGciOiJIUzI1NiJ9"
                    + ".eyJzdWIiOiIxMjM0"
                    + ".S1gnAtUre_xyz";
            TelemetryEvent event = base()
                    .withFailureReason("Bearer " + jwt)
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("BEARER_REDACTED")
                    .doesNotContain(jwt);
        }

        @Test
        @DisplayName("masks standalone JWT without Bearer")
        void scrub_jwt_masked() {
            String jwt = "eyJhbGciOiJSUzI1NiJ9"
                    + ".eyJzdWIiOiJteXVzZXIifQ"
                    + ".SIGNATURE";
            TelemetryEvent event = base()
                    .withFailureReason("token=" + jwt)
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("JWT_REDACTED")
                    .doesNotContain(jwt);
        }

        @Test
        @DisplayName("masks GitHub personal access token")
        void scrub_githubToken_masked() {
            String token = "ghp_"
                    + "1234567890abcdefghij"
                    + "1234567890abcdefghij";
            TelemetryEvent event = base()
                    .withFailureReason("x-header: " + token)
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("GITHUB_TOKEN_REDACTED")
                    .doesNotContain(token);
        }

        @Test
        @DisplayName("masks email in failureReason")
        void scrub_email_masked() {
            TelemetryEvent event = base()
                    .withFailureReason(
                            "user reached out: jane@example.com")
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("EMAIL_REDACTED")
                    .doesNotContain("jane@example.com");
        }

        @Test
        @DisplayName("masks Brazilian CPF")
        void scrub_cpf_masked() {
            TelemetryEvent event = base()
                    .withFailureReason(
                            "cpf: 123.456.789-00 invalid")
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("CPF_REDACTED")
                    .doesNotContain("123.456.789-00");
        }

        @Test
        @DisplayName("masks URL with user:password")
        void scrub_urlCredentials_masked() {
            TelemetryEvent event = base()
                    .withFailureReason(
                            "connect https://alice:s3cret@db"
                                    + ".example/app")
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("USER:PASS_REDACTED@HOST")
                    .doesNotContain("alice:s3cret")
                    .doesNotContain("db.example");
        }
    }

    @Nested
    @DisplayName("scrub — structure preservation")
    class StructurePreservation {

        @Test
        @DisplayName("preserves all non-scrubbed fields")
        void scrub_preservesStructure() {
            Map<String, Object> metadata =
                    new LinkedHashMap<>();
            metadata.put("retryCount", 2);
            TelemetryEvent event = new TelemetryEvent(
                    "1.0.0", EVENT_ID, TS, SESSION,
                    "EPIC-0040", "story-0040-0005",
                    "TASK-0040-0005-002",
                    EventType.TOOL_CALL,
                    "x-telemetry-analyze", "phase-2",
                    "Read", 42L, EventStatus.FAILED,
                    "leaked AKIAIOSFODNN7EXAMPLE",
                    metadata);

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.schemaVersion())
                    .isEqualTo("1.0.0");
            assertThat(result.eventId()).isEqualTo(EVENT_ID);
            assertThat(result.timestamp()).isEqualTo(TS);
            assertThat(result.sessionId())
                    .isEqualTo(SESSION);
            assertThat(result.epicId()).isEqualTo("EPIC-0040");
            assertThat(result.storyId())
                    .isEqualTo("story-0040-0005");
            assertThat(result.taskId())
                    .isEqualTo("TASK-0040-0005-002");
            assertThat(result.type())
                    .isEqualTo(EventType.TOOL_CALL);
            assertThat(result.skill())
                    .isEqualTo("x-telemetry-analyze");
            assertThat(result.phase()).isEqualTo("phase-2");
            assertThat(result.tool()).isEqualTo("Read");
            assertThat(result.durationMs()).isEqualTo(42L);
            assertThat(result.status())
                    .isEqualTo(EventStatus.FAILED);
            assertThat(result.failureReason())
                    .contains("AWS_KEY_REDACTED");
            assertThat(result.metadata())
                    .containsEntry("retryCount", 2);
        }
    }

    @Nested
    @DisplayName("scrub — metadata value scrubbing")
    class MetadataValueScrubbing {

        @Test
        @DisplayName("scrubs sensitive values inside"
                + " whitelisted string metadata keys")
        void scrub_commitShaWithEmail_neverHappens() {
            // commitSha is whitelisted. A malicious caller
            // that stuffs an email into commitSha still gets
            // the email redacted.
            Map<String, Object> metadata =
                    new LinkedHashMap<>();
            metadata.put(
                    "commitSha",
                    "abc123 contact jane@example.com");
            TelemetryEvent event = base()
                    .withMetadata(metadata)
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.metadata().get("commitSha"))
                    .asString()
                    .contains("EMAIL_REDACTED")
                    .doesNotContain("jane@example.com");
        }
    }

    @Nested
    @DisplayName("scrub — fail-open semantics")
    class FailOpen {

        @Test
        @DisplayName("returns original event when a rule"
                + " throws RuntimeException (invalid"
                + " backreference in replacement)")
        void scrub_ruleThrows_returnsOriginal() {
            // A replacement referencing group 9 on a regex
            // with no groups triggers
            // IndexOutOfBoundsException in
            // Matcher.replaceAll, exercising the
            // fail-open path.
            java.util.List<ScrubRule> failing =
                    java.util.List.of(new ScrubRule(
                            "bad",
                            java.util.regex.Pattern
                                    .compile("secret"),
                            "$9"));
            TelemetryScrubber brittle =
                    new TelemetryScrubber(
                            failing,
                            new MetadataWhitelist());
            TelemetryEvent event = base()
                    .withFailureReason("secret input")
                    .build();

            TelemetryEvent result = brittle.scrub(event);

            assertThat(result).isSameAs(event);
        }
    }

    @Nested
    @DisplayName("scrub — pattern ordering")
    class PatternOrdering {

        @Test
        @DisplayName("Bearer-JWT composite collapses to a"
                + " single marker (no nested JWT_REDACTED"
                + " inside BEARER_REDACTED)")
        void scrub_bearerJwt_noNestedMarker() {
            String jwt = "eyJhbGciOiJIUzI1NiJ9"
                    + ".eyJzdWIiOiJqb2UifQ"
                    + ".xyz";
            TelemetryEvent event = base()
                    .withFailureReason("Bearer " + jwt)
                    .build();

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.failureReason())
                    .contains("BEARER_REDACTED")
                    .doesNotContain("JWT_REDACTED");
        }
    }

    private static EventBuilder base() {
        return new EventBuilder();
    }

    private static final class EventBuilder {
        private String failureReason;
        private Map<String, Object> metadata;

        EventBuilder withFailureReason(String value) {
            this.failureReason = value;
            return this;
        }

        EventBuilder withMetadata(
                Map<String, Object> value) {
            this.metadata = value;
            return this;
        }

        TelemetryEvent build() {
            return new TelemetryEvent(
                    "1.0.0", EVENT_ID, TS, SESSION,
                    null, null, null,
                    EventType.SESSION_START,
                    null, null, null, null, null,
                    failureReason, metadata);
        }
    }
}
