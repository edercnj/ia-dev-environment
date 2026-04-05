package dev.iadev.domain.qualitygate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScenarioScorer}.
 */
class ScenarioScorerTest {

    @Nested
    class PerfectScenarios {

        @Test
        void score_perfectScenario_returns15() {
            var scenario = new GherkinScenario(
                    "@GK-1",
                    List.of("DADO a REST endpoint /api/items"),
                    List.of("QUANDO POST with valid JSON"),
                    List.of("ENTAO status code is 201"));
            var result = ScenarioScorer.score(scenario);
            assertThat(result.givenScore()).isEqualTo(5);
            assertThat(result.whenScore()).isEqualTo(5);
            assertThat(result.thenScore()).isEqualTo(5);
            assertThat(result.total()).isEqualTo(15);
            assertThat(result.issues()).isEmpty();
        }

        @Test
        void score_scenarioId_preserved() {
            var scenario = new GherkinScenario(
                    "@GK-42",
                    List.of("DADO valid state"),
                    List.of("QUANDO action occurs"),
                    List.of("ENTAO result is 200"));
            var result = ScenarioScorer.score(scenario);
            assertThat(result.scenarioId())
                    .isEqualTo("@GK-42");
        }
    }

    @Nested
    class VagueSteps {

        @Test
        void score_vagueGiven_givenScoreZero() {
            var scenario = new GherkinScenario(
                    "@GK-1",
                    List.of("DADO o sistema configurado"),
                    List.of("QUANDO request is sent"),
                    List.of("ENTAO returns 200"));
            var result = ScenarioScorer.score(scenario);
            assertThat(result.givenScore()).isZero();
            assertThat(result.whenScore()).isEqualTo(5);
            assertThat(result.thenScore()).isEqualTo(5);
            assertThat(result.total()).isEqualTo(10);
            assertThat(result.issues()).hasSize(1);
        }

        @Test
        void score_vagueWhen_whenScoreZero() {
            var scenario = new GherkinScenario(
                    "@GK-2",
                    List.of("DADO valid config file"),
                    List.of("QUANDO faz algo"),
                    List.of("ENTAO status 200"));
            var result = ScenarioScorer.score(scenario);
            assertThat(result.givenScore()).isEqualTo(5);
            assertThat(result.whenScore()).isZero();
            assertThat(result.thenScore()).isEqualTo(5);
            assertThat(result.issues()).hasSize(1);
            assertThat(result.issues().getFirst())
                    .contains("faz algo");
        }

        @Test
        void score_vagueThen_thenScoreZero() {
            var scenario = new GherkinScenario(
                    "@GK-3",
                    List.of("DADO valid config"),
                    List.of("QUANDO request sent"),
                    List.of("ENTAO resultado esperado"));
            var result = ScenarioScorer.score(scenario);
            assertThat(result.givenScore()).isEqualTo(5);
            assertThat(result.whenScore()).isEqualTo(5);
            assertThat(result.thenScore()).isZero();
            assertThat(result.issues()).hasSize(1);
            assertThat(result.issues().getFirst())
                    .contains("resultado esperado")
                    .contains("non-verifiable");
        }

        @Test
        void score_allVague_totalZero() {
            var scenario = new GherkinScenario(
                    "@GK-4",
                    List.of("DADO o sistema"),
                    List.of("QUANDO faz algo"),
                    List.of("ENTAO resultado esperado"));
            var result = ScenarioScorer.score(scenario);
            assertThat(result.total()).isZero();
            assertThat(result.issues()).hasSize(3);
        }
    }

    @Nested
    class IssueFormatting {

        @Test
        void score_vagueGiven_issueDescribesVague() {
            var scenario = new GherkinScenario(
                    "@GK-3",
                    List.of(
                            "DADO o sistema configurado"),
                    List.of("QUANDO request sent"),
                    List.of("ENTAO 200 returned"));
            var result = ScenarioScorer.score(scenario);
            assertThat(result.issues().getFirst())
                    .contains("@GK-3")
                    .contains("Given")
                    .contains("vague");
        }

        @Test
        void score_vagueThen_issueDescribesNonVerifiable() {
            var scenario = new GherkinScenario(
                    "@GK-1",
                    List.of("DADO valid state"),
                    List.of("QUANDO action"),
                    List.of("ENTAO resultado correto"));
            var result = ScenarioScorer.score(scenario);
            assertThat(result.issues().getFirst())
                    .contains("@GK-1")
                    .contains("Then")
                    .contains("non-verifiable");
        }
    }
}
