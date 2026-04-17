package dev.iadev.parallelism.cli;

import dev.iadev.parallelism.Collision;
import dev.iadev.parallelism.ParallelismEvaluator.Report;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders a {@link Report} as a small, deterministic JSON
 * document. Implemented by hand (no Jackson) to keep the
 * output stable byte-for-byte (RULE-008).
 */
public final class JsonRenderer {

    public String render(String scopeLabel, Report report) {
        Objects.requireNonNull(report, "report");
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"scopeLabel\": ")
                .append(quote(scopeLabel)).append(",\n");
        sb.append("  \"scope\": ")
                .append(quote(report.scope())).append(",\n");
        sb.append("  \"itemsAnalyzed\": ")
                .append(report.itemsAnalyzed()).append(",\n");
        sb.append("  \"hardCount\": ")
                .append(report.hardCount()).append(",\n");
        sb.append("  \"regenCount\": ")
                .append(report.regenCount()).append(",\n");
        sb.append("  \"softCount\": ")
                .append(report.softCount()).append(",\n");
        sb.append("  \"exitCode\": ")
                .append(report.exitCode()).append(",\n");
        appendCollisions(sb, report.collisions());
        sb.append(",\n");
        appendPhases(sb, report.phases());
        sb.append(",\n");
        appendHotspots(sb, report.hotspotTouches());
        sb.append(",\n");
        appendStringList(sb, "warnings",
                report.warnings(), "  ");
        sb.append('\n').append("}\n");
        return sb.toString();
    }

    private static void appendCollisions(
            StringBuilder sb, List<Collision> list) {
        sb.append("  \"collisions\": [");
        if (list.isEmpty()) {
            sb.append("]");
            return;
        }
        sb.append('\n');
        for (int i = 0; i < list.size(); i++) {
            Collision c = list.get(i);
            sb.append("    {\n");
            sb.append("      \"a\": ").append(quote(c.a()))
                    .append(",\n");
            sb.append("      \"b\": ").append(quote(c.b()))
                    .append(",\n");
            sb.append("      \"category\": ")
                    .append(quote(c.category().name()))
                    .append(",\n");
            sb.append("      \"reason\": ")
                    .append(c.reason() == null
                            ? "null"
                            : quote(c.reason()))
                    .append(",\n");
            sb.append("      \"sharedPaths\": ")
                    .append(jsonArray(c.sharedPaths()))
                    .append('\n');
            sb.append("    }");
            if (i < list.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ]");
    }

    private static void appendPhases(
            StringBuilder sb,
            List<List<String>> phases) {
        sb.append("  \"phases\": [");
        if (phases.isEmpty()) {
            sb.append("]");
            return;
        }
        sb.append('\n');
        for (int i = 0; i < phases.size(); i++) {
            sb.append("    ")
                    .append(jsonArray(phases.get(i)));
            if (i < phases.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ]");
    }

    private static void appendHotspots(
            StringBuilder sb,
            Map<String, List<String>> hotspots) {
        sb.append("  \"hotspotTouches\": {");
        if (hotspots.isEmpty()) {
            sb.append("}");
            return;
        }
        sb.append('\n');
        int idx = 0;
        for (Map.Entry<String, List<String>> e :
                hotspots.entrySet()) {
            sb.append("    ").append(quote(e.getKey()))
                    .append(": ")
                    .append(jsonArray(e.getValue()));
            if (idx < hotspots.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
            idx++;
        }
        sb.append("  }");
    }

    private static void appendStringList(
            StringBuilder sb, String key,
            List<String> items, String indent) {
        sb.append(indent).append('"').append(key)
                .append("\": ")
                .append(jsonArray(items));
    }

    private static String jsonArray(
            java.util.Collection<String> items) {
        if (items.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        int n = items.size();
        for (String s : items) {
            sb.append(quote(s));
            if (i < n - 1) {
                sb.append(", ");
            }
            i++;
        }
        sb.append(']');
        return sb.toString();
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format(
                                "\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
