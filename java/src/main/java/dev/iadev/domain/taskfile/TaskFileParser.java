package dev.iadev.domain.taskfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Markdown task file into a {@link ParsedTaskFile} projection.
 *
 * <p>Stateless and thread-safe; no I/O. Callers read the file themselves and pass the raw
 * markdown content to {@link #parse(String)}. Validation is the concern of {@code TaskValidator}.</p>
 */
public final class TaskFileParser {

    private static final Pattern BOLD_FIELD = Pattern.compile(
            "^\\*\\*(ID|Story|Status):\\*\\*\\s+(.+?)\\s*$");
    private static final Pattern H2_HEADING = Pattern.compile(
            "^##\\s+(\\d+)\\.\\s+(.+?)\\s*$");
    private static final Pattern H3_HEADING = Pattern.compile(
            "^###\\s+(\\d+)\\.(\\d+)\\s+(.+?)\\s*$");
    private static final Pattern CHECKLIST_ITEM = Pattern.compile(
            "^-\\s+\\[([x ])\\]\\s+(.*)$");
    private static final Pattern TASK_ID_REF = Pattern.compile(
            "TASK-\\d{4}-\\d{4}-\\d{3}");
    private static final Pattern TABLE_ROW = Pattern.compile(
            "^\\|\\s*(.+?)\\s*\\|.*$");
    private static final Pattern TABLE_SEPARATOR = Pattern.compile(
            "^\\|[\\s:\\-|]+\\|?\\s*$");

    private TaskFileParser() {
        // static-only
    }

    public static ParsedTaskFile parse(String markdown) {
        Objects.requireNonNull(markdown, "markdown");
        String[] lines = markdown.split("\\r?\\n", -1);
        SectionIndex sections = indexSections(lines);
        HeaderFields headers = extractHeaders(lines);
        return buildParsed(lines, sections, headers);
    }

    private static SectionIndex indexSections(String[] lines) {
        SectionIndex idx = new SectionIndex();
        for (int i = 0; i < lines.length; i++) {
            Matcher h2 = H2_HEADING.matcher(lines[i]);
            if (h2.matches()) {
                idx.setH2(Integer.parseInt(h2.group(1)), i);
                continue;
            }
            Matcher h3 = H3_HEADING.matcher(lines[i]);
            if (h3.matches()) {
                int parent = Integer.parseInt(h3.group(1));
                int child = Integer.parseInt(h3.group(2));
                idx.setH3(parent, child, i);
            }
        }
        return idx;
    }

    private static HeaderFields extractHeaders(String[] lines) {
        HeaderFields fields = new HeaderFields();
        for (String line : lines) {
            Matcher m = BOLD_FIELD.matcher(line);
            if (!m.matches()) {
                continue;
            }
            String value = m.group(2).trim();
            switch (m.group(1)) {
                case "ID" -> fields.taskId = Optional.of(value);
                case "Story" -> fields.storyId = Optional.of(value);
                case "Status" -> fields.status = Optional.of(value);
                default -> throw new AssertionError("unreachable: regex restricts group to ID|Story|Status");
            }
        }
        return fields;
    }

    private static ParsedTaskFile buildParsed(
            String[] lines, SectionIndex sections, HeaderFields headers) {
        String objective = bodyBetween(lines, sections.h2(1), sections.nextAfterH2(1, lines.length));
        String inputs = bodyBetween(lines, sections.h3(2, 1), sections.nextAfterH3(2, 1, lines.length));
        String outputs = bodyBetween(lines, sections.h3(2, 2), sections.nextAfterH3(2, 2, lines.length));
        TestabilityBlock test = parseTestability(
                lines, sections.h3(2, 3), sections.nextAfterH3(2, 3, lines.length));
        List<String> dod = parseChecklist(
                lines, sections.h2(3), sections.nextAfterH2(3, lines.length));
        List<String> deps = parseDependencies(
                lines, sections.h2(4), sections.nextAfterH2(4, lines.length));
        return new ParsedTaskFile(
                headers.taskId, headers.storyId, headers.status,
                objective, inputs, outputs,
                test.kinds, test.references, dod, deps);
    }

    private static String bodyBetween(String[] lines, int fromHeading, int toExclusive) {
        if (fromHeading < 0) {
            return "";
        }
        int end = Math.min(toExclusive, lines.length);
        StringBuilder sb = new StringBuilder();
        for (int i = fromHeading + 1; i < end; i++) {
            sb.append(lines[i]).append('\n');
        }
        return sb.toString().strip();
    }

    private static TestabilityBlock parseTestability(
            String[] lines, int fromHeading, int toExclusive) {
        TestabilityBlock block = new TestabilityBlock();
        if (fromHeading < 0) {
            return block;
        }
        int end = Math.min(toExclusive, lines.length);
        for (int i = fromHeading + 1; i < end; i++) {
            Matcher m = CHECKLIST_ITEM.matcher(lines[i]);
            if (!m.matches() || !"x".equals(m.group(1))) {
                continue;
            }
            String body = m.group(2);
            matchKind(body).ifPresent(block.kinds::add);
            extractTaskRefs(body, block.references);
        }
        return block;
    }

    private static Optional<TestabilityKind> matchKind(String line) {
        String lower = line.toLowerCase();
        if (lower.startsWith("independentemente testável")) {
            return Optional.of(TestabilityKind.INDEPENDENT);
        }
        if (lower.startsWith("requer mock de")) {
            return Optional.of(TestabilityKind.REQUIRES_MOCK);
        }
        if (lower.startsWith("coalescível com")) {
            return Optional.of(TestabilityKind.COALESCED);
        }
        return Optional.empty();
    }

    private static void extractTaskRefs(String text, List<String> sink) {
        Matcher m = TASK_ID_REF.matcher(text);
        while (m.find()) {
            sink.add(m.group());
        }
    }

    private static List<String> parseChecklist(String[] lines, int fromHeading, int toExclusive) {
        List<String> items = new ArrayList<>();
        if (fromHeading < 0) {
            return items;
        }
        int end = Math.min(toExclusive, lines.length);
        for (int i = fromHeading + 1; i < end; i++) {
            if (CHECKLIST_ITEM.matcher(lines[i]).matches()) {
                items.add(lines[i].strip());
            }
        }
        return items;
    }

    private static List<String> parseDependencies(
            String[] lines, int fromHeading, int toExclusive) {
        List<String> deps = new ArrayList<>();
        if (fromHeading < 0) {
            return deps;
        }
        int end = Math.min(toExclusive, lines.length);
        for (int i = fromHeading + 1; i < end; i++) {
            if (TABLE_SEPARATOR.matcher(lines[i]).matches()) {
                continue;
            }
            Matcher row = TABLE_ROW.matcher(lines[i]);
            if (!row.matches()) {
                continue;
            }
            String firstCell = row.group(1).trim();
            Matcher ref = TASK_ID_REF.matcher(firstCell);
            if (ref.find()) {
                deps.add(ref.group());
            }
        }
        return deps;
    }

    private static final class HeaderFields {
        Optional<String> taskId = Optional.empty();
        Optional<String> storyId = Optional.empty();
        Optional<String> status = Optional.empty();
    }

    private static final class TestabilityBlock {
        final List<TestabilityKind> kinds = new ArrayList<>();
        final List<String> references = new ArrayList<>();
    }

    private static final class SectionIndex {
        private final java.util.Map<Integer, Integer> h2 = new java.util.HashMap<>();
        private final java.util.Map<Long, Integer> h3 = new java.util.HashMap<>();

        void setH2(int section, int line) {
            h2.putIfAbsent(section, line);
        }

        void setH3(int parent, int child, int line) {
            h3.putIfAbsent(h3key(parent, child), line);
        }

        int h2(int section) {
            return h2.getOrDefault(section, -1);
        }

        int h3(int parent, int child) {
            return h3.getOrDefault(h3key(parent, child), -1);
        }

        int nextAfterH2(int section, int totalLines) {
            return h2.entrySet().stream()
                    .filter(e -> e.getKey() > section)
                    .mapToInt(java.util.Map.Entry::getValue)
                    .min().orElse(totalLines);
        }

        int nextAfterH3(int parent, int child, int totalLines) {
            int sameParentNext = h3.entrySet().stream()
                    .filter(e -> parentOf(e.getKey()) == parent && childOf(e.getKey()) > child)
                    .mapToInt(java.util.Map.Entry::getValue)
                    .min().orElse(Integer.MAX_VALUE);
            int nextH2 = nextAfterH2(parent, totalLines);
            return Math.min(sameParentNext, nextH2);
        }

        private static long h3key(int parent, int child) {
            return ((long) parent << 32) | (child & 0xffffffffL);
        }

        private static int parentOf(long key) {
            return (int) (key >> 32);
        }

        private static int childOf(long key) {
            return (int) (key & 0xffffffffL);
        }
    }
}
