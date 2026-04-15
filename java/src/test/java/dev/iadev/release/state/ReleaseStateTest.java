package dev.iadev.release.state;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReleaseState} record.
 *
 * <p>Covers TPP-1 (nil/degenerate), TPP-2 (constant) and
 * TPP-4 (collection roundtrip) for the v2 state-file schema
 * per story-0039-0002 §7.
 */
@DisplayName("ReleaseStateTest")
class ReleaseStateTest {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature
                            .WRITE_DATES_AS_TIMESTAMPS)
                    .configure(DeserializationFeature
                                    .FAIL_ON_UNKNOWN_PROPERTIES,
                            true);

    @Test
    @DisplayName("record is immutable and exposes"
            + " schemaVersion accessor")
    void recordIsImmutableAndExposesSchemaVersion() {
        ReleaseState state = minimalV2State();

        assertThat(state.schemaVersion()).isEqualTo(2);
        assertThat(state.version()).isEqualTo("3.2.0");
    }

    @Test
    @DisplayName("record serializes schemaVersion=2 via Jackson")
    void recordSerializesSchemaVersionAsTwo()
            throws Exception {
        ReleaseState state = minimalV2State();

        String json = MAPPER.writeValueAsString(state);

        assertThat(json).contains("\"schemaVersion\":2");
    }

    @Test
    @DisplayName("nextActions roundtrip preserves label and"
            + " command per entry")
    void nextActionsRoundtripPreservesLabelAndCommand()
            throws Exception {
        NextAction continueAction = new NextAction(
                "PR mergeado — continuar",
                "/x-release --continue-after-merge");
        NextAction fixAction = new NextAction(
                "Rodar fix-pr-comments",
                "/x-pr-fix 297");

        ReleaseState state = new ReleaseState(
                2,
                "3.2.0",
                "APPROVAL_PENDING",
                "release/3.2.0",
                "develop",
                false, false, false, false,
                "2026-04-13T08:00:00Z",
                "2026-04-13T08:12:34Z",
                List.of("INITIALIZED"),
                "3.2.0", "3.1.0", "minor",
                null, null, null, null, null,
                List.of(continueAction, fixAction),
                WaitingFor.PR_MERGE,
                Map.of(),
                "2026-04-13T08:12:35Z",
                null);

        String json = MAPPER.writeValueAsString(state);
        ReleaseState roundtripped =
                MAPPER.readValue(json, ReleaseState.class);

        assertThat(roundtripped.nextActions())
                .hasSize(2)
                .containsExactly(continueAction, fixAction);
    }

    @Test
    @DisplayName("phaseDurations empty map roundtrips"
            + " without error")
    void phaseDurationsEmptyMapRoundtrips()
            throws Exception {
        ReleaseState state = minimalV2State();

        String json = MAPPER.writeValueAsString(state);
        ReleaseState roundtripped =
                MAPPER.readValue(json, ReleaseState.class);

        assertThat(roundtripped.phaseDurations()).isEmpty();
    }

    @Test
    @DisplayName("waitingFor null is accepted (not mandatory)")
    void waitingForNullIsAccepted() {
        ReleaseState state = minimalV2State();

        assertThat(state.waitingFor()).isNull();
    }

    private static ReleaseState minimalV2State() {
        return new ReleaseState(
                2,
                "3.2.0",
                "APPROVAL_PENDING",
                "release/3.2.0",
                "develop",
                false, false, false, false,
                "2026-04-13T08:00:00Z",
                "2026-04-13T08:12:34Z",
                List.of("INITIALIZED"),
                "3.2.0", "3.1.0", "minor",
                null, null, null, null, null,
                List.of(),
                null,
                Map.of(),
                null,
                null);
    }
}
