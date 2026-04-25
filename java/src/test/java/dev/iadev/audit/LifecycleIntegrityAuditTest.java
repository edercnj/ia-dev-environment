package dev.iadev.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CI-blocking audit for RA9 planning artifact compliance
 * (RULE-006 EPIC-0056 — story-0056-0007).
 *
 * <p>Scans all {@code plans/epic-XXXX/} markdown files for
 * three RA9 rules:
 * <ul>
 *   <li>{@code RA9_SECTIONS_MISSING} — any of the 9 section
 *       headers absent.</li>
 *   <li>{@code RA9_RATIONALE_EMPTY} — section 8 present but
 *       empty / placeholder / TODO (Epic + Story only).</li>
 *   <li>{@code RA9_PACKAGES_MISSING} — section 2 present but
 *       all layers marked {@code —}.</li>
 * </ul>
 *
 * <p>Files matching patterns in
 * {@code audits/lifecycle-integrity-baseline.txt} (resource)
 * are grandfathered (epics 0001–0058) and skipped. Epic-0056
 * itself is the introduction of RA9, so its planning files
 * predate RA9 and are exempted. Epics 0057-0058 already
 * existed at the time RA9 was introduced and are likewise
 * exempt.</p>
 *
 * <p>Files containing {@code <!-- audit-exempt -->} are also
 * skipped.</p>
 */
@DisplayName("LifecycleIntegrityAuditTest — RA9 compliance")
class LifecycleIntegrityAuditTest {

    /**
     * Candidate locations for the {@code plans/} directory,
     * ordered by check priority. Tests can be invoked from
     * either the repo root (Maven via parent build) or the
     * {@code java/} subdir (direct {@code mvn -f java/pom.xml}).
     * Both must locate the same canonical {@code plans/}.
     */
    private static final List<Path> PLANS_CANDIDATES = List.of(
            Path.of("plans").toAbsolutePath().normalize(),
            Path.of("..").resolve("plans")
                    .toAbsolutePath().normalize());

    private static final String BASELINE_RESOURCE =
            "/audits/lifecycle-integrity-baseline.txt";

    private static final Set<String> PLANNING_ARTIFACT_PREFIXES =
            Set.of("epic-", "story-", "task-");

    private final Ra9SectionsChecker sectionsChecker =
            new Ra9SectionsChecker();
    private final Ra9RationaleChecker rationaleChecker =
            new Ra9RationaleChecker();
    private final Ra9PackagesChecker packagesChecker =
            new Ra9PackagesChecker();

    @Test
    @DisplayName("audit_planningArtifacts_noRa9Violations")
    void audit_planningArtifacts_noRa9Violations()
            throws IOException {
        Path plansRoot = resolvePlansRoot();

        Set<String> baselinePatterns = loadBaseline();
        List<String> allViolations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(plansRoot)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .filter(p -> isPlanningArtifact(p))
                    .filter(p -> !isBaselined(p, baselinePatterns))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(
                                    p, StandardCharsets.UTF_8);
                            String filename = p.toString();
                            allViolations.addAll(
                                    sectionsChecker.check(
                                            content, filename));
                            allViolations.addAll(
                                    rationaleChecker.check(
                                            content, filename));
                            allViolations.addAll(
                                    packagesChecker.check(
                                            content, filename));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }

        assertThat(allViolations)
                .as("RA9 audit found %d violation(s)."
                        + " Fix or add <!-- audit-exempt -->."
                        + "\nViolations:\n%s",
                        allViolations.size(),
                        String.join("\n", allViolations))
                .isEmpty();
    }

    /**
     * Resolves the {@code plans/} directory across both supported
     * working directories ({@code java/} subdir and repo root).
     * Fails loudly when the directory cannot be located so the
     * audit cannot silently skip in CI.
     */
    private Path resolvePlansRoot() {
        for (Path candidate : PLANS_CANDIDATES) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "[LifecycleIntegrityAuditTest] plans/ directory"
                + " not found in any candidate location: "
                + PLANS_CANDIDATES
                + ". The CI gate cannot run — investigate the"
                + " working directory setup.");
    }

    private boolean isPlanningArtifact(Path path) {
        String name = path.getFileName().toString();
        return PLANNING_ARTIFACT_PREFIXES.stream()
                .anyMatch(name::startsWith);
    }

    private boolean isBaselined(
            Path path, Set<String> patterns) {
        String normalized = path.toString()
                .replace('\\', '/');
        return patterns.stream()
                .anyMatch(pattern -> matchesGlob(
                        normalized, pattern));
    }

    /**
     * Glob matcher with strict path-segment boundaries.
     *
     * <p>{@code prefix/**} matches only paths that contain the
     * directory {@code prefix} as a complete path segment
     * (followed by {@code /}). Prevents over-matching:
     * {@code plans/epic-0001/**} no longer matches
     * {@code plans/epic-00010/**}.</p>
     */
    private boolean matchesGlob(
            String path, String pattern) {
        if (pattern.endsWith("/**")) {
            String dir = pattern.substring(
                    0, pattern.length() - 3);
            return path.contains("/" + dir + "/")
                    || path.startsWith(dir + "/");
        }
        return path.endsWith(pattern)
                || path.contains("/" + pattern);
    }

    private Set<String> loadBaseline() {
        try (InputStream is = LifecycleIntegrityAuditTest.class
                .getResourceAsStream(BASELINE_RESOURCE)) {
            if (is == null) {
                return Set.of();
            }
            String content = new String(
                    is.readAllBytes(), StandardCharsets.UTF_8);
            return content.lines()
                    .map(String::strip)
                    .filter(l -> !l.isEmpty()
                            && !l.startsWith("#"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            return Set.of();
        }
    }
}
