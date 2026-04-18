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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Application-layer orchestrator for {@code /x-parallel-eval}.
 *
 * <p>Loads {@link FileFootprint} blocks from story / task
 * planning artifacts, computes pairwise
 * {@link CollisionDetector} results grouped by phase (for
 * epic scope), and produces a serialization recommendation.</p>
 *
 * <p>Hexagonal discipline: no I/O happens in the domain
 * classes ({@link CollisionDetector},
 * {@link HotspotCatalog}); this class owns filesystem
 * access and delegates all classification decisions
 * downward.</p>
 */
public final class ParallelismEvaluator {

    private static final Pattern STORY_FOOTPRINT_HEADER =
            Pattern.compile(
                    "^##\\s+Story\\s+File\\s+Footprint\\s*$");
    private static final Pattern STORY_ID_PATTERN =
            Pattern.compile(
                    "^story-(\\d{4})-(\\d{4})\\.md$");
    private static final Pattern BLOCKED_BY_PATTERN =
            Pattern.compile(
                    "\\*\\*Blocked\\s+By\\*\\*"
                            + "\\s*\\|\\s*(.+?)\\s*\\|");
    private static final Pattern DEPS_SECTION_HEADER =
            Pattern.compile(
                    "^##\\s+1\\.?\\s*Depend[eê]ncias\\s*$");

    private final CollisionDetector detector;
    private final CollisionPolicy policy;

    public ParallelismEvaluator() {
        this(new CollisionDetector(),
                CollisionPolicy.EXCLUDE_SOFT);
    }

    public ParallelismEvaluator(
            CollisionDetector detector,
            CollisionPolicy policy) {
        this.detector = Objects.requireNonNull(
                detector, "detector");
        this.policy = Objects.requireNonNull(
                policy, "policy");
    }

    /** Immutable result of an evaluation invocation. */
    public record Report(
            String scope,
            int itemsAnalyzed,
            List<Collision> collisions,
            List<List<String>> phases,
            List<String> warnings,
            Map<String, List<String>> hotspotTouches) {

        public Report {
            Objects.requireNonNull(scope, "scope");
            collisions = List.copyOf(collisions);
            phases = phases.stream()
                    .map(List::copyOf)
                    .collect(Collectors.toUnmodifiableList());
            warnings = List.copyOf(warnings);
            hotspotTouches = Collections.unmodifiableMap(
                    new TreeMap<>(hotspotTouches));
        }

        public long hardCount() {
            return collisions.stream()
                    .filter(c -> c.category()
                            == CollisionCategory.HARD)
                    .count();
        }

        public long regenCount() {
            return collisions.stream()
                    .filter(c -> c.category()
                            == CollisionCategory.REGEN)
                    .count();
        }

        public long softCount() {
            return collisions.stream()
                    .filter(c -> c.category()
                            == CollisionCategory.SOFT)
                    .count();
        }

        public int exitCode() {
            if (hardCount() > 0 || regenCount() > 0) {
                return 2;
            }
            if (!warnings.isEmpty()) {
                return 1;
            }
            return 0;
        }
    }

    /**
     * Evaluate an epic directory. Groups stories into
     * topological phases derived from {@code Blocked By}
     * declarations and analyzes pairs within each phase.
     *
     * @param epicDir    path to the epic directory
     *                   (e.g., {@code plans/epic-0041})
     * @return aggregated report
     * @throws IOException on filesystem errors
     */
    public Report evaluateEpic(Path epicDir)
            throws IOException {
        Objects.requireNonNull(epicDir, "epicDir");
        Map<String, StoryNode> stories =
                loadStories(epicDir);
        List<List<String>> phases = topologicalPhases(stories);
        List<Collision> collisions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, List<String>> hotspotTouches =
                collectHotspotTouches(stories);

        for (Map.Entry<String, StoryNode> e :
                stories.entrySet()) {
            if (e.getValue().footprint.isEmpty()) {
                warnings.add("footprint missing for "
                        + e.getKey());
            }
        }

        for (List<String> phase : phases) {
            List<String> sorted = new ArrayList<>(phase);
            Collections.sort(sorted);
            for (int i = 0; i < sorted.size(); i++) {
                for (int j = i + 1;
                        j < sorted.size(); j++) {
                    String a = sorted.get(i);
                    String b = sorted.get(j);
                    detector.detect(
                                    a, stories.get(a).footprint,
                                    b, stories.get(b).footprint,
                                    policy)
                            .ifPresent(collisions::add);
                }
            }
        }

        collisions.sort((x, y) -> {
            int byA = x.a().compareTo(y.a());
            if (byA != 0) {
                return byA;
            }
            return x.b().compareTo(y.b());
        });

        return new Report(
                "epic",
                stories.size(),
                collisions,
                phases,
                warnings,
                hotspotTouches);
    }

    /**
     * Evaluate a pair of stories by ID. Reads their
     * planning artifacts (or story files) under
     * {@code epicDir} and returns a single-phase report.
     */
    public Report evaluateStoryPair(
            Path epicDir, String idA, String idB)
            throws IOException {
        Objects.requireNonNull(idA, "idA");
        Objects.requireNonNull(idB, "idB");
        Map<String, StoryNode> stories = loadStories(epicDir);
        StoryNode a = stories.getOrDefault(idA,
                new StoryNode(idA, FileFootprint.EMPTY,
                        List.of()));
        StoryNode b = stories.getOrDefault(idB,
                new StoryNode(idB, FileFootprint.EMPTY,
                        List.of()));
        List<Collision> collisions = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (a.footprint.isEmpty()) {
            warnings.add("footprint missing for " + idA);
        }
        if (b.footprint.isEmpty()) {
            warnings.add("footprint missing for " + idB);
        }
        detector.detect(idA, a.footprint,
                        idB, b.footprint, policy)
                .ifPresent(collisions::add);
        Map<String, List<String>> hotspotTouches =
                collectHotspotTouches(Map.of(idA, a, idB, b));
        List<List<String>> phases = List.of(
                List.of(idA, idB));
        return new Report("story",
                2, collisions, phases,
                warnings, hotspotTouches);
    }

    /** Internal story-node carrier. */
    record StoryNode(
            String id,
            FileFootprint footprint,
            List<String> blockedBy) {}

    private Map<String, StoryNode> loadStories(Path epicDir)
            throws IOException {
        Map<String, StoryNode> out = new LinkedHashMap<>();
        if (!Files.isDirectory(epicDir)) {
            return out;
        }
        List<Path> storyFiles = new ArrayList<>();
        try (DirectoryStream<Path> ds =
                     Files.newDirectoryStream(
                             epicDir, "story-*.md")) {
            for (Path p : ds) {
                storyFiles.add(p);
            }
        }
        Collections.sort(storyFiles);
        for (Path p : storyFiles) {
            String fname = p.getFileName().toString();
            Matcher m = STORY_ID_PATTERN.matcher(fname);
            if (!m.matches()) {
                continue;
            }
            String id = fname.substring(
                    0, fname.length() - 3);
            String body = Files.readString(p);
            FileFootprint fp =
                    parseStoryFootprint(body);
            List<String> blockedBy =
                    parseBlockedBy(body);
            out.put(id,
                    new StoryNode(id, fp, blockedBy));
        }
        return out;
    }

    static FileFootprint parseStoryFootprint(String body) {
        List<String> lines = body.lines().toList();
        int startIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (STORY_FOOTPRINT_HEADER.matcher(
                    lines.get(i)).matches()) {
                startIndex = i + 1;
                break;
            }
        }
        if (startIndex < 0) {
            FileFootprint fallback =
                    FileFootprintParser.parse(body);
            return fallback;
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

    static List<String> parseBlockedBy(String body) {
        List<String> lines = body.lines().toList();
        boolean inDeps = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (DEPS_SECTION_HEADER.matcher(line)
                    .matches()) {
                inDeps = true;
                continue;
            }
            if (!inDeps) {
                continue;
            }
            if (line.startsWith("## ")) {
                break;
            }
            String trimmed = line.trim();
            if (!trimmed.startsWith("|")) {
                continue;
            }
            String[] cells = trimmed.split("\\|", -1);
            if (cells.length < 3) {
                continue;
            }
            String first = cells[1].trim();
            String second = cells[2].trim();
            if (first.isEmpty()
                    || first.startsWith(":")
                    || first.startsWith("-")
                    || first.equalsIgnoreCase(
                            "Blocked By")) {
                continue;
            }
            return parseDepList(first);
        }
        return List.of();
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

    static List<List<String>> topologicalPhases(
            Map<String, StoryNode> stories) {
        Map<String, Set<String>> remaining = new HashMap<>();
        for (StoryNode n : stories.values()) {
            Set<String> deps = new TreeSet<>();
            for (String d : n.blockedBy) {
                if (stories.containsKey(d)) {
                    deps.add(d);
                }
            }
            remaining.put(n.id, deps);
        }
        List<List<String>> phases = new ArrayList<>();
        while (!remaining.isEmpty()) {
            List<String> wave = new ArrayList<>();
            for (Map.Entry<String, Set<String>> e :
                    remaining.entrySet()) {
                if (e.getValue().isEmpty()) {
                    wave.add(e.getKey());
                }
            }
            if (wave.isEmpty()) {
                List<String> leftover =
                        new ArrayList<>(
                                remaining.keySet());
                Collections.sort(leftover);
                phases.add(leftover);
                break;
            }
            Collections.sort(wave);
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

    private Map<String, List<String>> collectHotspotTouches(
            Map<String, StoryNode> stories) {
        HotspotCatalog catalog = new HotspotCatalog();
        Map<String, TreeSet<String>> acc = new TreeMap<>();
        for (StoryNode n : stories.values()) {
            TreeSet<String> paths = new TreeSet<>();
            paths.addAll(n.footprint.writes());
            paths.addAll(n.footprint.regens());
            for (String path : paths) {
                catalog.matchHotspot(path)
                        .ifPresent(hit -> acc.computeIfAbsent(
                                        hit,
                                        k -> new TreeSet<>())
                                .add(n.id));
            }
        }
        Map<String, List<String>> out = new TreeMap<>();
        for (Map.Entry<String, TreeSet<String>> e :
                acc.entrySet()) {
            if (e.getValue().size() >= 2) {
                out.put(e.getKey(),
                        List.copyOf(e.getValue()));
            }
        }
        return out;
    }

    // Intentionally kept for callers that want direct access.
    public Optional<Collision> detect(
            String idA, FileFootprint fpA,
            String idB, FileFootprint fpB) {
        return detector.detect(
                idA, fpA, idB, fpB, policy);
    }
}
