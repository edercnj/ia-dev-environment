package dev.iadev.application.taskmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.taskmap.exception.CyclicDependencyException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TaskImplementationMapGeneratorIT {

    private static String taskFile(String taskId, String title, String depsRow,
            String testabilityChecklist) {
        return """
                # Task: %s

                **ID:** %s
                **Story:** story-0038-0002
                **Status:** Pendente

                ## 1. Objetivo

                %s

                ## 2. Contratos I/O

                ### 2.1 Inputs
                - none

                ### 2.2 Outputs
                - %s exists

                ### 2.3 Testabilidade
                %s

                ## 3. Definition of Done
                - [ ] code
                - [ ] test
                - [ ] coverage
                - [ ] commit
                - [ ] doc
                - [ ] review

                ## 4. Dependências
                %s

                ## 5. Plano de implementação
                placeholder
                """.formatted(title, taskId, title, taskId, testabilityChecklist, depsRow);
    }

    private static String independent() {
        return """
                - [x] Independentemente testável
                - [ ] Requer mock de TASK-XXXX-YYYY-NNN
                - [ ] Coalescível com TASK-XXXX-YYYY-NNN""";
    }

    private static String coalescedWith(String partner) {
        return """
                - [ ] Independentemente testável
                - [ ] Requer mock de TASK-XXXX-YYYY-NNN
                - [x] Coalescível com %s""".formatted(partner);
    }

    private static String depsTable(String... taskIds) {
        if (taskIds.length == 0) {
            return "| Depends on | Relação | Pode mockar? |\n| :--- | :--- | :--- |\n| — | — | — |";
        }
        StringBuilder sb = new StringBuilder("| Depends on | Relação | Pode mockar? |\n| :--- | :--- | :--- |\n");
        for (String id : taskIds) {
            sb.append("| ").append(id).append(" | dep | Sim |\n");
        }
        return sb.toString();
    }

    @Nested
    class HappyPath {

        @Test
        void singleTaskFile_writesValidMap(@TempDir Path tmp) throws IOException {
            Files.writeString(
                    tmp.resolve("task-TASK-0038-0002-001.md"),
                    taskFile("TASK-0038-0002-001", "schema doc", depsTable(), independent()),
                    StandardCharsets.UTF_8);
            Path output = TaskImplementationMapGenerator.generate(tmp, "story-0038-0002");
            assertThat(output.getFileName().toString())
                    .isEqualTo("task-implementation-map-STORY-0038-0002.md");
            String md = Files.readString(output, StandardCharsets.UTF_8);
            assertThat(md).startsWith("# Task Implementation Map — story-0038-0002");
            assertThat(md).contains("- Total tasks: 1");
        }

        @Test
        void sevenTaskFixture_includesCoalescedGroupAndCorrectMetrics(@TempDir Path tmp)
                throws IOException {
            writeFixture(tmp);
            Path output = TaskImplementationMapGenerator.generate(tmp, "story-0038-0002");
            String md = Files.readString(output, StandardCharsets.UTF_8);
            assertThat(md)
                    .contains("- Total tasks: 7")
                    .contains("- (TASK-0038-0002-004 + TASK-0038-0002-005)");
        }

        @Test
        void output_isIdempotentByteForByteAcrossReruns(@TempDir Path tmp) throws IOException {
            writeFixture(tmp);
            Path first = TaskImplementationMapGenerator.generate(tmp, "story-0038-0002");
            byte[] firstBytes = Files.readAllBytes(first);
            Path second = TaskImplementationMapGenerator.generate(tmp, "story-0038-0002");
            byte[] secondBytes = Files.readAllBytes(second);
            assertThat(secondBytes).isEqualTo(firstBytes);
        }
    }

    @Nested
    class ErrorPropagation {

        @Test
        void noTaskFiles_throwsIllegalArgument(@TempDir Path tmp) {
            assertThatThrownBy(() -> TaskImplementationMapGenerator.generate(tmp, "story-0038-0002"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no task files");
        }

        @Test
        void invalidStoryId_throwsIllegalArgument(@TempDir Path tmp) {
            assertThatThrownBy(() -> TaskImplementationMapGenerator.generate(tmp, "not-a-story"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void nullArgs_throwNullPointer(@TempDir Path tmp) {
            assertThatThrownBy(() -> TaskImplementationMapGenerator.generate(null, "story-0038-0002"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> TaskImplementationMapGenerator.generate(tmp, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void cyclicDependency_propagatesAsCyclicException(@TempDir Path tmp)
                throws IOException {
            Files.writeString(
                    tmp.resolve("task-TASK-0038-0002-001.md"),
                    taskFile("TASK-0038-0002-001", "a", depsTable("TASK-0038-0002-002"),
                            independent()),
                    StandardCharsets.UTF_8);
            Files.writeString(
                    tmp.resolve("task-TASK-0038-0002-002.md"),
                    taskFile("TASK-0038-0002-002", "b", depsTable("TASK-0038-0002-001"),
                            independent()),
                    StandardCharsets.UTF_8);
            assertThatThrownBy(() -> TaskImplementationMapGenerator.generate(tmp, "story-0038-0002"))
                    .isInstanceOf(CyclicDependencyException.class);
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void taskFileWithoutId_throwsIllegalArgument(@TempDir Path tmp) throws IOException {
            Files.writeString(tmp.resolve("task-TASK-0038-0002-001.md"),
                    "# Task: missing id\n\n**Story:** story-0038-0002\n**Status:** Pendente\n",
                    StandardCharsets.UTF_8);
            assertThatThrownBy(() -> TaskImplementationMapGenerator.generate(tmp, "story-0038-0002"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("missing **ID:**");
        }

        @Test
        void taskFileWithoutTitleHeading_usesUntitledFallback(@TempDir Path tmp) throws IOException {
            String md = """
                    **ID:** TASK-0038-0002-001
                    **Story:** story-0038-0002
                    **Status:** Pendente

                    ## 1. Objetivo

                    body

                    ## 2. Contratos I/O

                    ### 2.2 Outputs
                    - x

                    ### 2.3 Testabilidade
                    %s

                    ## 3. Definition of Done
                    - [ ] a
                    - [ ] b
                    - [ ] c
                    - [ ] d
                    - [ ] e
                    - [ ] f

                    ## 4. Dependências
                    %s
                    """.formatted(independent(), depsTable());
            Files.writeString(tmp.resolve("task-TASK-0038-0002-001.md"), md,
                    StandardCharsets.UTF_8);
            Path output = TaskImplementationMapGenerator.generate(tmp, "story-0038-0002");
            String result = Files.readString(output, StandardCharsets.UTF_8);
            assertThat(result).contains("TASK-0038-0002-001");
        }

        @Test
        void otherStoriesTaskFiles_areIgnored(@TempDir Path tmp) throws IOException {
            Files.writeString(tmp.resolve("task-TASK-0038-0002-001.md"),
                    taskFile("TASK-0038-0002-001", "a", depsTable(), independent()),
                    StandardCharsets.UTF_8);
            Files.writeString(tmp.resolve("task-TASK-0038-0099-001.md"),
                    taskFile("TASK-0038-0099-001", "other story", depsTable(), independent()),
                    StandardCharsets.UTF_8);
            Path output = TaskImplementationMapGenerator.generate(tmp, "story-0038-0002");
            String md = Files.readString(output, StandardCharsets.UTF_8);
            assertThat(md).contains("- Total tasks: 1");
            assertThat(md).doesNotContain("TASK-0038-0099");
        }

        @Test
        void plansDirDoesNotExist_throwsUncheckedIO(@TempDir Path tmp) {
            Path missing = tmp.resolve("missing");
            assertThatThrownBy(() -> TaskImplementationMapGenerator.generate(missing, "story-0038-0002"))
                    .isInstanceOf(java.io.UncheckedIOException.class);
        }
    }

    private static void writeFixture(Path tmp) throws IOException {
        Files.writeString(tmp.resolve("task-TASK-0038-0002-001.md"),
                taskFile("TASK-0038-0002-001", "schema", depsTable(), independent()),
                StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("task-TASK-0038-0002-002.md"),
                taskFile("TASK-0038-0002-002", "domain",
                        depsTable("TASK-0038-0002-001"), independent()),
                StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("task-TASK-0038-0002-003.md"),
                taskFile("TASK-0038-0002-003", "sorter",
                        depsTable("TASK-0038-0002-002"), independent()),
                StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("task-TASK-0038-0002-004.md"),
                taskFile("TASK-0038-0002-004", "writer",
                        depsTable("TASK-0038-0002-002"),
                        coalescedWith("TASK-0038-0002-005")),
                StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("task-TASK-0038-0002-005.md"),
                taskFile("TASK-0038-0002-005", "generator",
                        depsTable("TASK-0038-0002-003", "TASK-0038-0002-004"),
                        coalescedWith("TASK-0038-0002-004")),
                StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("task-TASK-0038-0002-006.md"),
                taskFile("TASK-0038-0002-006", "cli",
                        depsTable("TASK-0038-0002-005"), independent()),
                StandardCharsets.UTF_8);
        Files.writeString(tmp.resolve("task-TASK-0038-0002-007.md"),
                taskFile("TASK-0038-0002-007", "smoke",
                        depsTable("TASK-0038-0002-006"), independent()),
                StandardCharsets.UTF_8);
    }
}
