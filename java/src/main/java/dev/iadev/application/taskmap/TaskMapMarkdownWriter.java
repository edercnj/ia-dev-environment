package dev.iadev.application.taskmap;

import dev.iadev.domain.taskmap.Edge;
import dev.iadev.domain.taskmap.TaskGraph;
import dev.iadev.domain.taskmap.TaskNode;
import dev.iadev.domain.taskmap.Wave;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serialises a {@link TaskGraph} as a {@code task-implementation-map-STORY-XXXX-YYYY.md}
 * document per the schema in {@code plans/epic-0038/schemas/task-implementation-map-schema.md}.
 *
 * <p>Output is deterministic — sections appear in fixed order, nodes are emitted by
 * primary TASK-ID, and edges are sorted by {@code (from, to)}. Two calls with the same
 * input produce byte-for-byte identical output (idempotency contract §6 of the schema).</p>
 */
public final class TaskMapMarkdownWriter {

    private TaskMapMarkdownWriter() {
        // static-only
    }

    public static String write(String storyId, TaskGraph graph) {
        Objects.requireNonNull(storyId, "storyId");
        Objects.requireNonNull(graph, "graph");
        if (storyId.isBlank()) {
            throw new IllegalArgumentException("storyId must not be blank");
        }
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, storyId);
        appendDependencyGraph(sb, graph);
        appendExecutionOrder(sb, graph);
        appendCoalescedGroups(sb, graph);
        appendParallelismAnalysis(sb, graph);
        return sb.toString();
    }

    private static void appendHeader(StringBuilder sb, String storyId) {
        sb.append("# Task Implementation Map — ").append(storyId).append("\n\n");
    }

    private static void appendDependencyGraph(StringBuilder sb, TaskGraph graph) {
        sb.append("## Dependency Graph\n\n");
        sb.append("```mermaid\n");
        sb.append("graph TD\n");
        for (TaskNode n : graph.nodes()) {
            sb.append("    ").append(nodeMermaidId(n))
                    .append("[\"").append(escapeMermaidLabel(nodeLabel(n)))
                    .append("\"]\n");
        }
        for (Edge e : sortedEdges(graph.edges())) {
            sb.append("    ").append(escapeMermaidId(e.from()))
                    .append(" --> ").append(escapeMermaidId(e.to())).append("\n");
        }
        sb.append("```\n\n");
    }

    private static void appendExecutionOrder(StringBuilder sb, TaskGraph graph) {
        sb.append("## Execution Order\n\n");
        sb.append("| Wave | Tasks (parallelisable) | Blocks |\n");
        sb.append("| :--- | :--- | :--- |\n");
        for (Wave w : graph.waves()) {
            String tasks = w.nodes().stream().map(TaskMapMarkdownWriter::nodeLabel)
                    .collect(Collectors.joining(", "));
            String blocks = blocksOf(w, graph);
            sb.append("| ").append(w.ordinal()).append(" | ")
                    .append(tasks).append(" | ").append(blocks).append(" |\n");
        }
        sb.append("\n");
    }

    private static void appendCoalescedGroups(StringBuilder sb, TaskGraph graph) {
        sb.append("## Coalesced Groups\n\n");
        if (graph.coalescedGroups().isEmpty()) {
            sb.append("—\n\n");
            return;
        }
        for (Set<String> group : graph.coalescedGroups()) {
            String members = group.stream().sorted().collect(Collectors.joining(" + "));
            sb.append("- (").append(members)
                    .append(") — coalesced per RULE-TF-04 (mutual COALESCED declaration)\n");
        }
        sb.append("\n");
    }

    private static void appendParallelismAnalysis(StringBuilder sb, TaskGraph graph) {
        sb.append("## Parallelism Analysis\n\n");
        sb.append("- Total tasks: ").append(graph.totalTasks()).append("\n");
        sb.append("- Number of waves: ").append(graph.waves().size()).append("\n");
        sb.append("- Largest wave size: ").append(graph.largestWaveSize()).append("\n");
        sb.append("- Estimated speedup vs sequential: ")
                .append(formatSpeedup(graph.estimatedSpeedup())).append("\n");
    }

    private static String nodeLabel(TaskNode n) {
        return n.taskIds().stream().sorted().collect(Collectors.joining(", "));
    }

    private static String nodeMermaidId(TaskNode n) {
        return escapeMermaidId(n.taskIds().stream().sorted().findFirst().orElseThrow());
    }

    private static String escapeMermaidId(String id) {
        return id.replace("-", "_");
    }

    private static String escapeMermaidLabel(String label) {
        return label.replace("\"", "&quot;");
    }

    private static List<Edge> sortedEdges(Set<Edge> edges) {
        return edges.stream()
                .sorted(Comparator.<Edge, String>comparing(Edge::from)
                        .thenComparing(Edge::to))
                .toList();
    }

    private static String blocksOf(Wave wave, TaskGraph graph) {
        Set<String> waveKeys = wave.nodes().stream()
                .map(n -> n.taskIds().stream().sorted().findFirst().orElseThrow())
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        List<String> blocks = graph.edges().stream()
                .filter(e -> waveKeys.contains(e.from()))
                .map(Edge::to)
                .distinct().sorted().toList();
        return blocks.isEmpty() ? "—" : String.join(", ", blocks);
    }

    private static String formatSpeedup(double s) {
        return String.format(Locale.ROOT, "%.2f", s);
    }
}
