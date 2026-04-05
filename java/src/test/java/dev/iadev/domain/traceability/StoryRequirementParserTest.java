package dev.iadev.domain.traceability;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoryRequirementParserTest {

    @Nested
    class EmptyInput {

        @Test
        void parse_nullInput_returnsEmptyList() {
            var result = StoryRequirementParser.parse(null);

            assertThat(result).isEmpty();
        }

        @Test
        void parse_emptyString_returnsEmptyList() {
            var result = StoryRequirementParser.parse("");

            assertThat(result).isEmpty();
        }

        @Test
        void parse_blankString_returnsEmptyList() {
            var result = StoryRequirementParser.parse("   \n  ");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class NoGherkinSection {

        @Test
        void parse_noSection7_returnsEmptyList() {
            var markdown = """
                    # Story Title

                    ## 1. Dependencies
                    Some text.

                    ## 3. Description
                    More text.
                    """;

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class SingleScenario {

        @Test
        void parse_oneGherkinScenario_returnsOneRequirement() {
            var markdown = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: payment approved
                      DADO a valid card
                      QUANDO payment is processed
                      ENTAO status is 200
                    """;

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().gherkinId())
                    .isEqualTo("@GK-1");
            assertThat(result.getFirst().title())
                    .isEqualTo("payment approved");
        }
    }

    @Nested
    class MultipleScenarios {

        @Test
        void parse_fourScenarios_returnsFourRequirements() {
            var markdown = buildFourScenarioMarkdown();

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result).hasSize(4);
        }

        @Test
        void parse_fourScenarios_firstHasCorrectId() {
            var markdown = buildFourScenarioMarkdown();

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result.get(0).gherkinId())
                    .isEqualTo("@GK-1");
            assertThat(result.get(0).title())
                    .isEqualTo("pagamento aprovado");
        }

        @Test
        void parse_fourScenarios_fourthHasCorrectId() {
            var markdown = buildFourScenarioMarkdown();

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result.get(3).gherkinId())
                    .isEqualTo("@GK-4");
            assertThat(result.get(3).title())
                    .isEqualTo("duplicado");
        }

        @Test
        void parse_scenariosWithEnglishKeyword_parsed() {
            var markdown = """
                    ## 7. Acceptance Criteria (Gherkin)

                    @GK-1
                    Scenario: happy path
                      Given valid input
                      When processed
                      Then success

                    @GK-2
                    Scenario: error path
                      Given invalid input
                      When processed
                      Then failure
                    """;

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).title())
                    .isEqualTo("happy path");
            assertThat(result.get(1).title())
                    .isEqualTo("error path");
        }

        private String buildFourScenarioMarkdown() {
            return """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: pagamento aprovado
                      DADO um cartao valido
                      QUANDO o pagamento e processado
                      ENTAO o status e 200

                    @GK-2
                    Cenario: pagamento negado
                      DADO um cartao invalido
                      QUANDO o pagamento e processado
                      ENTAO o status e 400

                    @GK-3
                    Cenario: timeout
                      DADO um timeout do autorizador
                      QUANDO o pagamento e processado
                      ENTAO o status e 504

                    @GK-4
                    Cenario: duplicado
                      DADO uma transacao duplicada
                      QUANDO o pagamento e processado
                      ENTAO o status e 409
                    """;
        }
    }

    @Nested
    class AcceptanceTestMapping {

        @Test
        void parse_subTasksWithAtIds_mapsToRequirements() {
            var markdown = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: payment approved
                      DADO valid card
                      QUANDO processed
                      ENTAO 200

                    @GK-2
                    Cenario: payment denied
                      DADO invalid card
                      QUANDO processed
                      ENTAO 400

                    ## 8. Sub-tarefas

                    - [ ] [AT-1] @GK-1 test payment approved
                    - [ ] [AT-2] @GK-2 test payment denied
                    """;

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).acceptanceTestId())
                    .hasValue("AT-1");
            assertThat(result.get(1).acceptanceTestId())
                    .hasValue("AT-2");
        }

        @Test
        void parse_noSubTaskSection_atIdsAreEmpty() {
            var markdown = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: payment approved
                      DADO valid card
                      QUANDO processed
                      ENTAO 200
                    """;

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result.getFirst().acceptanceTestId())
                    .isEmpty();
        }
    }

    @Nested
    class SectionBoundary {

        @Test
        void parse_scenariosStopAtNextSection() {
            var markdown = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: first scenario
                      DADO something
                      QUANDO processed
                      ENTAO result

                    ## 8. Sub-tarefas

                    @GK-NOT-A-SCENARIO
                    This is not a scenario line
                    """;

            var result = StoryRequirementParser.parse(markdown);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().gherkinId())
                    .isEqualTo("@GK-1");
        }
    }
}
