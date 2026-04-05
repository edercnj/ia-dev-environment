package dev.iadev.domain.qualitygate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StoryMarkdownParser}.
 */
class StoryMarkdownParserTest {

    @Nested
    class ExtractScenarios {

        @Test
        void extractScenarios_nullContent_returnsEmpty() {
            var result = StoryMarkdownParser
                    .extractScenarios(null);
            assertThat(result).isEmpty();
        }

        @Test
        void extractScenarios_blankContent_returnsEmpty() {
            var result = StoryMarkdownParser
                    .extractScenarios("  ");
            assertThat(result).isEmpty();
        }

        @Test
        void extractScenarios_noGherkinSection_returnsEmpty() {
            var content = """
                    # Story
                    Some description.
                    ## Other Section
                    More content.
                    """;
            var result = StoryMarkdownParser
                    .extractScenarios(content);
            assertThat(result).isEmpty();
        }

        @Test
        void extractScenarios_singleScenario_parsesCorrectly() {
            var content = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: Test scenario
                      DADO a valid configuration
                      QUANDO the action is performed
                      ENTAO the result is 200

                    ## 8. Sub-tarefas
                    """;
            var result = StoryMarkdownParser
                    .extractScenarios(content);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().scenarioId())
                    .isEqualTo("@GK-1");
            assertThat(result.getFirst().givenLines())
                    .hasSize(1);
            assertThat(result.getFirst().whenLines())
                    .hasSize(1);
            assertThat(result.getFirst().thenLines())
                    .hasSize(1);
        }

        @Test
        void extractScenarios_multipleScenarios_parsesAll() {
            var content = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: First
                      DADO state A
                      QUANDO action X
                      ENTAO result Y

                    @GK-2
                    Cenario: Second
                      DADO state B
                      QUANDO action Z
                      ENTAO result W

                    ## 8. Next
                    """;
            var result = StoryMarkdownParser
                    .extractScenarios(content);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).scenarioId())
                    .isEqualTo("@GK-1");
            assertThat(result.get(1).scenarioId())
                    .isEqualTo("@GK-2");
        }

        @Test
        void extractScenarios_withAndSteps_parsesCorrectly() {
            var content = """
                    ## 7. Criterios de Aceite (Gherkin)

                    @GK-1
                    Cenario: With And steps
                      DADO state A
                      E additional state
                      QUANDO action X
                      ENTAO result Y
                      E another assertion

                    ## 8. Next
                    """;
            var result = StoryMarkdownParser
                    .extractScenarios(content);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().givenLines())
                    .hasSize(2);
            assertThat(result.getFirst().thenLines())
                    .hasSize(2);
        }
    }

    @Nested
    class ExtractDataContract {

        @Test
        void extractDataContract_nullContent_returnsEmpty() {
            var result = StoryMarkdownParser
                    .extractDataContract(null);
            assertThat(result).isEmpty();
        }

        @Test
        void extractDataContract_noSection_returnsEmpty() {
            var content = """
                    # Story
                    No data contract here.
                    """;
            var result = StoryMarkdownParser
                    .extractDataContract(content);
            assertThat(result).isEmpty();
        }

        @Test
        void extractDataContract_withFields_parsesCorrectly() {
            var content = """
                    ## 5. Contratos de Dados

                    **InputDTO**

                    | Campo | Tipo | Obrigatorio |
                    | :--- | :--- | :--- |
                    | `name` | String | M |
                    | `age` | int | O |

                    ## 6. Next
                    """;
            var result = StoryMarkdownParser
                    .extractDataContract(content);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name())
                    .isEqualTo("name");
            assertThat(result.get(0).type())
                    .isEqualTo("String");
            assertThat(result.get(0).mandatory()).isTrue();
            assertThat(result.get(1).name())
                    .isEqualTo("age");
            assertThat(result.get(1).mandatory()).isFalse();
        }

        @Test
        void extractDataContract_multipleContracts_parsesAll() {
            var content = """
                    ## 5. Contratos de Dados

                    **InputDTO**

                    | Campo | Tipo | Obrigatorio |
                    | :--- | :--- | :--- |
                    | `name` | String | M |

                    **OutputDTO**

                    | Campo | Tipo | Obrigatorio |
                    | :--- | :--- | :--- |
                    | `id` | int | M |
                    | `status` | String | M |

                    ## 6. Next
                    """;
            var result = StoryMarkdownParser
                    .extractDataContract(content);
            assertThat(result).hasSize(3);
        }
    }

    @Nested
    class ExtractDependencies {

        @Test
        void extractDependencies_nullContent_returnsEmpty() {
            var result = StoryMarkdownParser
                    .extractDependencies(null);
            assertThat(result).isEmpty();
        }

        @Test
        void extractDependencies_noSection_returnsEmpty() {
            var content = """
                    # Story
                    No dependencies section.
                    """;
            var result = StoryMarkdownParser
                    .extractDependencies(content);
            assertThat(result).isEmpty();
        }

        @Test
        void extractDependencies_emptyDeps_returnsEmpty() {
            var content = """
                    ## 1. Dependencias

                    | Blocked By | Blocks |
                    | :--- | :--- |
                    | -- | story-0016-0007 |
                    """;
            var result = StoryMarkdownParser
                    .extractDependencies(content);
            assertThat(result).isEmpty();
        }

        @Test
        void extractDependencies_singleDep_returnsList() {
            var content = """
                    ## 1. Dependencias

                    | Blocked By | Blocks |
                    | :--- | :--- |
                    | story-0016-0005 | story-0016-0007 |
                    """;
            var result = StoryMarkdownParser
                    .extractDependencies(content);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst())
                    .isEqualTo("story-0016-0005");
        }

        @Test
        void extractDependencies_multipleDeps_returnsList() {
            var content = """
                    ## 1. Dependencias

                    | Blocked By | Blocks |
                    | :--- | :--- |
                    | story-0016-0003, story-0016-0004 | s7 |
                    """;
            var result = StoryMarkdownParser
                    .extractDependencies(content);
            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(
                    "story-0016-0003", "story-0016-0004");
        }
    }

    @Nested
    class IsGenericType {

        @Test
        void isGenericType_null_returnsTrue() {
            assertThat(StoryMarkdownParser
                    .isGenericType(null)).isTrue();
        }

        @Test
        void isGenericType_blank_returnsTrue() {
            assertThat(StoryMarkdownParser
                    .isGenericType("  ")).isTrue();
        }

        @Test
        void isGenericType_object_returnsTrue() {
            assertThat(StoryMarkdownParser
                    .isGenericType("object")).isTrue();
        }

        @Test
        void isGenericType_data_returnsTrue() {
            assertThat(StoryMarkdownParser
                    .isGenericType("data")).isTrue();
        }

        @Test
        void isGenericType_string_returnsFalse() {
            assertThat(StoryMarkdownParser
                    .isGenericType("String")).isFalse();
        }

        @Test
        void isGenericType_int_returnsFalse() {
            assertThat(StoryMarkdownParser
                    .isGenericType("int")).isFalse();
        }

        @Test
        void isGenericType_listOfString_returnsFalse() {
            assertThat(StoryMarkdownParser
                    .isGenericType("List<String>"))
                    .isFalse();
        }
    }
}
