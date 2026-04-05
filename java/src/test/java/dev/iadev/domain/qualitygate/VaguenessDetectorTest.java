package dev.iadev.domain.qualitygate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VaguenessDetector}.
 */
class VaguenessDetectorTest {

    @Nested
    class CheckSingleText {

        @Test
        void check_nullText_returnsEmpty() {
            var result = VaguenessDetector.check(
                    null, StepType.GIVEN);
            assertThat(result).isEmpty();
        }

        @Test
        void check_blankText_returnsEmpty() {
            var result = VaguenessDetector.check(
                    "  ", StepType.GIVEN);
            assertThat(result).isEmpty();
        }

        @Test
        void check_cleanText_returnsEmpty() {
            var result = VaguenessDetector.check(
                    "a valid JSON payload with status 200",
                    StepType.THEN);
            assertThat(result).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "funciona corretamente",
                "funciona bem",
                "resultado esperado",
                "resultado correto",
                "configurado corretamente",
                "faz algo",
                "realiza operacao",
                "o sistema",
                "o usuario",
                "dados validos",
                "dados invalidos",
                "erro apropriado",
                "mensagem adequada"
        })
        void check_prohibitedTerm_detectsTerm(String term) {
            var result = VaguenessDetector.check(
                    "DADO " + term, StepType.GIVEN);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().term())
                    .isEqualTo(term);
            assertThat(result.getFirst().stepType())
                    .isEqualTo(StepType.GIVEN);
        }

        @Test
        void check_caseInsensitive_detectsTerm() {
            var result = VaguenessDetector.check(
                    "DADO Funciona Corretamente",
                    StepType.GIVEN);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().term())
                    .isEqualTo("funciona corretamente");
        }

        @Test
        void check_multipleTerms_detectsAll() {
            var result = VaguenessDetector.check(
                    "o sistema funciona corretamente",
                    StepType.GIVEN);
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    class CheckScenario {

        @Test
        void checkScenario_cleanScenario_returnsEmpty() {
            var scenario = new GherkinScenario(
                    "@GK-1",
                    List.of("DADO a REST endpoint /api/items"),
                    List.of("QUANDO POST with valid JSON"),
                    List.of("ENTAO status code is 201"));
            var result = VaguenessDetector
                    .checkScenario(scenario);
            assertThat(result).isEmpty();
        }

        @Test
        void checkScenario_vagueGiven_detectsTerm() {
            var scenario = new GherkinScenario(
                    "@GK-1",
                    List.of("DADO o sistema configurado"),
                    List.of("QUANDO request is sent"),
                    List.of("ENTAO returns 200"));
            var result = VaguenessDetector
                    .checkScenario(scenario);
            assertThat(result).isNotEmpty();
            assertThat(result.getFirst().stepType())
                    .isEqualTo(StepType.GIVEN);
        }

        @Test
        void checkScenario_vagueWhen_detectsTerm() {
            var scenario = new GherkinScenario(
                    "@GK-2",
                    List.of("DADO a valid config"),
                    List.of("QUANDO faz algo"),
                    List.of("ENTAO returns 200"));
            var result = VaguenessDetector
                    .checkScenario(scenario);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().stepType())
                    .isEqualTo(StepType.WHEN);
        }

        @Test
        void checkScenario_vagueThen_detectsTerm() {
            var scenario = new GherkinScenario(
                    "@GK-3",
                    List.of("DADO a valid config"),
                    List.of("QUANDO request is sent"),
                    List.of("ENTAO resultado esperado"));
            var result = VaguenessDetector
                    .checkScenario(scenario);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().stepType())
                    .isEqualTo(StepType.THEN);
            assertThat(result.getFirst().term())
                    .isEqualTo("resultado esperado");
        }
    }

    @Nested
    class ProhibitedTermsList {

        @Test
        void prohibitedTerms_returnsAllTerms() {
            var terms = VaguenessDetector.prohibitedTerms();
            assertThat(terms).hasSize(13);
            assertThat(terms).contains(
                    "funciona corretamente",
                    "resultado esperado",
                    "dados validos");
        }

        @Test
        void prohibitedTerms_isUnmodifiable() {
            var terms = VaguenessDetector.prohibitedTerms();
            assertThat(terms)
                    .isInstanceOf(List.class)
                    .isUnmodifiable();
        }
    }
}
