package dev.iadev.parallelism;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Parses the {@code ## File Footprint} block emitted by x-task-plan Phase 4.5.
 *
 * <p>Grammar (informal):</p>
 * <pre>
 *   ## File Footprint
 *
 *   ### write:
 *   - path1
 *   - path2
 *
 *   ### read:
 *   - path3
 *
 *   ### regen:
 *   - path4
 * </pre>
 *
 * <p>Sub-sections are optional and may appear in any order. Empty sub-sections
 * (no bullet lines) are tolerated. A plan that predates the structured block
 * (RULE-006 backward compatibility) yields {@link FileFootprint#EMPTY} with
 * a single warning log line — never an exception.</p>
 */
public final class FileFootprintParser {

    /** Marker used by story-0041-0002 AC-4 backward-compat test. */
    static final String WARN_MISSING = "footprint ausente";

    private static final Logger LOG =
            Logger.getLogger(FileFootprintParser.class.getName());

    private static final String HEADER = "## File Footprint";
    private static final String SUB_HEADER_PREFIX = "### ";

    private enum Section { WRITE, READ, REGEN, NONE }

    private FileFootprintParser() {
        // utility
    }

    /**
     * Parse the footprint block from a plan's markdown body.
     *
     * @param markdown the full plan body (may be empty, may lack the block)
     * @return structured footprint; {@link FileFootprint#EMPTY} when absent
     */
    public static FileFootprint parse(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            LOG.warning(WARN_MISSING);
            return FileFootprint.EMPTY;
        }
        List<String> lines = markdown.lines().toList();
        int start = locateHeader(lines);
        if (start < 0) {
            LOG.warning(WARN_MISSING);
            return FileFootprint.EMPTY;
        }
        return collect(lines, start + 1);
    }

    private static int locateHeader(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (HEADER.equals(lines.get(i).trim())) {
                return i;
            }
        }
        return -1;
    }

    private static FileFootprint collect(List<String> lines, int from) {
        Set<String> writes = new HashSet<>();
        Set<String> reads = new HashSet<>();
        Set<String> regens = new HashSet<>();
        Section current = Section.NONE;
        for (int i = from; i < lines.size(); i++) {
            String raw = lines.get(i);
            String trimmed = raw.trim();
            if (isNextTopLevelHeader(trimmed)) {
                break;
            }
            if (trimmed.startsWith(SUB_HEADER_PREFIX)) {
                current = classifySubHeader(trimmed);
                continue;
            }
            Optional<String> path = extractBulletPath(trimmed);
            if (path.isEmpty() || current == Section.NONE) {
                continue;
            }
            appendPath(writes, reads, regens, current, path.get());
        }
        return new FileFootprint(writes, reads, regens);
    }

    private static boolean isNextTopLevelHeader(String trimmed) {
        return trimmed.startsWith("## ") && !trimmed.startsWith(SUB_HEADER_PREFIX);
    }

    private static Section classifySubHeader(String trimmed) {
        String label = trimmed.substring(SUB_HEADER_PREFIX.length())
                .replace(":", "").trim().toLowerCase();
        return switch (label) {
            case "write", "writes" -> Section.WRITE;
            case "read", "reads" -> Section.READ;
            case "regen", "regens" -> Section.REGEN;
            default -> Section.NONE;
        };
    }

    private static Optional<String> extractBulletPath(String trimmed) {
        if (!trimmed.startsWith("- ")) {
            return Optional.empty();
        }
        String body = trimmed.substring(2).trim();
        if (body.startsWith("`") && body.endsWith("`") && body.length() >= 2) {
            body = body.substring(1, body.length() - 1).trim();
        }
        return body.isEmpty() ? Optional.empty() : Optional.of(body);
    }

    private static void appendPath(
            Set<String> writes,
            Set<String> reads,
            Set<String> regens,
            Section current,
            String path) {
        switch (current) {
            case WRITE -> writes.add(path);
            case READ -> reads.add(path);
            case REGEN -> regens.add(path);
            default -> { /* NONE is unreachable here */ }
        }
    }
}
