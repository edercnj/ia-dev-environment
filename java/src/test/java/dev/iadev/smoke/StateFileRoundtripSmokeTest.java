package dev.iadev.smoke;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.iadev.release.state.NextAction;
import dev.iadev.release.state.ReleaseState;
import dev.iadev.release.state.StateFileValidator;
import dev.iadev.release.state.WaitingFor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Smoke test that serializes a fully populated v2 release
 * state, deserializes it back, and asserts field-by-field
 * equality for all 25 fields including the 5 new v2 ones.
 *
 * <p>Implements TASK-007 of story-0039-0002. Complements the
 * fine-grained {@code ReleaseStateTest} (record unit coverage)
 * by validating end-to-end JSON roundtrip semantics against
 * the {@link StateFileValidator} v2 contract.
 */
@DisplayName("StateFileRoundtripSmokeTest")
class StateFileRoundtripSmokeTest {

    private static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature
                            .WRITE_DATES_AS_TIMESTAMPS)
                    .configure(DeserializationFeature
                                    .FAIL_ON_UNKNOWN_PROPERTIES,
                            true);

    @Test
    @DisplayName("full v2 state roundtrips without loss"
            + " and passes validator")
    void fullV2StateRoundtripsWithoutLoss()
            throws Exception {
        ReleaseState original = canonicalV2State();

        String json = MAPPER.writeValueAsString(original);
        ReleaseState restored =
                MAPPER.readValue(json, ReleaseState.class);

        assertThat(restored).isEqualTo(original);
        assertThatCode(() ->
                new StateFileValidator().validate(restored))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("roundtrip preserves every v2 field"
            + " individually")
    void roundtripPreservesEveryV2Field()
            throws Exception {
        ReleaseState original = canonicalV2State();

        String json = MAPPER.writeValueAsString(original);
        ReleaseState r =
                MAPPER.readValue(json, ReleaseState.class);

        assertThat(r.schemaVersion()).isEqualTo(2);
        assertThat(r.version()).isEqualTo("3.2.0");
        assertThat(r.phase()).isEqualTo("APPROVAL_PENDING");
        assertThat(r.branch()).isEqualTo("release/3.2.0");
        assertThat(r.baseBranch()).isEqualTo("develop");
        assertThat(r.interactive()).isTrue();
        assertThat(r.phasesCompleted())
                .containsExactly("INITIALIZED", "DETERMINED");
        assertThat(r.prNumber()).isEqualTo(297);
        assertThat(r.nextActions()).hasSize(2);
        assertThat(r.waitingFor())
                .isEqualTo(WaitingFor.PR_MERGE);
        assertThat(r.phaseDurations())
                .containsEntry("VALIDATED", 142L);
        assertThat(r.lastPromptAnsweredAt())
                .isEqualTo("2026-04-13T08:12:35Z");
        assertThat(r.githubReleaseUrl()).isNull();
    }

    private static ReleaseState canonicalV2State() {
        return new ReleaseState(
                2,
                "3.2.0",
                "APPROVAL_PENDING",
                "release/3.2.0",
                "develop",
                false, false, false, true,
                "2026-04-13T08:00:00Z",
                "2026-04-13T08:12:34Z",
                List.of("INITIALIZED", "DETERMINED"),
                "3.2.0", "3.1.0", "minor",
                297,
                "https://github.com/owner/repo/pull/297",
                "Release v3.2.0",
                "## [3.2.0] - 2026-04-13\n...",
                "Release v3.2.0",
                List.of(
                        new NextAction(
                                "PR mergeado — continuar",
                                "/x-release"),
                        new NextAction(
                                "Rodar fix-pr-comments",
                                "/x-pr-fix-comments")),
                WaitingFor.PR_MERGE,
                Map.of(
                        "VALIDATED", 142L,
                        "BRANCHED", 3L),
                "2026-04-13T08:12:35Z",
                null);
    }
}
