package dev.iadev.release.integrity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper for {@link IntegrityChecker#checkNoNewTodos(String)}.
 *
 * <p>Scans unified-diff content for added lines introducing new {@code TODO}, {@code FIXME},
 * {@code HACK}, or {@code XXX} markers in .java/.md/.peb files, excluding test paths and
 * documented {@code TODO(...)} intents.</p>
 */
final class DiffTodoScanner {

    // TODO/FIXME/HACK/XXX on diff-added lines (leading '+', not '+++'); exclude TODO(...).
    private static final Pattern NEW_TODO = Pattern.compile(
            "^\\+(?!\\+\\+)(?:(?!\\+\\+\\+).)*\\b(?:TODO(?!\\()|FIXME|HACK|XXX)\\b",
            Pattern.MULTILINE);
    private static final Pattern DIFF_FILE_HEADER = Pattern.compile(
            "^\\+\\+\\+\\s+b/(\\S+)", Pattern.MULTILINE);

    private static final List<String> SCAN_EXTENSIONS = List.of(".java", ".md", ".peb");
    private static final List<String> TEST_PATH_FRAGMENTS = List.of("/test/", "/tests/", "src/test/");

    private DiffTodoScanner() {
        throw new AssertionError("no instances");
    }

    static List<String> scan(String diff) {
        Map<Integer, String> fileByOffset = indexDiffSections(diff);
        Matcher tm = NEW_TODO.matcher(diff);
        Set<String> results = new LinkedHashSet<>();
        while (tm.find()) {
            String path = fileAtOffset(fileByOffset, tm.start());
            if (path != null && matchesExtension(path) && !isTestPath(path)) {
                results.add(path);
            }
        }
        return new ArrayList<>(results);
    }

    private static Map<Integer, String> indexDiffSections(String diff) {
        Map<Integer, String> fileByOffset = new LinkedHashMap<>();
        Matcher fh = DIFF_FILE_HEADER.matcher(diff);
        while (fh.find()) {
            fileByOffset.put(fh.start(), fh.group(1));
        }
        return fileByOffset;
    }

    private static String fileAtOffset(Map<Integer, String> fileByOffset, int offset) {
        String current = null;
        for (Map.Entry<Integer, String> e : fileByOffset.entrySet()) {
            if (e.getKey() > offset) {
                break;
            }
            current = e.getValue();
        }
        return current;
    }

    private static boolean matchesExtension(String path) {
        for (String ext : SCAN_EXTENSIONS) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTestPath(String path) {
        String normalized = path.replace('\\', '/');
        for (String fragment : TEST_PATH_FRAGMENTS) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
