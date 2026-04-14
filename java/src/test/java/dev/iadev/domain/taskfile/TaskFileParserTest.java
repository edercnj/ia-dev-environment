package dev.iadev.domain.taskfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TaskFileParserTest {

    @Nested
    class EdgeCases {

        @Test
        void parse_emptyInput_returnsAllAbsentAndEmpty() {
            ParsedTaskFile p = TaskFileParser.parse("");
            assertThat(p.taskId()).isEmpty();
            assertThat(p.storyId()).isEmpty();
            assertThat(p.status()).isEmpty();
            assertThat(p.objective()).isEmpty();
            assertThat(p.inputs()).isEmpty();
            assertThat(p.outputs()).isEmpty();
            assertThat(p.testabilityCheckedKinds()).isEmpty();
            assertThat(p.testabilityReferenceIds()).isEmpty();
            assertThat(p.dodItems()).isEmpty();
            assertThat(p.dependencies()).isEmpty();
        }

        @Test
        void parse_nullInput_throwsNullPointer() {
            assertThatThrownBy(() -> TaskFileParser.parse(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("markdown");
        }

        @Test
        void parse_listFieldsAreImmutable() {
            ParsedTaskFile p = TaskFileParser.parse("");
            assertThatThrownBy(() -> p.dodItems().add("- [ ] x"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void parse_crlfLineEndings_parsesSameAsLf() {
            String lf = "**ID:** TASK-0038-0001-001\n**Story:** story-0038-0001\n";
            String crlf = lf.replace("\n", "\r\n");
            ParsedTaskFile pLf = TaskFileParser.parse(lf);
            ParsedTaskFile pCrlf = TaskFileParser.parse(crlf);
            assertThat(pCrlf.taskId()).isEqualTo(pLf.taskId());
            assertThat(pCrlf.storyId()).isEqualTo(pLf.storyId());
        }
    }

    @Nested
    class Headers {

        @Test
        void parse_onlyIdLine_populatesTaskId() {
            ParsedTaskFile p = TaskFileParser.parse("**ID:** TASK-0038-0001-001");
            assertThat(p.taskId()).contains("TASK-0038-0001-001");
            assertThat(p.storyId()).isEmpty();
            assertThat(p.status()).isEmpty();
        }

        @Test
        void parse_allHeaderLines_populatesThreeFields() {
            String md = """
                    # Task: example

                    **ID:** TASK-0038-0001-001
                    **Story:** story-0038-0001
                    **Status:** Pendente
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.taskId()).contains("TASK-0038-0001-001");
            assertThat(p.storyId()).contains("story-0038-0001");
            assertThat(p.status()).contains("Pendente");
        }
    }

    @Nested
    class Sections {

        @Test
        void parse_objectiveSection_populatesObjectiveBody() {
            String md = """
                    ## 1. Objetivo

                    Deliver atomic scaffolding.

                    ## 2. Contratos I/O
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.objective()).isEqualTo("Deliver atomic scaffolding.");
        }

        @Test
        void parse_inputsAndOutputsSections_populateBodies() {
            String md = """
                    ## 2. Contratos I/O

                    ### 2.1 Inputs

                    - Branch created
                    - Spec available

                    ### 2.2 Outputs

                    - File X exists
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.inputs()).contains("Branch created").contains("Spec available");
            assertThat(p.outputs()).contains("File X exists");
        }
    }

    @Nested
    class Testability {

        @Test
        void parse_independent_populatesKindNoRefs() {
            String md = """
                    ### 2.3 Testabilidade

                    - [x] Independentemente testável
                    - [ ] Requer mock de TASK-0038-0001-999
                    - [ ] Coalescível com TASK-0038-0001-888
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.testabilityCheckedKinds()).containsExactly(TestabilityKind.INDEPENDENT);
            assertThat(p.testabilityReferenceIds()).isEmpty();
        }

        @Test
        void parse_requiresMock_extractsReference() {
            String md = """
                    ### 2.3 Testabilidade

                    - [ ] Independentemente testável
                    - [x] Requer mock de TASK-0038-0001-002 (VOs)
                    - [ ] Coalescível com TASK-XXXX-YYYY-NNN
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.testabilityCheckedKinds()).containsExactly(TestabilityKind.REQUIRES_MOCK);
            assertThat(p.testabilityReferenceIds()).containsExactly("TASK-0038-0001-002");
        }

        @Test
        void parse_coalesced_extractsReference() {
            String md = """
                    ### 2.3 Testabilidade

                    - [ ] Independentemente testável
                    - [ ] Requer mock de TASK-YYY
                    - [x] Coalescível com TASK-0038-0001-004
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.testabilityCheckedKinds()).containsExactly(TestabilityKind.COALESCED);
            assertThat(p.testabilityReferenceIds()).containsExactly("TASK-0038-0001-004");
        }

        @Test
        void parse_multipleChecked_returnsAllKindsInOrder() {
            String md = """
                    ### 2.3 Testabilidade

                    - [x] Independentemente testável
                    - [x] Requer mock de TASK-0038-0001-002
                    - [ ] Coalescível com TASK-XXXX-YYYY-NNN
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.testabilityCheckedKinds())
                    .containsExactly(TestabilityKind.INDEPENDENT, TestabilityKind.REQUIRES_MOCK);
        }

        @Test
        void parse_noneChecked_returnsEmpty() {
            String md = """
                    ### 2.3 Testabilidade

                    - [ ] Independentemente testável
                    - [ ] Requer mock de TASK-0038-0001-002
                    - [ ] Coalescível com TASK-0038-0001-004
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.testabilityCheckedKinds()).isEmpty();
            assertThat(p.testabilityReferenceIds()).isEmpty();
        }

        @Test
        void parse_checkedWithUnknownPrefix_ignoresKindButExtractsRef() {
            String md = """
                    ### 2.3 Testabilidade

                    - [x] Something unrecognized about TASK-0038-0001-777
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.testabilityCheckedKinds()).isEmpty();
            assertThat(p.testabilityReferenceIds()).containsExactly("TASK-0038-0001-777");
        }
    }

    @Nested
    class Dod {

        @Test
        void parse_dodSection_collectsAllChecklistLines() {
            String md = """
                    ## 3. Definition of Done

                    - [ ] Código implementado
                    - [ ] Teste cobre output
                    - [x] `mvn compile` verde
                    - [ ] Red→Green→Refactor
                    - [ ] Contratos I/O respeitados
                    - [ ] Commit atômico

                    ## 4. Dependências
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.dodItems()).hasSize(6);
            assertThat(p.dodItems().get(2)).contains("mvn compile");
        }

        @Test
        void parse_dodSectionWithoutChecklistItems_returnsEmpty() {
            String md = """
                    ## 3. Definition of Done

                    See `plan-task-TASK-0038-0001-003.md`.

                    ## 4. Dependências
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.dodItems()).isEmpty();
        }
    }

    @Nested
    class Dependencies {

        @Test
        void parse_dependenciesTable_extractsTaskIdsFromFirstColumn() {
            String md = """
                    ## 4. Dependências

                    | Depends on | Relação | Pode mockar? |
                    | :--- | :--- | :--- |
                    | TASK-0038-0001-002 | VO consumer | Sim |
                    | TASK-0038-0001-001 | Schema doc | Não |
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.dependencies())
                    .containsExactly("TASK-0038-0001-002", "TASK-0038-0001-001");
        }

        @Test
        void parse_dependenciesWithDashPlaceholder_returnsEmpty() {
            String md = """
                    ## 4. Dependências

                    | Depends on | Relação | Pode mockar? |
                    | :--- | :--- | :--- |
                    | — | — | — |
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.dependencies()).isEmpty();
        }

        @Test
        void parse_dependenciesTableRowWithoutTaskId_isSkipped() {
            String md = """
                    ## 4. Dependências

                    | Depends on | Relação | Pode mockar? |
                    | :--- | :--- | :--- |
                    | TASK-0038-0001-002 | consumer | Sim |
                    | plain-text-row | no task id | — |
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.dependencies()).containsExactly("TASK-0038-0001-002");
        }
    }

    @Nested
    class FullFile {

        @Test
        void parse_fullValidFile_populatesAllFields() {
            String md = """
                    # Task: Example full

                    **ID:** TASK-0038-0001-003
                    **Story:** story-0038-0001
                    **Status:** Em Andamento

                    ## 1. Objetivo

                    Implement the parser.

                    ## 2. Contratos I/O

                    ### 2.1 Inputs

                    - State A

                    ### 2.2 Outputs

                    - File B exists

                    ### 2.3 Testabilidade

                    - [x] Independentemente testável
                    - [ ] Requer mock de TASK-XXXX-YYYY-NNN
                    - [ ] Coalescível com TASK-XXXX-YYYY-NNN

                    ## 3. Definition of Done

                    - [ ] Tests pass
                    - [ ] Build green
                    - [ ] Coverage met
                    - [ ] Atomic commit
                    - [ ] Review approved
                    - [ ] PR merged

                    ## 4. Dependências

                    | Depends on | Relação | Pode mockar? |
                    | :--- | :--- | :--- |
                    | TASK-0038-0001-002 | VO consumer | Sim |
                    """;
            ParsedTaskFile p = TaskFileParser.parse(md);
            assertThat(p.taskId()).contains("TASK-0038-0001-003");
            assertThat(p.storyId()).contains("story-0038-0001");
            assertThat(p.status()).contains("Em Andamento");
            assertThat(p.objective()).isEqualTo("Implement the parser.");
            assertThat(p.inputs()).contains("State A");
            assertThat(p.outputs()).contains("File B exists");
            assertThat(p.testabilityCheckedKinds()).containsExactly(TestabilityKind.INDEPENDENT);
            assertThat(p.dodItems()).hasSize(6);
            assertThat(p.dependencies()).containsExactly("TASK-0038-0001-002");
        }
    }
}
