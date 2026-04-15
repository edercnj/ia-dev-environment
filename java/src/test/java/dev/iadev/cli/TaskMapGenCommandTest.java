package dev.iadev.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class TaskMapGenCommandTest {

    private static String validTaskFile(String taskId) {
        return """
                # Task: %s

                **ID:** %s
                **Story:** story-0038-0002
                **Status:** Pendente

                ## 1. Objetivo
                body

                ## 2. Contratos I/O

                ### 2.1 Inputs
                - none

                ### 2.2 Outputs
                - %s exists

                ### 2.3 Testabilidade
                - [x] Independentemente testável
                - [ ] Requer mock de TASK-XXXX-YYYY-NNN
                - [ ] Coalescível com TASK-XXXX-YYYY-NNN

                ## 3. Definition of Done
                - [ ] a
                - [ ] b
                - [ ] c
                - [ ] d
                - [ ] e
                - [ ] f

                ## 4. Dependências
                | Depends on | Relação | Pode mockar? |
                | :--- | :--- | :--- |
                | — | — | — |

                ## 5. Plano de implementação
                placeholder
                """.formatted(taskId, taskId, taskId);
    }

    private record Result(int exitCode, String stdout, String stderr) { }

    private static Result runCli(String... args) {
        StringWriter outBuf = new StringWriter();
        StringWriter errBuf = new StringWriter();
        CommandLine cli = new CommandLine(new TaskMapGenCommand())
                .setOut(new PrintWriter(outBuf))
                .setErr(new PrintWriter(errBuf));
        int code = cli.execute(args);
        return new Result(code, outBuf.toString(), errBuf.toString());
    }

    @Nested
    class HappyPath {

        @Test
        void validInput_writesMapAndExitsZero(@TempDir Path tmp) throws Exception {
            Files.writeString(tmp.resolve("task-TASK-0038-0002-001.md"),
                    validTaskFile("TASK-0038-0002-001"), StandardCharsets.UTF_8);
            Result r = runCli("--story", "story-0038-0002", "--plans-dir", tmp.toString());
            assertThat(r.exitCode()).isEqualTo(TaskMapGenCommand.EXIT_SUCCESS);
            assertThat(r.stdout()).contains("wrote");
            assertThat(r.stderr()).isEmpty();
            assertThat(tmp.resolve("task-implementation-map-STORY-0038-0002.md"))
                    .exists();
        }

        @Test
        void shortFlagAliases_areAccepted(@TempDir Path tmp) throws Exception {
            Files.writeString(tmp.resolve("task-TASK-0038-0002-001.md"),
                    validTaskFile("TASK-0038-0002-001"), StandardCharsets.UTF_8);
            Result r = runCli("-s", "story-0038-0002", "-d", tmp.toString());
            assertThat(r.exitCode()).isEqualTo(TaskMapGenCommand.EXIT_SUCCESS);
        }
    }

    @Nested
    class ErrorPaths {

        @Test
        void missingRequiredStoryFlag_failsWithUsage() {
            Result r = runCli();
            assertThat(r.exitCode()).isNotEqualTo(TaskMapGenCommand.EXIT_SUCCESS);
            assertThat(r.stderr()).containsIgnoringCase("--story");
        }

        @Test
        void noTaskFiles_returnsExitOneWithStderr(@TempDir Path tmp) {
            Result r = runCli("--story", "story-0038-0002", "--plans-dir", tmp.toString());
            assertThat(r.exitCode()).isEqualTo(TaskMapGenCommand.EXIT_FAILURE);
            assertThat(r.stderr()).contains("ERROR").contains("no task files");
        }

        @Test
        void invalidStoryId_returnsExitOneWithStderr(@TempDir Path tmp) {
            Result r = runCli("--story", "not-a-story", "--plans-dir", tmp.toString());
            assertThat(r.exitCode()).isEqualTo(TaskMapGenCommand.EXIT_FAILURE);
            assertThat(r.stderr()).contains("ERROR");
        }

        @Test
        void cyclicDependency_errorMentionsTaskIds(@TempDir Path tmp) throws Exception {
            String body = """
                    # Task: %s

                    **ID:** %s
                    **Story:** story-0038-0002
                    **Status:** Pendente

                    ## 1. Objetivo
                    body

                    ## 2. Contratos I/O

                    ### 2.1 Inputs
                    - none

                    ### 2.2 Outputs
                    - %s exists

                    ### 2.3 Testabilidade
                    - [x] Independentemente testável
                    - [ ] Requer mock de TASK-XXXX-YYYY-NNN
                    - [ ] Coalescível com TASK-XXXX-YYYY-NNN

                    ## 3. Definition of Done
                    - [ ] a
                    - [ ] b
                    - [ ] c
                    - [ ] d
                    - [ ] e
                    - [ ] f

                    ## 4. Dependências
                    | Depends on | Relação | Pode mockar? |
                    | :--- | :--- | :--- |
                    | %s | dep | Sim |

                    ## 5. Plano de implementação
                    placeholder
                    """;
            Files.writeString(tmp.resolve("task-TASK-0038-0002-001.md"),
                    body.formatted("a", "TASK-0038-0002-001", "TASK-0038-0002-001",
                            "TASK-0038-0002-002"),
                    StandardCharsets.UTF_8);
            Files.writeString(tmp.resolve("task-TASK-0038-0002-002.md"),
                    body.formatted("b", "TASK-0038-0002-002", "TASK-0038-0002-002",
                            "TASK-0038-0002-001"),
                    StandardCharsets.UTF_8);
            Result r = runCli("--story", "story-0038-0002", "--plans-dir", tmp.toString());
            assertThat(r.exitCode()).isEqualTo(TaskMapGenCommand.EXIT_FAILURE);
            assertThat(r.stderr())
                    .contains("ERROR")
                    .contains("TASK-0038-0002-001")
                    .contains("TASK-0038-0002-002");
        }

        @Test
        void plansDirDefault_derivedFromStoryId(@TempDir Path workdir) {
            // Use missing default dir; expect the deeply nested fallback path attempted.
            Result r = runCli("--story", "story-9999-0001");
            assertThat(r.exitCode()).isEqualTo(TaskMapGenCommand.EXIT_FAILURE);
            assertThat(r.stderr())
                    .contains("ERROR")
                    .containsAnyOf("plans/epic-9999/plans", "no task files");
        }
    }
}
