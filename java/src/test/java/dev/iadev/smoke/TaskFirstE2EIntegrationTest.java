package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.taskmap.TaskImplementationMapGenerator;
import dev.iadev.cli.TaskMapGenCommand;
import dev.iadev.domain.schemaversion.PlanningSchemaVersion;
import dev.iadev.domain.schemaversion.SchemaVersionResolution;
import dev.iadev.domain.schemaversion.SchemaVersionResolver;
import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.TaskFileParser;
import dev.iadev.domain.taskfile.TaskValidator;
import dev.iadev.domain.taskfile.TestabilityKind;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Story-0038-0010 E2E integration test: exercises the full task-first planning and
 * execution surface produced by EPIC-0038 stories 0001-0009 against a minimal
 * synthetic epic in a TempDir. Verifies:
 *
 * <ol>
 *   <li>Task files pass the story-0038-0001 schema validator with zero ERROR-level
 *       violations.</li>
 *   <li>The story-0038-0002 generator emits a task-implementation-map that honours
 *       the declared wave structure (independent tasks in wave 1, join in wave 2).</li>
 *   <li>The story-0038-0002 CLI returns exit 0 when invoked against the fixture.</li>
 *   <li>The story-0038-0008 SchemaVersionResolver detects v2 when the fixture's
 *       execution-state.json declares `planningSchemaVersion: "2.0"`.</li>
 * </ol>
 *
 * <p>The actual LLM-driven skill invocations (x-story-plan, x-task-plan,
 * x-story-implement, x-task-implement) are out of scope for CI — they require a live
 * orchestrator. This test proves the underlying Java contracts invariants wired by
 * the SKILL.md instructions.</p>
 */
class TaskFirstE2EIntegrationTest {

    private static String taskFile(String id, String deps, TestabilityKind kind) {
        String checklist;
        if (kind == TestabilityKind.INDEPENDENT) {
            checklist = "- [x] Independentemente testável\n"
                    + "- [ ] Requer mock de TASK-XXXX-YYYY-NNN\n"
                    + "- [ ] Coalescível com TASK-XXXX-YYYY-NNN";
        } else if (kind == TestabilityKind.REQUIRES_MOCK) {
            checklist = "- [ ] Independentemente testável\n"
                    + "- [x] Requer mock de TASK-9999-0001-999\n"
                    + "- [ ] Coalescível com TASK-XXXX-YYYY-NNN";
        } else {
            checklist = "- [ ] Independentemente testável\n"
                    + "- [ ] Requer mock de TASK-XXXX-YYYY-NNN\n"
                    + "- [x] Coalescível com TASK-9999-0001-999";
        }
        return """
                # Task: %s

                **ID:** %s
                **Story:** story-9999-0001
                **Status:** Pendente

                ## 1. Objetivo
                E2E fixture task.

                ## 2. Contratos I/O

                ### 2.1 Inputs
                - baseline

                ### 2.2 Outputs
                - %s produced (verifiable)

                ### 2.3 Testabilidade
                %s

                ## 3. Definition of Done
                - [ ] code
                - [ ] test
                - [ ] cov
                - [ ] commit
                - [ ] doc
                - [ ] review

                ## 4. Dependências
                | Depends on | Relação | Pode mockar? |
                | :--- | :--- | :--- |
                %s

                ## 5. Plano de Implementação
                placeholder
                """.formatted(id, id, id, checklist,
                        deps.isBlank() ? "| — | — | — |" : deps);
    }

    @Test
    void endToEnd_pipeline_produces_valid_map_and_resolves_v2(@TempDir Path tmp)
            throws IOException {
        // --- set up synthetic epic-9999 in tmp ---
        Path epicDir = tmp.resolve("plans").resolve("epic-9999");
        Path plansDir = epicDir.resolve("plans");
        Files.createDirectories(plansDir);
        Files.writeString(
                epicDir.resolve("execution-state.json"),
                "{\"epicId\": \"TEST\", \"planningSchemaVersion\": \"2.0\"}",
                StandardCharsets.UTF_8);
        Files.writeString(plansDir.resolve("task-TASK-9999-0001-001.md"),
                taskFile("TASK-9999-0001-001", "", TestabilityKind.INDEPENDENT),
                StandardCharsets.UTF_8);
        Files.writeString(plansDir.resolve("task-TASK-9999-0001-002.md"),
                taskFile("TASK-9999-0001-002", "", TestabilityKind.INDEPENDENT),
                StandardCharsets.UTF_8);
        Files.writeString(plansDir.resolve("task-TASK-9999-0001-003.md"),
                taskFile("TASK-9999-0001-003",
                        "| TASK-9999-0001-001 | dep | Sim |\n| TASK-9999-0001-002 | dep | Sim |",
                        TestabilityKind.INDEPENDENT),
                StandardCharsets.UTF_8);

        // --- schema resolution (story-0038-0008) ---
        SchemaVersionResolution resolution = SchemaVersionResolver.resolve(epicDir);
        assertThat(resolution.version()).isEqualTo(PlanningSchemaVersion.V2);
        assertThat(resolution.isFallback()).isFalse();

        // --- per-task parser + validator (story-0038-0001) ---
        TaskValidator validator = TaskValidator.defaultValidator();
        try (Stream<Path> stream = Files.list(plansDir)) {
            stream.filter(p -> p.getFileName().toString().startsWith("task-TASK"))
                    .forEach(p -> assertNoErrorViolations(p, validator));
        }

        // --- generator (story-0038-0002) ---
        Path mapPath = TaskImplementationMapGenerator.generate(plansDir, "story-9999-0001");
        assertThat(mapPath.getFileName().toString())
                .isEqualTo("task-implementation-map-STORY-9999-0001.md");
        String mapMarkdown = Files.readString(mapPath, StandardCharsets.UTF_8);
        assertThat(mapMarkdown)
                .contains("## Dependency Graph")
                .contains("## Execution Order")
                .contains("## Coalesced Groups")
                .contains("## Parallelism Analysis")
                .contains("- Total tasks: 3")
                .contains("- Number of waves: 2")
                .contains("- Largest wave size: 2");

        // --- CLI invocation (story-0038-0002 TASK-006) ---
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        int cliExit = new CommandLine(new TaskMapGenCommand())
                .setOut(new PrintWriter(out))
                .setErr(new PrintWriter(err))
                .execute("--story", "story-9999-0001", "--plans-dir", plansDir.toString());
        assertThat(cliExit).isZero();
    }

    @Test
    void grepSanity_noTaskEmbeddedInStoryAntiPattern() throws IOException {
        Path rulesDir = Path.of(
                "src", "main", "resources", "targets", "claude", "rules");
        try (Stream<Path> rules = Files.list(rulesDir)) {
            rules.filter(p -> p.getFileName().toString().matches("^1[5-9]-.*\\.md$"))
                    .forEach(p -> {
                        try {
                            String md = Files.readString(p, StandardCharsets.UTF_8);
                            assertThat(md)
                                    .as("%s must not endorse the legacy anti-pattern",
                                            p.getFileName())
                                    .doesNotContain("task embedded in story");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private static void assertNoErrorViolations(Path taskFile, TaskValidator validator) {
        try {
            String md = Files.readString(taskFile, StandardCharsets.UTF_8);
            ParsedTaskFile parsed = TaskFileParser.parse(md);
            List<ValidationViolation> violations = validator.validateAll(
                    parsed, ValidationContext.of(taskFile.getFileName().toString()));
            List<ValidationViolation> errors = violations.stream()
                    .filter(v -> v.severity() == Severity.ERROR)
                    .toList();
            assertThat(errors)
                    .as("fixture %s must have zero ERROR-level violations", taskFile)
                    .isEmpty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
