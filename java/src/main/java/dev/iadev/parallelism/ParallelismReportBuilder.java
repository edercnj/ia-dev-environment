package dev.iadev.parallelism;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Presentation-layer helper for {@code /x-parallel-eval}.
 *
 * <p>Builds the public {@link ParallelismEvaluator.Report}
 * record from already-computed collaborators:
 * {@link StoryNode}s, collisions, phase DAG. Aggregates
 * hotspot touches and serialization warnings.</p>
 *
 * <p>Pure: no filesystem I/O, no regex parsing. Exists to
 * keep {@link ParallelismEvaluator} focused on orchestration.</p>
 */
final class ParallelismReportBuilder {

    private final HotspotCatalog catalog;

    ParallelismReportBuilder() {
        this(new HotspotCatalog());
    }

    ParallelismReportBuilder(HotspotCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Assemble the epic-scope {@link ParallelismEvaluator.Report}.
     */
    ParallelismEvaluator.Report buildEpicReport(
            Map<String, StoryNode> stories,
            List<Collision> collisions,
            List<List<String>> phases) {
        List<String> warnings = buildMissingFootprintWarnings(
                stories.values());
        Map<String, List<String>> hotspotTouches =
                collectHotspotTouches(stories);
        return new ParallelismEvaluator.Report(
                "epic",
                stories.size(),
                collisions,
                phases,
                warnings,
                hotspotTouches);
    }

    /**
     * Assemble the story-pair-scope
     * {@link ParallelismEvaluator.Report}.
     */
    ParallelismEvaluator.Report buildStoryPairReport(
            StoryNode a,
            StoryNode b,
            List<Collision> collisions) {
        List<String> warnings = new ArrayList<>();
        if (a.footprint().isEmpty()) {
            warnings.add("footprint missing for " + a.id());
        }
        if (b.footprint().isEmpty()) {
            warnings.add("footprint missing for " + b.id());
        }
        Map<String, List<String>> hotspotTouches =
                collectHotspotTouches(Map.of(
                        a.id(), a, b.id(), b));
        List<List<String>> phases = List.of(
                List.of(a.id(), b.id()));
        return new ParallelismEvaluator.Report(
                "story",
                2,
                collisions,
                phases,
                warnings,
                hotspotTouches);
    }

    static List<String> buildMissingFootprintWarnings(
            Collection<StoryNode> stories) {
        List<String> warnings = new ArrayList<>();
        for (StoryNode n : stories) {
            if (n.footprint().isEmpty()) {
                warnings.add("footprint missing for "
                        + n.id());
            }
        }
        return warnings;
    }

    Map<String, List<String>> collectHotspotTouches(
            Map<String, StoryNode> stories) {
        Map<String, TreeSet<String>> acc = new TreeMap<>();
        for (StoryNode n : stories.values()) {
            TreeSet<String> paths = new TreeSet<>();
            paths.addAll(n.footprint().writes());
            paths.addAll(n.footprint().regens());
            for (String path : paths) {
                catalog.matchHotspot(path)
                        .ifPresent(hit -> acc.computeIfAbsent(
                                        hit,
                                        k -> new TreeSet<>())
                                .add(n.id()));
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
}
