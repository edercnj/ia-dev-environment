package dev.iadev.domain.qualitygate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for value objects: VagueTerm, GherkinScenario,
 * DataContractField, ScenarioScore, QualityGateResult.
 */
class ValueObjectsTest {

    @Nested
    class VagueTermTests {

        @Test
        void create_validTerm_succeeds() {
            var vt = new VagueTerm(
                    "resultado esperado", StepType.THEN);
            assertThat(vt.term())
                    .isEqualTo("resultado esperado");
            assertThat(vt.stepType())
                    .isEqualTo(StepType.THEN);
        }

        @Test
        void create_nullTerm_throwsException() {
            assertThatThrownBy(() ->
                    new VagueTerm(null, StepType.GIVEN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("term");
        }

        @Test
        void create_blankTerm_throwsException() {
            assertThatThrownBy(() ->
                    new VagueTerm("  ", StepType.GIVEN))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void create_nullStepType_throwsException() {
            assertThatThrownBy(() ->
                    new VagueTerm("test", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("stepType");
        }
    }

    @Nested
    class GherkinScenarioTests {

        @Test
        void create_validScenario_listsAreImmutable() {
            var scenario = new GherkinScenario(
                    "@GK-1",
                    List.of("DADO state"),
                    List.of("QUANDO action"),
                    List.of("ENTAO result"));
            assertThat(scenario.givenLines())
                    .isUnmodifiable();
            assertThat(scenario.whenLines())
                    .isUnmodifiable();
            assertThat(scenario.thenLines())
                    .isUnmodifiable();
        }
    }

    @Nested
    class ScenarioScoreTests {

        @Test
        void total_allFive_returns15() {
            var score = new ScenarioScore(
                    "@GK-1", 5, 5, 5, List.of());
            assertThat(score.total()).isEqualTo(15);
        }

        @Test
        void total_allZero_returnsZero() {
            var score = new ScenarioScore(
                    "@GK-1", 0, 0, 0,
                    List.of("issue1", "issue2"));
            assertThat(score.total()).isZero();
            assertThat(score.issues()).hasSize(2);
        }

        @Test
        void issues_isImmutable() {
            var score = new ScenarioScore(
                    "@GK-1", 5, 5, 5, List.of());
            assertThat(score.issues()).isUnmodifiable();
        }
    }

    @Nested
    class QualityGateResultTests {

        @Test
        void create_validResult_allFieldsPresent() {
            var result = new QualityGateResult(
                    85, 70, true,
                    List.of(), 10, 10, 10, 15, 10,
                    List.of());
            assertThat(result.score()).isEqualTo(85);
            assertThat(result.threshold()).isEqualTo(70);
            assertThat(result.passed()).isTrue();
            assertThat(result.dataContractScore())
                    .isEqualTo(10);
            assertThat(result.typeExplicitnessScore())
                    .isEqualTo(10);
            assertThat(result.scenarioCountScore())
                    .isEqualTo(10);
            assertThat(result.vaguenessScore())
                    .isEqualTo(15);
            assertThat(result.dependencyScore())
                    .isEqualTo(10);
        }

        @Test
        void scenarioScores_isImmutable() {
            var result = new QualityGateResult(
                    100, 70, true,
                    List.of(), 10, 10, 10, 15, 10,
                    List.of());
            assertThat(result.scenarioScores())
                    .isUnmodifiable();
        }

        @Test
        void actionItems_isImmutable() {
            var result = new QualityGateResult(
                    100, 70, true,
                    List.of(), 10, 10, 10, 15, 10,
                    List.of());
            assertThat(result.actionItems())
                    .isUnmodifiable();
        }
    }

    @Nested
    class DataContractFieldTests {

        @Test
        void create_mandatoryField_mandatoryTrue() {
            var field = new DataContractField(
                    "name", "String", true);
            assertThat(field.mandatory()).isTrue();
        }

        @Test
        void create_optionalField_mandatoryFalse() {
            var field = new DataContractField(
                    "age", "int", false);
            assertThat(field.mandatory()).isFalse();
        }
    }

    @Nested
    class StepTypeTests {

        @Test
        void values_containsAllThreeTypes() {
            assertThat(StepType.values()).hasSize(3);
            assertThat(StepType.values()).containsExactly(
                    StepType.GIVEN,
                    StepType.WHEN,
                    StepType.THEN);
        }
    }
}
