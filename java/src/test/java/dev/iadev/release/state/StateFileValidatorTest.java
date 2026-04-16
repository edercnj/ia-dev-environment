package dev.iadev.release.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StateFileValidator}.
 *
 * <p>Maps 1:1 to the 6 Gherkin scenarios declared in
 * story-0039-0002 §7 (TPP-ordered from degenerate to
 * error path).
 */
@DisplayName("StateFileValidatorTest")
class StateFileValidatorTest {

    private final StateFileValidator validator =
            new StateFileValidator();

    @Test
    @DisplayName("Cenario: State file v1 eh rejeitado"
            + " (degenerate)")
    void v1IsRejectedWithSchemaVersionCode() {
        ReleaseState v1 = stateWithSchemaVersion(1);

        assertThatThrownBy(() -> validator.validate(v1))
                .isInstanceOf(
                        StateFileValidationException.class)
                .hasMessageContaining("STATE_SCHEMA_VERSION")
                .hasMessageContaining(
                        "/x-release --abort");
    }

    @Test
    @DisplayName("Cenario: State file v2 eh lido com sucesso"
            + " (happy path)")
    void v2IsAcceptedWithoutError() {
        ReleaseState v2 = minimalV2State();

        assertThatCode(() -> validator.validate(v2))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cenario: Roundtrip de campo nextActions"
            + " (boundary) — two valid actions accepted")
    void nextActionsWithValidCommandsAccepted() {
        ReleaseState state = minimalV2State()
                .withNextActions(List.of(
                        new NextAction("a",
                                "/x-release"),
                        new NextAction("b",
                                "/x-pr-fix")));

        assertThatCode(() -> validator.validate(state))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cenario: phaseDurations vazio eh aceito"
            + " (boundary at-min)")
    void phaseDurationsEmptyIsAccepted() {
        ReleaseState state = minimalV2State();

        assertThat(state.phaseDurations()).isEmpty();
        assertThatCode(() -> validator.validate(state))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cenario: Comando malformado em"
            + " nextActions (error path)")
    void malformedCommandIsRejected() {
        ReleaseState state = minimalV2State()
                .withNextActions(List.of(
                        new NextAction("bad", "git push")));

        assertThatThrownBy(() -> validator.validate(state))
                .isInstanceOf(
                        StateFileValidationException.class)
                .hasMessageContaining("STATE_INVALID_ACTION");
    }

    @Test
    @DisplayName("Uppercase command in nextActions is"
            + " rejected (regex ^/[a-z\\-]+)")
    void uppercaseCommandIsRejected() {
        ReleaseState state = minimalV2State()
                .withNextActions(List.of(
                        new NextAction("bad",
                                "/X-RELEASE")));

        assertThatThrownBy(() -> validator.validate(state))
                .isInstanceOf(
                        StateFileValidationException.class)
                .hasMessageContaining("STATE_INVALID_ACTION");
    }

    @Test
    @DisplayName("Null nextActions list is accepted")
    void nullNextActionsIsAccepted() {
        ReleaseState state = minimalV2State()
                .withNextActions(null);

        assertThatCode(() -> validator.validate(state))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Schema version 3 is also rejected"
            + " (only 2 is accepted)")
    void schemaVersionThreeIsRejected() {
        ReleaseState v3 = stateWithSchemaVersion(3);

        assertThatThrownBy(() -> validator.validate(v3))
                .isInstanceOf(
                        StateFileValidationException.class)
                .hasMessageContaining("STATE_SCHEMA_VERSION");
    }

    @Test
    @DisplayName("Null command in NextAction is rejected")
    void nullCommandIsRejected() {
        ReleaseState state = minimalV2State()
                .withNextActions(List.of(
                        new NextAction("label", null)));

        assertThatThrownBy(() -> validator.validate(state))
                .isInstanceOf(
                        StateFileValidationException.class)
                .hasMessageContaining("STATE_INVALID_ACTION");
    }

    private static ReleaseState stateWithSchemaVersion(
            int version) {
        return new ReleaseState(
                version,
                "3.2.0",
                "APPROVAL_PENDING",
                "release/3.2.0",
                "develop",
                false, false, false, false,
                false,
                "2026-04-13T08:00:00Z",
                "2026-04-13T08:12:34Z",
                List.of(),
                "3.2.0", "3.1.0", "minor",
                null, null, null, null, null,
                null,
                List.of(),
                null,
                Map.of(),
                null,
                null,
                null,
                null);
    }

    private static ReleaseState minimalV2State() {
        return stateWithSchemaVersion(2);
    }
}
