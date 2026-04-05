package dev.iadev.domain.qualitygate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link QualityGateEngine} covering all acceptance
 * criteria from story-0016-0006.
 */
class QualityGateEngineTest {

    private static final int DEFAULT_THRESHOLD = 70;

    /** Story helper: builds markdown with no scenarios. */
    private static String storyWithoutScenarios() {
        return """
                # Story
                ## 1. Dependencias
                | Blocked By | Blocks |
                | :--- | :--- |
                | -- | story-0016-0007 |
                ## 3. Descricao
                Some description.
                """;
    }

    /** Story helper: builds a perfect story with 4 scenarios. */
    private static String perfectStory() {
        return """
                ## 1. Dependencias
                | Blocked By | Blocks |
                | :--- | :--- |
                | story-0016-0001 | story-0016-0007 |

                ## 5. Contratos de Dados

                **InputDTO**

                | Campo | Tipo | Obrigatorio |
                | :--- | :--- | :--- |
                | `name` | String | M |
                | `age` | int | M |

                ## 7. Criterios de Aceite (Gherkin)

                @GK-1
                Cenario: First perfect
                  DADO a REST endpoint /api/items
                  QUANDO POST with payload {"name":"x"}
                  ENTAO status code is 201

                @GK-2
                Cenario: Second perfect
                  DADO an existing item with id 1
                  QUANDO GET /api/items/1
                  ENTAO response contains field "name"

                @GK-3
                Cenario: Third perfect
                  DADO two items in database
                  QUANDO GET /api/items
                  ENTAO response list has size 2

                @GK-4
                Cenario: Fourth perfect
                  DADO an item with id 99
                  QUANDO DELETE /api/items/99
                  ENTAO status code is 204

                ## 8. Sub-tarefas
                """;
    }

    @Nested
    class GK1_NoScenarios {

        @Test
        void evaluate_noScenarios_scoreIsZero() {
            var result = QualityGateEngine.evaluate(
                    storyWithoutScenarios(),
                    List.of(), DEFAULT_THRESHOLD);
            assertThat(result.score()).isZero();
        }

        @Test
        void evaluate_noScenarios_actionItemIndicatesMinimum() {
            var result = QualityGateEngine.evaluate(
                    storyWithoutScenarios(),
                    List.of(), DEFAULT_THRESHOLD);
            assertThat(result.actionItems())
                    .anyMatch(item -> item.contains(
                            "No Gherkin scenarios found")
                            && item.contains(
                            "minimum 4 required"));
        }

        @Test
        void evaluate_noScenarios_passedIsFalse() {
            var result = QualityGateEngine.evaluate(
                    storyWithoutScenarios(),
                    List.of(), DEFAULT_THRESHOLD);
            assertThat(result.passed()).isFalse();
        }
    }

    @Nested
    class GK2_PerfectStory {

        @Test
        void evaluate_perfectStory_scoreIs100() {
            var result = QualityGateEngine.evaluate(
                    perfectStory(),
                    List.of("story-0016-0001"),
                    DEFAULT_THRESHOLD);
            assertThat(result.score()).isEqualTo(100);
        }

        @Test
        void evaluate_perfectStory_passedIsTrue() {
            var result = QualityGateEngine.evaluate(
                    perfectStory(),
                    List.of("story-0016-0001"),
                    DEFAULT_THRESHOLD);
            assertThat(result.passed()).isTrue();
        }

        @Test
        void evaluate_perfectStory_noActionItems() {
            var result = QualityGateEngine.evaluate(
                    perfectStory(),
                    List.of("story-0016-0001"),
                    DEFAULT_THRESHOLD);
            assertThat(result.actionItems()).isEmpty();
        }

        @Test
        void evaluate_perfectStory_allSubScoresMax() {
            var result = QualityGateEngine.evaluate(
                    perfectStory(),
                    List.of("story-0016-0001"),
                    DEFAULT_THRESHOLD);
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
        void evaluate_perfectStory_fourScenarioScores() {
            var result = QualityGateEngine.evaluate(
                    perfectStory(),
                    List.of("story-0016-0001"),
                    DEFAULT_THRESHOLD);
            assertThat(result.scenarioScores()).hasSize(4);
            for (var s : result.scenarioScores()) {
                assertThat(s.total()).isEqualTo(15);
            }
        }
    }

    @Nested
    class GK3_VagueThen {

        @Test
        void evaluate_vagueThen_thenScoreZero() {
            var content = """
                    ## 5. Contratos de Dados

                    **DTO**

                    | Campo | Tipo | Obrigatorio |
                    | :--- | :--- | :--- |
                    | `name` | String | M |

                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Vague then
                      DADO a valid config file
                      QUANDO request sent to endpoint
                      ENTAO resultado esperado

                    @GK-2
                    Cenario: Good
                      DADO valid state
                      QUANDO action performed
                      ENTAO status 200

                    @GK-3
                    Cenario: Good 2
                      DADO valid state 2
                      QUANDO action 2
                      ENTAO status 201

                    @GK-4
                    Cenario: Good 3
                      DADO valid state 3
                      QUANDO action 3
                      ENTAO status 204

                    ## 8. Sub-tarefas
                    """;
            var result = QualityGateEngine.evaluate(
                    content,
                    List.of(), DEFAULT_THRESHOLD);
            var gk1 = result.scenarioScores().stream()
                    .filter(s -> s.scenarioId()
                            .equals("@GK-1"))
                    .findFirst().orElseThrow();
            assertThat(gk1.thenScore()).isZero();
            assertThat(gk1.issues())
                    .anyMatch(i -> i.contains(
                            "resultado esperado")
                            && i.contains("non-verifiable"));
        }
    }

    @Nested
    class GK4_NoMandatoryField {

        @Test
        void evaluate_allOptionalFields_dataContractZero() {
            var content = """
                    ## 5. Contratos de Dados

                    **DTO**

                    | Campo | Tipo | Obrigatorio |
                    | :--- | :--- | :--- |
                    | `name` | String | O |
                    | `age` | int | O |

                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Test
                      DADO valid state
                      QUANDO action
                      ENTAO result 200

                    ## 8. Sub-tarefas
                    """;
            var result = QualityGateEngine.evaluate(
                    content, List.of(), DEFAULT_THRESHOLD);
            assertThat(result.dataContractScore()).isZero();
            assertThat(result.actionItems())
                    .anyMatch(i -> i.contains(
                            "mandatory (M) field"));
        }
    }

    @Nested
    class GK5_BelowThreshold {

        @Test
        void evaluate_belowThreshold_passedFalse() {
            var content = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Single vague
                      DADO o sistema
                      QUANDO faz algo
                      ENTAO resultado esperado

                    ## 8. Sub-tarefas
                    """;
            var result = QualityGateEngine.evaluate(
                    content, List.of(), DEFAULT_THRESHOLD);
            assertThat(result.passed()).isFalse();
            assertThat(result.score())
                    .isLessThan(DEFAULT_THRESHOLD);
        }
    }

    @Nested
    class GK6_CustomThreshold {

        @Test
        void evaluate_customThreshold85_rejectsLowerScore() {
            var content = """
                    ## 5. Contratos de Dados

                    **DTO**

                    | Campo | Tipo | Obrigatorio |
                    | :--- | :--- | :--- |
                    | `name` | String | M |

                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Clean 1
                      DADO valid state
                      QUANDO action 1
                      ENTAO result 200

                    @GK-2
                    Cenario: Clean 2
                      DADO valid state 2
                      QUANDO action 2
                      ENTAO result 201

                    @GK-3
                    Cenario: With vague
                      DADO o sistema
                      QUANDO action 3
                      ENTAO result 204

                    @GK-4
                    Cenario: Clean 4
                      DADO valid state 4
                      QUANDO action 4
                      ENTAO result 200

                    ## 8. Sub-tarefas
                    """;
            var result = QualityGateEngine.evaluate(
                    content, List.of(), 85);
            assertThat(result.threshold()).isEqualTo(85);
            assertThat(result.passed()).isFalse();
        }
    }

    @Nested
    class Normalization {

        @Test
        void normalize_zeroMaxRaw_returnsZero() {
            assertThat(QualityGateEngine.normalize(0, 0))
                    .isZero();
        }

        @Test
        void normalize_fullScore_returns100() {
            assertThat(QualityGateEngine.normalize(105, 105))
                    .isEqualTo(100);
        }

        @Test
        void normalize_halfScore_returns50() {
            assertThat(QualityGateEngine.normalize(50, 100))
                    .isEqualTo(50);
        }

        @Test
        void computeMaxRaw_fourScenarios_returns115() {
            // 4*15 + 55 = 115
            assertThat(QualityGateEngine.computeMaxRaw(4))
                    .isEqualTo(115);
        }

        @Test
        void computeMaxRaw_zeroScenarios_returns55() {
            assertThat(QualityGateEngine.computeMaxRaw(0))
                    .isEqualTo(55);
        }
    }

    @Nested
    class DependencyScoring {

        @Test
        void evaluate_depsExistInIndex_fullScore() {
            var content = """
                    ## 1. Dependencias
                    | Blocked By | Blocks |
                    | :--- | :--- |
                    | story-001 | story-003 |

                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Test
                      DADO state
                      QUANDO action
                      ENTAO result

                    ## 8. Sub
                    """;
            var result = QualityGateEngine.evaluate(
                    content,
                    List.of("story-001", "story-002"),
                    DEFAULT_THRESHOLD);
            assertThat(result.dependencyScore())
                    .isEqualTo(10);
        }

        @Test
        void evaluate_depsMissingFromIndex_zeroScore() {
            var content = """
                    ## 1. Dependencias
                    | Blocked By | Blocks |
                    | :--- | :--- |
                    | story-999 | story-003 |

                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Test
                      DADO state
                      QUANDO action
                      ENTAO result

                    ## 8. Sub
                    """;
            var result = QualityGateEngine.evaluate(
                    content,
                    List.of("story-001"),
                    DEFAULT_THRESHOLD);
            assertThat(result.dependencyScore()).isZero();
            assertThat(result.actionItems())
                    .anyMatch(i -> i.contains("story-999")
                            && i.contains("not found"));
        }

        @Test
        void evaluate_noDeps_fullScore() {
            var content = """
                    ## 1. Dependencias
                    | Blocked By | Blocks |
                    | :--- | :--- |
                    | -- | story-003 |

                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Test
                      DADO state
                      QUANDO action
                      ENTAO result

                    ## 8. Sub
                    """;
            var result = QualityGateEngine.evaluate(
                    content,
                    List.of(), DEFAULT_THRESHOLD);
            assertThat(result.dependencyScore())
                    .isEqualTo(10);
        }
    }

    @Nested
    class TypeExplicitness {

        @Test
        void evaluate_genericTypes_typeScoreZero() {
            var content = """
                    ## 5. Contratos de Dados

                    **DTO**

                    | Campo | Tipo | Obrigatorio |
                    | :--- | :--- | :--- |
                    | `payload` | object | M |
                    | `meta` | data | O |

                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Test
                      DADO state
                      QUANDO action
                      ENTAO result

                    ## 8. Sub
                    """;
            var result = QualityGateEngine.evaluate(
                    content, List.of(), DEFAULT_THRESHOLD);
            assertThat(result.typeExplicitnessScore())
                    .isZero();
            assertThat(result.actionItems())
                    .anyMatch(i -> i.contains(
                            "without explicit type"));
        }

        @Test
        void evaluate_explicitTypes_fullScore() {
            var content = """
                    ## 5. Contratos de Dados

                    **DTO**

                    | Campo | Tipo | Obrigatorio |
                    | :--- | :--- | :--- |
                    | `name` | String | M |
                    | `count` | int | O |

                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Test
                      DADO state
                      QUANDO action
                      ENTAO result

                    ## 8. Sub
                    """;
            var result = QualityGateEngine.evaluate(
                    content, List.of(), DEFAULT_THRESHOLD);
            assertThat(result.typeExplicitnessScore())
                    .isEqualTo(10);
        }
    }

    @Nested
    class ScenarioCount {

        @Test
        void evaluate_fewerThanFour_countScoreZero() {
            var content = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Only one
                      DADO state
                      QUANDO action
                      ENTAO result

                    @GK-2
                    Cenario: Two
                      DADO state2
                      QUANDO action2
                      ENTAO result2

                    ## 8. Sub
                    """;
            var result = QualityGateEngine.evaluate(
                    content, List.of(), DEFAULT_THRESHOLD);
            assertThat(result.scenarioCountScore()).isZero();
            assertThat(result.actionItems())
                    .anyMatch(i -> i.contains(
                            "minimum 4 required"));
        }

        @Test
        void evaluate_exactlyFour_fullCountScore() {
            var result = QualityGateEngine.evaluate(
                    perfectStory(),
                    List.of("story-0016-0001"),
                    DEFAULT_THRESHOLD);
            assertThat(result.scenarioCountScore())
                    .isEqualTo(10);
        }
    }
}
