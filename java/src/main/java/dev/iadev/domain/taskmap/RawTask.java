package dev.iadev.domain.taskmap;

import dev.iadev.domain.taskfile.TestabilityKind;
import java.util.List;
import java.util.Objects;

/**
 * Input contract for the topological sort. Carries the minimum information the algorithm
 * needs from a parsed task file (story-0038-0001) to build the dependency graph.
 *
 * @param taskId                  TASK-XXXX-YYYY-NNN identifier
 * @param title                   short label used in the rendered Mermaid graph
 * @param dependencies            TASK-IDs declared in §4 Dependências
 * @param testabilityKind         the single declared kind, or null when the task file is
 *                                inconsistent (TF-SCHEMA-003); the sorter treats null as
 *                                non-coalesced for graph purposes
 * @param testabilityReferenceIds TASK-IDs cited on the checked testability line
 *                                (REQUIRES_MOCK / COALESCED targets)
 */
public record RawTask(
        String taskId,
        String title,
        List<String> dependencies,
        TestabilityKind testabilityKind,
        List<String> testabilityReferenceIds) {

    public RawTask {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(testabilityReferenceIds, "testabilityReferenceIds");
        if (taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        dependencies = List.copyOf(dependencies);
        testabilityReferenceIds = List.copyOf(testabilityReferenceIds);
    }

    public boolean isCoalescedDeclaration() {
        return testabilityKind == TestabilityKind.COALESCED;
    }
}
