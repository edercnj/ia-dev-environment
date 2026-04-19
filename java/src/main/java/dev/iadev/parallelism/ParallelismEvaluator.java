package dev.iadev.parallelism;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Application-layer orchestrator for {@code /x-parallel-eval}.
 *
 * <p>Coordinates {@link FootprintLoader} (filesystem I/O),
 * {@link CollisionDetector} (pure collision logic), and
 * {@link ParallelismReportBuilder} (report assembly) to produce
 * a phase-aware serialization recommendation for an epic or
 * story pair.</p>
 *
 * <p>Hexagonal discipline: this class has zero filesystem I/O
 * and zero regex parsing — all file reads happen in
 * {@link FootprintLoader}; all presentation logic sits in
 * {@link ParallelismReportBuilder}.</p>
 */
public final class ParallelismEvaluator {

    private final FootprintLoader loader;
    private final CollisionDetector detector;
    private final CollisionPolicy policy;
    private final ParallelismReportBuilder reportBuilder;

    public ParallelismEvaluator() {
        this(new CollisionDetector(),
                CollisionPolicy.EXCLUDE_SOFT);
    }

    public ParallelismEvaluator(
            CollisionDetector detector,
            CollisionPolicy policy) {
        this(new FootprintLoader(),
                Objects.requireNonNull(detector, "detector"),
                Objects.requireNonNull(policy, "policy"),
                new ParallelismReportBuilder());
    }

    ParallelismEvaluator(
            FootprintLoader loader,
            CollisionDetector detector,
            CollisionPolicy policy,
            ParallelismReportBuilder reportBuilder) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.detector = Objects.requireNonNull(
                detector, "detector");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.reportBuilder = Objects.requireNonNull(
                reportBuilder, "reportBuilder");
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
     * @param epicDir path to the epic directory
     *                (e.g., {@code plans/epic-0041})
     * @return aggregated report
     * @throws IOException on filesystem errors
     */
    public Report evaluateEpic(Path epicDir)
            throws IOException {
        Objects.requireNonNull(epicDir, "epicDir");
        Map<String, StoryNode> stories =
                loader.loadStories(epicDir);
        List<List<String>> phases =
                FootprintLoader.topologicalPhases(stories);
        List<Collision> collisions =
                detectPhaseCollisions(stories, phases);
        return reportBuilder.buildEpicReport(
                stories, collisions, phases);
    }

    /**
     * Evaluate a pair of stories by ID. Reads their planning
     * artifacts under {@code epicDir} and returns a
     * single-phase report.
     */
    public Report evaluateStoryPair(
            Path epicDir, String idA, String idB)
            throws IOException {
        Objects.requireNonNull(idA, "idA");
        Objects.requireNonNull(idB, "idB");
        Map<String, StoryNode> stories =
                loader.loadStories(epicDir);
        StoryNode a = stories.getOrDefault(idA,
                new StoryNode(idA, FileFootprint.EMPTY,
                        List.of()));
        StoryNode b = stories.getOrDefault(idB,
                new StoryNode(idB, FileFootprint.EMPTY,
                        List.of()));
        List<Collision> collisions = new ArrayList<>();
        detector.detect(idA, a.footprint(),
                        idB, b.footprint(), policy)
                .ifPresent(collisions::add);
        return reportBuilder.buildStoryPairReport(
                a, b, collisions);
    }

    // Intentionally kept for callers that want direct access.
    public Optional<Collision> detect(
            String idA, FileFootprint fpA,
            String idB, FileFootprint fpB) {
        return detector.detect(
                idA, fpA, idB, fpB, policy);
    }

    private List<Collision> detectPhaseCollisions(
            Map<String, StoryNode> stories,
            List<List<String>> phases) {
        List<Collision> collisions = new ArrayList<>();
        for (List<String> phase : phases) {
            List<String> sorted = new ArrayList<>(phase);
            Collections.sort(sorted);
            for (int i = 0; i < sorted.size(); i++) {
                for (int j = i + 1; j < sorted.size(); j++) {
                    String a = sorted.get(i);
                    String b = sorted.get(j);
                    detector.detect(
                                    a, stories.get(a).footprint(),
                                    b, stories.get(b).footprint(),
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
        return collisions;
    }

}
