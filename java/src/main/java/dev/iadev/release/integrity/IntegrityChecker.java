package dev.iadev.release.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-domain integrity checker for pre-release drift detection (Story 0039-0003).
 *
 * <p>All check methods are pure functions over in-memory strings; file I/O is delegated
 * to an adapter (see {@link RepoFileReader}). This keeps the domain free of filesystem,
 * XML parsing, and path-traversal concerns (ADR-0004, Rule 04 domain purity).</p>
 *
 * <p>Three checks are implemented:
 * <ol>
 *   <li>{@link #checkChangelogUnreleased(String)} — CHANGELOG {@code [Unreleased]} non-empty</li>
 *   <li>{@link #checkVersionAlignment(String, Map)} — version strings aligned across files</li>
 *   <li>{@link #checkNoNewTodos(String)} — no new {@code TODO}/{@code FIXME} in diff (WARN)</li>
 * </ol>
 * </p>
 */
public final class IntegrityChecker {

    public static final String CHECK_CHANGELOG_UNRELEASED = "changelog_unreleased_non_empty";
    public static final String CHECK_VERSION_ALIGNMENT = "version_alignment";
    public static final String CHECK_NO_NEW_TODOS = "no_new_todos";

    // [Unreleased] section between '## [Unreleased]' and the next '## ' header (or EOF).
    private static final Pattern UNRELEASED_SECTION = Pattern.compile(
            "(?s)##\\s*\\[Unreleased\\](.*?)(?=^##\\s|\\z)",
            Pattern.MULTILINE);
    // Entry line inside a section (bullet or numbered-list item).
    private static final Pattern ENTRY_LINE = Pattern.compile(
            "^(?:-\\s+\\S|\\*\\s+\\S|\\d+\\.\\s+\\S)", Pattern.MULTILINE);

    private IntegrityChecker() {
        throw new AssertionError("no instances");
    }

    /**
     * Run all three checks and aggregate into an {@link IntegrityReport}.
     *
     * @param changelogContent raw CHANGELOG.md content (or null/blank when file absent)
     * @param versionedFiles   map of {@code path -> content} for version-bearing files;
     *                         {@code pom.xml} is used as the reference version source
     * @param diffContent      unified {@code git log -p} output since last tag (nullable)
     * @return aggregated report
     */
    public static IntegrityReport run(String changelogContent,
                                      Map<String, String> versionedFiles,
                                      String diffContent) {
        Objects.requireNonNull(versionedFiles, "versionedFiles");
        List<CheckResult> results = new ArrayList<>(3);
        results.add(checkChangelogUnreleased(changelogContent));
        String targetVersion = VersionExtractor.extractPomVersion(versionedFiles);
        results.add(checkVersionAlignment(targetVersion, versionedFiles));
        results.add(checkNoNewTodos(diffContent == null ? "" : diffContent));
        return IntegrityReport.aggregate(results);
    }

    /**
     * CHANGELOG {@code [Unreleased]} non-empty check.
     *
     * <p>PASS when the section contains at least one entry line (bullet or numbered item).
     * Sub-section headers ({@code ###}) alone do not count as entries.</p>
     */
    public static CheckResult checkChangelogUnreleased(String changelog) {
        if (changelog == null || changelog.isBlank()) {
            return CheckResult.fail(CHECK_CHANGELOG_UNRELEASED, List.of("CHANGELOG.md"));
        }
        Matcher m = UNRELEASED_SECTION.matcher(changelog);
        if (!m.find()) {
            return CheckResult.fail(CHECK_CHANGELOG_UNRELEASED, List.of("CHANGELOG.md"));
        }
        String section = m.group(1);
        if (!ENTRY_LINE.matcher(section).find()) {
            return CheckResult.fail(CHECK_CHANGELOG_UNRELEASED, List.of("CHANGELOG.md"));
        }
        return CheckResult.pass(CHECK_CHANGELOG_UNRELEASED);
    }

    /**
     * Version alignment across files.
     *
     * <p>For each file in {@code files}, the first semver-shaped match in the content is
     * compared against {@code targetVersion}. Files with no semver match are ignored.
     * Files where the first semver does not equal the normalized target are flagged.</p>
     *
     * @param targetVersion reference version (e.g., from pom.xml); null or blank returns PASS
     * @param files         map of {@code path -> content}
     */
    public static CheckResult checkVersionAlignment(String targetVersion, Map<String, String> files) {
        Objects.requireNonNull(files, "files");
        if (targetVersion == null || targetVersion.isBlank()) {
            return CheckResult.pass(CHECK_VERSION_ALIGNMENT);
        }
        String normalizedTarget = VersionExtractor.normalize(targetVersion);
        List<String> divergent = findDivergentVersionFiles(normalizedTarget, files);
        if (divergent.isEmpty()) {
            return CheckResult.pass(CHECK_VERSION_ALIGNMENT);
        }
        return CheckResult.fail(CHECK_VERSION_ALIGNMENT, divergent);
    }

    /**
     * No-new-TODOs check over a unified-diff payload.
     *
     * <p>Returns {@link CheckStatus#WARN} (never FAIL) when the diff introduces new
     * {@code TODO}/{@code FIXME}/{@code HACK}/{@code XXX} markers in .java/.md/.peb
     * files outside test paths. Documented {@code TODO(...)} intents are excluded.</p>
     */
    public static CheckResult checkNoNewTodos(String diff) {
        if (diff == null || diff.isBlank()) {
            return CheckResult.pass(CHECK_NO_NEW_TODOS);
        }
        List<String> hits = DiffTodoScanner.scan(diff);
        if (hits.isEmpty()) {
            return CheckResult.pass(CHECK_NO_NEW_TODOS);
        }
        return CheckResult.warn(CHECK_NO_NEW_TODOS, hits);
    }

    private static List<String> findDivergentVersionFiles(String normalizedTarget,
                                                          Map<String, String> files) {
        List<String> divergent = new ArrayList<>();
        for (Map.Entry<String, String> entry : files.entrySet()) {
            String path = entry.getKey();
            String content = entry.getValue();
            if (content == null || content.isBlank()) {
                continue;
            }
            String divergenceRef = firstDivergentSemver(normalizedTarget, path, content);
            if (divergenceRef != null) {
                divergent.add(divergenceRef);
            }
        }
        return divergent;
    }

    private static String firstDivergentSemver(String normalizedTarget, String path, String content) {
        Matcher m = VersionExtractor.SEMVER.matcher(content);
        if (m.find()) {
            String found = m.group(1);
            if (!found.equals(normalizedTarget)) {
                int line = VersionExtractor.lineNumberOfOffset(content, m.start());
                return path + ":" + line;
            }
        }
        return null;
    }
}
