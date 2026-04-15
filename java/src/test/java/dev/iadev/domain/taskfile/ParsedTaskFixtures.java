package dev.iadev.domain.taskfile;

import java.util.List;
import java.util.Optional;

/**
 * Test helper — constructs {@link ParsedTaskFile} instances with sensible defaults so rule
 * tests stay focused on the one or two fields they exercise.
 */
public final class ParsedTaskFixtures {

    private ParsedTaskFixtures() {
        // static-only
    }

    public static ParsedTaskFile empty() {
        return new ParsedTaskFile(
                Optional.empty(), Optional.empty(), Optional.empty(),
                "", "", "",
                List.of(), 0, List.of(), List.of(), List.of());
    }

    public static ParsedTaskFile withId(String id) {
        return new ParsedTaskFile(
                Optional.of(id), Optional.empty(), Optional.empty(),
                "", "", "",
                List.of(), 0, List.of(), List.of(), List.of());
    }

    public static ParsedTaskFile withStatus(String status) {
        return new ParsedTaskFile(
                Optional.empty(), Optional.empty(), Optional.of(status),
                "", "", "",
                List.of(), 0, List.of(), List.of(), List.of());
    }

    public static ParsedTaskFile withOutputs(String outputs) {
        return new ParsedTaskFile(
                Optional.empty(), Optional.empty(), Optional.empty(),
                "", "", outputs,
                List.of(), 0, List.of(), List.of(), List.of());
    }

    public static ParsedTaskFile withTestability(List<TestabilityKind> kinds, List<String> refs) {
        return new ParsedTaskFile(
                Optional.empty(), Optional.empty(), Optional.empty(),
                "", "", "",
                kinds, kinds.size(), refs, List.of(), List.of());
    }

    public static ParsedTaskFile withDod(int itemCount) {
        List<String> items = new java.util.ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add("- [ ] item " + i);
        }
        return new ParsedTaskFile(
                Optional.empty(), Optional.empty(), Optional.empty(),
                "", "", "",
                List.of(), 0, List.of(), List.copyOf(items), List.of());
    }
}
