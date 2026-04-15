package dev.iadev.domain.taskfile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Structured projection of a task file's parseable fields, produced by
 * {@code TaskFileParser} and consumed by validation rules.
 *
 * <p>Nullable extracted fields use {@link Optional} so absence is explicit (no nulls in
 * domain). List fields are defensively copied in the canonical constructor.</p>
 *
 * @param taskId                  value extracted from the {@code **ID:**} line, if present
 * @param storyId                 value extracted from the {@code **Story:**} line, if present
 * @param status                  raw string after {@code **Status:**}, if present
 * @param objective               body of section "## 1. Objetivo" (trimmed; possibly blank)
 * @param inputs                  body of section "### 2.1 Inputs" (trimmed; possibly blank)
 * @param outputs                 body of section "### 2.2 Outputs" (trimmed; possibly blank)
 * @param testabilityCheckedKinds {@link TestabilityKind} values whose checkbox was checked
 * @param testabilityReferenceIds TASK-IDs cited on checked testability lines (REQUIRES_MOCK/COALESCED)
 * @param dodItems                lines matching {@code - [ ]} or {@code - [x]} under "## 3. Definition of Done"
 * @param dependencies            TASK-IDs referenced in the first column of "## 4. Dependências"
 */
public record ParsedTaskFile(
        Optional<String> taskId,
        Optional<String> storyId,
        Optional<String> status,
        String objective,
        String inputs,
        String outputs,
        List<TestabilityKind> testabilityCheckedKinds,
        List<String> testabilityReferenceIds,
        List<String> dodItems,
        List<String> dependencies) {

    public ParsedTaskFile {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(storyId, "storyId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(objective, "objective");
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(outputs, "outputs");
        Objects.requireNonNull(testabilityCheckedKinds, "testabilityCheckedKinds");
        Objects.requireNonNull(testabilityReferenceIds, "testabilityReferenceIds");
        Objects.requireNonNull(dodItems, "dodItems");
        Objects.requireNonNull(dependencies, "dependencies");
        testabilityCheckedKinds = List.copyOf(testabilityCheckedKinds);
        testabilityReferenceIds = List.copyOf(testabilityReferenceIds);
        dodItems = List.copyOf(dodItems);
        dependencies = List.copyOf(dependencies);
    }
}
