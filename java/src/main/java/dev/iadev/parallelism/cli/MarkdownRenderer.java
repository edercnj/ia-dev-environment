package dev.iadev.parallelism.cli;

import dev.iadev.parallelism.Collision;
import dev.iadev.parallelism.ParallelismEvaluator.Report;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders a {@link Report} into the canonical Markdown
 * output shape documented in the {@code /x-parallel-eval}
 * skill contract (story-0041-0004 §3.2).
 *
 * <p>The renderer is deterministic (RULE-008): identical
 * inputs produce byte-identical outputs. It is intentionally
 * I/O-free — callers pass the result to
 * {@link ParallelEvalCli} for file or stdout writing.</p>
 */
public final class MarkdownRenderer {

    public String render(String scopeLabel, Report report) {
        Objects.requireNonNull(report, "report");
        StringBuilder sb = new StringBuilder();
        sb.append("# Parallelism Evaluation — ")
                .append(scopeLabel).append('\n')
                .append('\n');
        sb.append("**Scope:** ").append(report.scope())
                .append(" | **Items analyzed:** ")
                .append(report.itemsAnalyzed())
                .append(" | **Conflicts:** ")
                .append(report.hardCount()).append(" hard, ")
                .append(report.regenCount()).append(" regen, ")
                .append(report.softCount()).append(" soft\n")
                .append('\n');
        renderCollisionMatrix(sb, report.collisions());
        sb.append('\n');
        renderGroups(sb, report);
        sb.append('\n');
        renderHotspots(sb, report.hotspotTouches());
        if (!report.warnings().isEmpty()) {
            sb.append('\n');
            sb.append("## Warnings\n");
            for (String w : report.warnings()) {
                sb.append("- ").append(w).append('\n');
            }
        }
        return sb.toString();
    }

    private static void renderCollisionMatrix(
            StringBuilder sb,
            List<Collision> collisions) {
        sb.append("## Collision Matrix\n");
        sb.append("| A | B | Category | Shared paths |\n");
        sb.append("| :--- | :--- | :--- | :--- |\n");
        if (collisions.isEmpty()) {
            sb.append(
                    "| — | — | none | — |\n");
            return;
        }
        for (Collision c : collisions) {
            sb.append("| ").append(c.a())
                    .append(" | ").append(c.b())
                    .append(" | ")
                    .append(categoryLabel(c))
                    .append(" | ")
                    .append(joinPaths(c))
                    .append(" |\n");
        }
    }

    private static String categoryLabel(Collision c) {
        String cat = c.category().name().toLowerCase();
        if (c.reason() != null && !c.reason().isBlank()) {
            return cat + " (" + c.reason() + ")";
        }
        return cat;
    }

    private static String joinPaths(Collision c) {
        if (c.sharedPaths().isEmpty()) {
            return "—";
        }
        return String.join(", ", c.sharedPaths());
    }

    private static void renderGroups(
            StringBuilder sb, Report report) {
        sb.append("## Recommended Serialization Groups\n");
        List<List<String>> phases = report.phases();
        if (phases.isEmpty()) {
            sb.append("- (no items)\n");
            return;
        }
        int gnum = 0;
        for (List<String> phase : phases) {
            gnum++;
            boolean hasConflict =
                    phaseHasConflict(phase,
                            report.collisions());
            if (hasConflict) {
                sb.append("- **Group ").append(gnum)
                        .append(" (serialize):** ")
                        .append(String.join(" → ", phase))
                        .append(" (conflicts detected — "
                                + "see Collision Matrix)\n");
            } else {
                sb.append("- **Group ").append(gnum)
                        .append(" (parallel):** ")
                        .append(String.join(", ", phase))
                        .append(" (no conflicts)\n");
            }
        }
    }

    private static boolean phaseHasConflict(
            List<String> phase,
            List<Collision> collisions) {
        for (Collision c : collisions) {
            if (phase.contains(c.a())
                    && phase.contains(c.b())
                    && c.category()
                            != dev.iadev.parallelism
                                    .CollisionCategory.SOFT) {
                return true;
            }
        }
        return false;
    }

    private static void renderHotspots(
            StringBuilder sb,
            Map<String, List<String>> hotspots) {
        sb.append("## Hotspot Touches\n");
        if (hotspots.isEmpty()) {
            sb.append("- (none)\n");
            return;
        }
        for (Map.Entry<String, List<String>> e :
                hotspots.entrySet()) {
            sb.append("- `").append(e.getKey())
                    .append("` touched by: ")
                    .append(String.join(", ", e.getValue()))
                    .append('\n');
        }
    }
}
