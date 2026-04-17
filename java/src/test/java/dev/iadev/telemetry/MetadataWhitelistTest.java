package dev.iadev.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link MetadataWhitelist} and its interaction
 * with {@link TelemetryScrubber} when filtering
 * {@link TelemetryEvent#metadata()}.
 */
@DisplayName("MetadataWhitelist")
class MetadataWhitelistTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "22222222-2222-4222-8222-222222222222");
    private static final Instant TS =
            Instant.parse("2026-04-16T13:00:00.000Z");
    private static final String SESSION =
            "claude-sess-xyz789";

    @Nested
    @DisplayName("default whitelist")
    class DefaultWhitelist {

        private final MetadataWhitelist whitelist =
                new MetadataWhitelist();

        @ParameterizedTest
        @ValueSource(strings = {
                "retryCount",
                "commitSha",
                "filesChanged",
                "linesAdded",
                "linesDeleted",
                "exitCode",
                "toolAttempt",
                "phaseNumber"
        })
        @DisplayName("allows each of the 8 whitelisted keys")
        void isAllowed_knownKey_returnsTrue(String key) {
            assertThat(whitelist.isAllowed(key)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "awsSecret",
                "password",
                "apiKey",
                "token",
                "user",
                "email",
                "",
                " "
        })
        @DisplayName("rejects keys outside the whitelist")
        void isAllowed_unknownKey_returnsFalse(String key) {
            assertThat(whitelist.isAllowed(key)).isFalse();
        }

        @Test
        @DisplayName("rejects null key without throwing")
        void isAllowed_null_returnsFalse() {
            assertThat(whitelist.isAllowed(null)).isFalse();
        }

        @Test
        @DisplayName("allowedKeys() exposes exactly 8 keys")
        void allowedKeys_returnsEightKeys() {
            assertThat(whitelist.allowedKeys())
                    .hasSize(8)
                    .containsExactlyInAnyOrder(
                            "retryCount", "commitSha",
                            "filesChanged", "linesAdded",
                            "linesDeleted", "exitCode",
                            "toolAttempt", "phaseNumber");
        }

        @Test
        @DisplayName("allowedKeys() returns an immutable view")
        void allowedKeys_isUnmodifiable() {
            assertThatThrownBy(() ->
                    whitelist.allowedKeys().add("foo"))
                    .isInstanceOf(
                            UnsupportedOperationException
                                    .class);
        }
    }

    @Nested
    @DisplayName("custom whitelist")
    class CustomWhitelist {

        @Test
        @DisplayName("empty whitelist rejects every key")
        void emptyWhitelist_rejectsAll() {
            MetadataWhitelist whitelist =
                    new MetadataWhitelist(Set.of());

            assertThat(whitelist.isAllowed("retryCount"))
                    .isFalse();
        }

        @Test
        @DisplayName("custom key is allowed when injected")
        void customKey_isAllowed() {
            MetadataWhitelist whitelist =
                    new MetadataWhitelist(
                            Set.of("customKey"));

            assertThat(whitelist.isAllowed("customKey"))
                    .isTrue();
            assertThat(whitelist.isAllowed("retryCount"))
                    .isFalse();
        }

        @Test
        @DisplayName("throws when constructed with null")
        void nullAllowed_throws() {
            assertThatThrownBy(() ->
                    new MetadataWhitelist(null))
                    .isInstanceOf(
                            NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("integration with TelemetryScrubber")
    class ScrubberIntegration {

        private final TelemetryScrubber scrubber =
                new TelemetryScrubber();

        @Test
        @DisplayName("removes non-whitelisted key, keeps"
                + " whitelisted key")
        void scrub_mixedKeys_removesNonWhitelisted() {
            Map<String, Object> metadata =
                    new LinkedHashMap<>();
            metadata.put("retryCount", 1);
            metadata.put("awsSecret", "abc");

            TelemetryEvent event = new TelemetryEvent(
                    "1.0.0", EVENT_ID, TS, SESSION,
                    null, null, null,
                    EventType.SESSION_START,
                    null, null, null, null, null, null,
                    metadata);

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.metadata())
                    .containsOnlyKeys("retryCount")
                    .containsEntry("retryCount", 1);
        }

        @Test
        @DisplayName("null metadata map is preserved")
        void scrub_nullMetadata_preserved() {
            TelemetryEvent event = new TelemetryEvent(
                    "1.0.0", EVENT_ID, TS, SESSION,
                    null, null, null,
                    EventType.SESSION_START,
                    null, null, null, null, null, null,
                    null);

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.metadata()).isNull();
        }

        @Test
        @DisplayName("empty metadata map is preserved")
        void scrub_emptyMetadata_preserved() {
            TelemetryEvent event = new TelemetryEvent(
                    "1.0.0", EVENT_ID, TS, SESSION,
                    null, null, null,
                    EventType.SESSION_START,
                    null, null, null, null, null, null,
                    Map.of());

            TelemetryEvent result = scrubber.scrub(event);

            assertThat(result.metadata()).isEmpty();
        }
    }
}
