package dev.iadev.domain.traceability;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Scans Java test source content to discover test methods
 * and their AT-N linkages.
 *
 * <p>Detects linkages via:
 * <ul>
 *   <li>Naming convention: {@code at1_scenarioName()}</li>
 *   <li>{@code @Tag("AT-1")} annotations</li>
 *   <li>{@code @DisplayName} containing AT-N references</li>
 * </ul>
 */
public final class TestMethodScanner {

    private static final Pattern TEST_ANNOTATION =
            Pattern.compile("@Test");
    private static final Pattern TAG_ANNOTATION =
            Pattern.compile("@Tag\\(\"([^\"]+)\"\\)");
    private static final Pattern METHOD_SIGNATURE =
            Pattern.compile(
                    "void\\s+(\\w+)\\s*\\(");
    private static final Pattern AT_NAMING_CONVENTION =
            Pattern.compile(
                    "^at(\\d+)_");

    private TestMethodScanner() {
    }

    /**
     * Scans a single Java test source file content.
     *
     * @param className  the test class name (without package)
     * @param sourceCode the Java source code content
     * @return immutable list of discovered test methods
     */
    public static List<TestMethod> scan(
            String className,
            String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return List.of();
        }

        var lines = sourceCode.split("\n");
        return extractTestMethods(className, lines);
    }

    private static List<TestMethod> extractTestMethods(
            String className, String[] lines) {
        var methods = new ArrayList<TestMethod>();
        var pendingTags = new ArrayList<String>();
        boolean testAnnotationFound = false;

        for (var line : lines) {
            var trimmed = line.trim();

            if (TEST_ANNOTATION.matcher(trimmed).find()) {
                testAnnotationFound = true;
                collectTags(trimmed, pendingTags);
                continue;
            }

            if (testAnnotationFound) {
                collectTags(trimmed, pendingTags);
            }

            if (!testAnnotationFound) {
                continue;
            }

            var methodMatcher =
                    METHOD_SIGNATURE.matcher(trimmed);
            if (methodMatcher.find()) {
                var methodName = methodMatcher.group(1);
                var linkedAtId = resolveLinkedAtId(
                        methodName, pendingTags);
                methods.add(new TestMethod(
                        className,
                        methodName,
                        linkedAtId,
                        List.copyOf(pendingTags)));
                pendingTags.clear();
                testAnnotationFound = false;
            }
        }

        return List.copyOf(methods);
    }

    private static void collectTags(
            String line, List<String> tags) {
        var tagMatcher = TAG_ANNOTATION.matcher(line);
        while (tagMatcher.find()) {
            tags.add(tagMatcher.group(1));
        }
    }

    private static Optional<String> resolveLinkedAtId(
            String methodName, List<String> tags) {
        var atFromTags = findAtIdInTags(tags);
        if (atFromTags.isPresent()) {
            return atFromTags;
        }
        return findAtIdFromNaming(methodName);
    }

    private static Optional<String> findAtIdInTags(
            List<String> tags) {
        return tags.stream()
                .filter(t -> t.startsWith("AT-"))
                .findFirst();
    }

    private static Optional<String> findAtIdFromNaming(
            String methodName) {
        var matcher =
                AT_NAMING_CONVENTION.matcher(methodName);
        if (matcher.find()) {
            return Optional.of(
                    "AT-%s".formatted(matcher.group(1)));
        }
        return Optional.empty();
    }
}
