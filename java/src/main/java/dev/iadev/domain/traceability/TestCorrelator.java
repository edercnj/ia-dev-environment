package dev.iadev.domain.traceability;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Correlates story requirements with test methods to produce
 * traceability entries.
 *
 * <p>For each {@code @GK-N} requirement, finds matching test
 * methods by {@code AT-N} linkage and classifies the result
 * as MAPPED, UNMAPPED_REQUIREMENT, or UNMAPPED_TEST.</p>
 */
public final class TestCorrelator {

    private TestCorrelator() {
    }

    /**
     * Correlates requirements with test methods.
     *
     * @param requirements the parsed story requirements
     * @param testMethods  the discovered test methods
     * @return immutable list of traceability entries
     */
    public static List<TraceabilityEntry> correlate(
            List<StoryRequirement> requirements,
            List<TestMethod> testMethods) {
        if (isNullOrEmpty(requirements)
                && isNullOrEmpty(testMethods)) {
            return List.of();
        }

        var safeReqs = safeList(requirements);
        var safeMethods = safeList(testMethods);

        var entries = new ArrayList<TraceabilityEntry>();
        var matchedMethodIndices = new HashSet<Integer>();

        correlateRequirements(
                safeReqs, safeMethods,
                entries, matchedMethodIndices);
        collectUnmappedTests(
                safeMethods, matchedMethodIndices, entries);

        return List.copyOf(entries);
    }

    private static void correlateRequirements(
            List<StoryRequirement> requirements,
            List<TestMethod> testMethods,
            List<TraceabilityEntry> entries,
            Set<Integer> matchedMethodIndices) {
        for (var req : requirements) {
            if (req.acceptanceTestId().isEmpty()) {
                entries.add(
                        TraceabilityEntry.unmappedRequirement(
                                req.gherkinId(), ""));
                continue;
            }

            var atId = req.acceptanceTestId().get();
            Optional<TestMethod> matched = findMatchingMethod(
                    atId, testMethods, matchedMethodIndices);

            if (matched.isPresent()) {
                TestMethod m = matched.get();
                entries.add(TraceabilityEntry.mapped(
                        req.gherkinId(),
                        atId,
                        m.className(),
                        m.methodName()));
            } else {
                entries.add(
                        TraceabilityEntry.unmappedRequirement(
                                req.gherkinId(), atId));
            }
        }
    }

    private static Optional<TestMethod> findMatchingMethod(
            String atId,
            List<TestMethod> testMethods,
            Set<Integer> matchedMethodIndices) {
        for (int i = 0; i < testMethods.size(); i++) {
            if (matchedMethodIndices.contains(i)) {
                continue;
            }
            var method = testMethods.get(i);
            if (method.linkedAtId().isPresent()
                    && method.linkedAtId().get().equals(atId)) {
                matchedMethodIndices.add(i);
                return Optional.of(method);
            }
        }
        return Optional.empty();
    }

    private static void collectUnmappedTests(
            List<TestMethod> testMethods,
            Set<Integer> matchedMethodIndices,
            List<TraceabilityEntry> entries) {
        for (int i = 0; i < testMethods.size(); i++) {
            if (matchedMethodIndices.contains(i)) {
                continue;
            }
            var method = testMethods.get(i);
            if (method.linkedAtId().isEmpty()) {
                entries.add(TraceabilityEntry.unmappedTest(
                        method.className(),
                        method.methodName()));
            }
        }
    }

    private static <T> boolean isNullOrEmpty(List<T> list) {
        return list == null || list.isEmpty();
    }

    private static <T> List<T> safeList(List<T> list) {
        return (list != null) ? list : List.of();
    }
}
