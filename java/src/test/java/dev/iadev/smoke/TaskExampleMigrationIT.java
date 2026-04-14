package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.taskfile.ParsedTaskFile;
import dev.iadev.domain.taskfile.Severity;
import dev.iadev.domain.taskfile.TaskFileParser;
import dev.iadev.domain.taskfile.TaskFileValidationResult;
import dev.iadev.domain.taskfile.TaskValidator;
import dev.iadev.domain.taskfile.TestabilityKind;
import dev.iadev.domain.taskfile.ValidationContext;
import dev.iadev.domain.taskfile.ValidationViolation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration smoke test for EPIC-0038 story-0038-0001 (TASK-0038-0001-006).
 *
 * <p>Reads the canonical migrated example at
 * {@code plans/epic-0038/examples/task-TASK-0037-0001-001.md} and verifies it passes
 * through the full pipeline (parser + validator) without ERROR-level violations.</p>
 */
class TaskExampleMigrationIT {

    private static final Path EXAMPLE_PATH = Path.of(
            "..", "plans", "epic-0038", "examples", "task-TASK-0037-0001-001.md");

    private static final String FILENAME = "task-TASK-0037-0001-001.md";

    @Test
    void exampleFile_parsesAndValidatesWithoutErrors() throws IOException {
        String markdown = Files.readString(EXAMPLE_PATH);
        ParsedTaskFile parsed = TaskFileParser.parse(markdown);
        List<ValidationViolation> violations = TaskValidator.defaultValidator()
                .validateAll(parsed, ValidationContext.of(FILENAME));
        List<ValidationViolation> errors = violations.stream()
                .filter(v -> v.severity() == Severity.ERROR)
                .toList();
        assertThat(errors)
                .as("example file must have zero ERROR-level violations; got: %s", errors)
                .isEmpty();
    }

    @Test
    void exampleFile_extractsTaskIdAndTestabilityIndependent() throws IOException {
        String markdown = Files.readString(EXAMPLE_PATH);
        ParsedTaskFile parsed = TaskFileParser.parse(markdown);
        assertThat(parsed.taskId()).contains("TASK-0037-0001-001");
        assertThat(parsed.storyId()).contains("story-0037-0001");
        assertThat(parsed.status()).contains("Concluída");
        assertThat(parsed.testabilityCheckedKinds())
                .containsExactly(TestabilityKind.INDEPENDENT);
    }

    @Test
    void exampleFile_builtValidationResultIsValidIndependent() throws IOException {
        String markdown = Files.readString(EXAMPLE_PATH);
        ParsedTaskFile parsed = TaskFileParser.parse(markdown);
        List<ValidationViolation> violations = TaskValidator.defaultValidator()
                .validateAll(parsed, ValidationContext.of(FILENAME));
        TaskFileValidationResult result = TaskFileValidationResult.of(
                parsed.taskId().orElseThrow(), violations, TestabilityKind.INDEPENDENT);
        assertThat(result.valid()).isTrue();
        assertThat(result.testabilityKind()).isEqualTo(TestabilityKind.INDEPENDENT);
    }
}
