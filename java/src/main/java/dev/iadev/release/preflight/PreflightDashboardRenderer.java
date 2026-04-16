package dev.iadev.release.preflight;

import dev.iadev.release.BumpType;
import dev.iadev.release.integrity.CheckResult;
import dev.iadev.release.integrity.CheckStatus;
import dev.iadev.release.integrity.IntegrityReport;

import java.util.List;
import java.util.Objects;

/**
 * Pure renderer that composes the pre-flight dashboard text from
 * {@link DashboardData} (story-0039-0009, TASK-001/002).
 *
 * <p>The output is formatted as a compact dashboard and contains five
 * sections: Version, Commits, CHANGELOG preview, Integrity checks, and
 * Execution Plan. No I/O — the caller is responsible for printing.</p>
 */
public final class PreflightDashboardRenderer {

    /** Default number of CHANGELOG lines shown before truncation. */
    public static final int DEFAULT_CHANGELOG_LINES = 10;

    /** Minimum allowed value for changelog line limit. */
    public static final int MIN_CHANGELOG_LINES = 1;

    /** Maximum allowed value for changelog line limit (TASK-007 bounds). */
    public static final int MAX_CHANGELOG_LINES = 500;

    private PreflightDashboardRenderer() {
        throw new AssertionError("no instances");
    }

    /**
     * Renders the pre-flight dashboard using the default changelog truncation.
     *
     * @param data dashboard data; must not be null
     * @return rendered dashboard text
     */
    public static String render(DashboardData data) {
        return render(data, DEFAULT_CHANGELOG_LINES);
    }

    /**
     * Renders the pre-flight dashboard with explicit changelog line limit.
     *
     * @param data          dashboard data; must not be null
     * @param maxChangelog   max CHANGELOG lines to show; clamped to
     *                       [{@value MIN_CHANGELOG_LINES}..{@value MAX_CHANGELOG_LINES}]
     * @return rendered dashboard text
     */
    public static String render(DashboardData data, int maxChangelog) {
        Objects.requireNonNull(data, "data");
        int clampedMax = clampChangelogLines(maxChangelog);
        var sb = new StringBuilder(512);
        renderHeader(sb, data);
        renderVersionSection(sb, data);
        renderCommitSection(sb, data);
        renderChangelogSection(sb, data, clampedMax);
        renderIntegritySection(sb, data.integrityReport());
        renderExecutionPlan(sb, data);
        return sb.toString();
    }

    static int clampChangelogLines(int requested) {
        if (requested < MIN_CHANGELOG_LINES) {
            return MIN_CHANGELOG_LINES;
        }
        if (requested > MAX_CHANGELOG_LINES) {
            return MAX_CHANGELOG_LINES;
        }
        return requested;
    }

    private static void renderHeader(StringBuilder sb, DashboardData data) {
        sb.append("=== PRE-FLIGHT — release v")
          .append(sanitize(data.targetVersion().toString()))
          .append(" ===\n\n");
    }

    private static void renderVersionSection(StringBuilder sb,
                                             DashboardData data) {
        var bumpType = data.bumpType();
        String bumpLabel = bumpType.name()
                + " — "
                + (bumpType == BumpType.EXPLICIT
                        ? "explicit" : "auto");
        sb.append("Versao detectada:    ")
          .append(sanitize(data.targetVersion().toString()))
          .append(" (").append(bumpLabel).append(")\n");
        if (data.previousVersion().isPresent()) {
            sb.append("Ultima tag:          v")
              .append(sanitize(data.previousVersion().get().toString()))
              .append(" (").append(data.lastTagAgeDays())
              .append(" dias atras)\n");
        } else {
            sb.append("Ultima tag:          (nenhuma)\n");
        }
    }

    private static void renderCommitSection(StringBuilder sb,
                                            DashboardData data) {
        var cc = data.commitCounts();
        sb.append("Commits desde tag:   ")
          .append(cc.total())
          .append(" (")
          .append(cc.feat()).append(" feat, ")
          .append(cc.fix()).append(" fix, ")
          .append(cc.breaking()).append(" breaking, ")
          .append(cc.ignored()).append(" ignored)\n\n");
    }

    private static void renderChangelogSection(StringBuilder sb,
                                               DashboardData data,
                                               int maxLines) {
        List<String> lines = data.changelogLines();
        sb.append("CHANGELOG preview ([Unreleased]):\n");
        if (lines.isEmpty()) {
            sb.append("  (vazio)\n");
        } else {
            int shown = Math.min(lines.size(), maxLines);
            for (int i = 0; i < shown; i++) {
                sb.append("  ").append(sanitize(lines.get(i))).append('\n');
            }
            int omitted = lines.size() - shown;
            if (omitted > 0) {
                sb.append("  (").append(omitted)
                  .append(" linhas omitidas)\n");
            }
        }
        sb.append('\n');
    }

    private static void renderIntegritySection(StringBuilder sb,
                                               IntegrityReport report) {
        String statusIcon = switch (report.overallStatus()) {
            case PASS -> "PASS";
            case WARN -> "WARN";
            case FAIL -> "FAIL";
        };
        sb.append("Integrity checks: ").append(statusIcon).append('\n');
        for (CheckResult check : report.checks()) {
            String icon = checkIcon(check.status());
            sb.append("  ").append(icon).append(' ')
              .append(check.name()).append('\n');
        }
        sb.append('\n');
    }

    private static void renderExecutionPlan(StringBuilder sb,
                                            DashboardData data) {
        String version = sanitize(data.targetVersion().toString());
        String base = sanitize(data.baseBranch());
        sb.append("Plano de execucao:\n");
        sb.append("  1. Criar branch release/").append(version)
          .append(" from ").append(base).append('\n');
        sb.append("  2. Bump pom.xml -> ").append(version).append('\n');
        sb.append("  3. CHANGELOG: [Unreleased] -> [")
          .append(version).append("]\n");
        sb.append("  4. Commit + push\n");
        sb.append("  5. PR release/").append(version)
          .append(" -> main\n");
    }

    private static String checkIcon(CheckStatus status) {
        return switch (status) {
            case PASS -> "v";
            case WARN -> "~";
            case FAIL -> "x";
        };
    }

    /**
     * Strip ANSI escape sequences and control characters from terminal
     * output to prevent injection (TASK-007, OWASP A03/A05).
     */
    static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        var sb = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\u001b') {
                i = skipAnsiSequence(input, i);
            } else if (Character.isISOControl(c)
                    && c != '\n' && c != '\r' && c != '\t') {
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static int skipAnsiSequence(String s, int start) {
        int i = start + 1;
        if (i < s.length() && s.charAt(i) == '[') {
            i++;
            while (i < s.length()) {
                char c = s.charAt(i);
                i++;
                if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                    return i;
                }
            }
        }
        return i;
    }
}
