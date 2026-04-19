package dev.iadev.parallelism;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filesystem adapter for {@code /x-parallel-eval}.
 *
 * <p>Reads story planning artifacts from an epic directory,
 * extracts the {@code ## File Footprint} (or {@code ## Story
 * File Footprint}) block, and parses {@code Blocked By}
 * declarations into a {@link StoryNode}.</p>
 *
 * <p>Owns ALL filesystem I/O for the parallelism pipeline —
 * {@link ParallelismEvaluator} itself stays pure.</p>
 */
final class FootprintLoader {

    private static final Pattern STORY_FOOTPRINT_HEADER =
            Pattern.compile(
                    "^##\\s+Story\\s+File\\s+Footprint\\s*$");
    private static final Pattern STORY_ID_PATTERN =
            Pattern.compile(
                    "^story-(\\d{4})-(\\d{4})\\.md$");
    private static final Pattern DEPS_SECTION_HEADER =
            Pattern.compile(
                    "^##\\s+1\\.?\\s*Depend[eê]ncias\\s*$");

    FootprintLoader() {
        // package-private constructor
    }

    /**
     * Load every {@code story-XXXX-YYYY.md} under {@code epicDir}
     * into a {@link StoryNode}. Returns an empty map if the
     * directory does not exist.
     */
    Map<String, StoryNode> loadStories(Path epicDir)
            throws IOException {
        Map<String, StoryNode> out = new LinkedHashMap<>();
        if (!Files.isDirectory(epicDir)) {
            return out;
        }
        List<Path> storyFiles = collectStoryFiles(epicDir);
        for (Path p : storyFiles) {
            String fname = p.getFileName().toString();
            Matcher m = STORY_ID_PATTERN.matcher(fname);
            if (!m.matches()) {
                continue;
            }
            String id = fname.substring(
                    0, fname.length() - 3);
            String body = Files.readString(p);
            FileFootprint fp = parseStoryFootprint(body);
            List<String> blockedBy = parseBlockedBy(body);
            out.put(id,
                    new StoryNode(id, fp, blockedBy));
        }
        return out;
    }

    private static List<Path> collectStoryFiles(Path epicDir)
            throws IOException {
        List<Path> storyFiles = new ArrayList<>();
        try (DirectoryStream<Path> ds =
                     Files.newDirectoryStream(
                             epicDir, "story-*.md")) {
            for (Path p : ds) {
                storyFiles.add(p);
            }
        }
        Collections.sort(storyFiles);
        return storyFiles;
    }

    static FileFootprint parseStoryFootprint(String body) {
        List<String> lines = body.lines().toList();
        int startIndex = locateStoryFootprintHeader(lines);
        if (startIndex < 0) {
            return FileFootprintParser.parse(body);
        }
        StringBuilder slice = new StringBuilder();
        slice.append("## File Footprint\n");
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("## ")
                    && !line.startsWith("### ")) {
                break;
            }
            slice.append(line).append('\n');
        }
        return FileFootprintParser.parse(slice.toString());
    }

    private static int locateStoryFootprintHeader(
            List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (STORY_FOOTPRINT_HEADER.matcher(
                    lines.get(i)).matches()) {
                return i + 1;
            }
        }
        return -1;
    }

    static List<String> parseBlockedBy(String body) {
        List<String> lines = body.lines().toList();
        boolean inDeps = false;
        for (String line : lines) {
            if (DEPS_SECTION_HEADER.matcher(line).matches()) {
                inDeps = true;
                continue;
            }
            if (!inDeps) {
                continue;
            }
            if (line.startsWith("## ")) {
                break;
            }
            List<String> deps = extractDepsFromRow(line);
            if (deps != null) {
                return deps;
            }
        }
        return List.of();
    }

    private static List<String> extractDepsFromRow(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("|")) {
            return null;
        }
        String[] cells = trimmed.split("\\|", -1);
        if (cells.length < 3) {
            return null;
        }
        String first = cells[1].trim();
        if (first.isEmpty()
                || first.startsWith(":")
                || first.startsWith("-")
                || first.equalsIgnoreCase("Blocked By")) {
            return null;
        }
        return parseDepList(first);
    }

    /**
     * Produce the topological phase-wave decomposition of a
     * story graph using Kahn's algorithm. Each wave contains
     * stories independent of each other (all their
     * dependencies have been emitted earlier). Cycles emit a
     * final wave containing the remaining nodes.
     */
    static List<List<String>> topologicalPhases(
            Map<String, StoryNode> stories) {
        Map<String, Set<String>> remaining = new HashMap<>();
        for (StoryNode n : stories.values()) {
            Set<String> deps = new TreeSet<>();
            for (String d : n.blockedBy()) {
                if (stories.containsKey(d)) {
                    deps.add(d);
                }
            }
            remaining.put(n.id(), deps);
        }
        List<List<String>> phases = new ArrayList<>();
        while (!remaining.isEmpty()) {
            List<String> wave = nextWave(remaining);
            if (wave.isEmpty()) {
                List<String> leftover =
                        new ArrayList<>(remaining.keySet());
                Collections.sort(leftover);
                phases.add(leftover);
                break;
            }
            phases.add(wave);
            for (String done : wave) {
                remaining.remove(done);
            }
            for (Set<String> deps : remaining.values()) {
                deps.removeAll(wave);
            }
        }
        return phases;
    }

    private static List<String> nextWave(
            Map<String, Set<String>> remaining) {
        List<String> wave = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e :
                remaining.entrySet()) {
            if (e.getValue().isEmpty()) {
                wave.add(e.getKey());
            }
        }
        Collections.sort(wave);
        return wave;
    }

    private static List<String> parseDepList(String cell) {
        if (cell == null || cell.isBlank()
                || "—".equals(cell.trim())
                || "-".equals(cell.trim())) {
            return List.of();
        }
        String[] parts = cell.split("[,;]");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.matches("story-\\d{4}-\\d{4}")) {
                out.add(t);
            }
        }
        return out;
    }
}
