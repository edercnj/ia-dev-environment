package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repo-level smoke test for the EPIC-0057 evidence-table expansion.
 *
 * <p>Reads the canonical reference golden at
 * {@code java/src/test/resources/golden/java-spring/.claude/rules/24-execution-integrity.md}
 * — the committed reference for the most exercised profile — and
 * checks the Mandatory Evidence Artifacts table carries the
 * EPIC-0057 expansion. Complements
 * {@link Rule24EvidenceTableExpansionTest}, which exercises the
 * pipeline end-to-end. This smoke version is independent of
 * {@code @TempDir} and runs as a fast guard against drift between
 * the source-of-truth rule and the committed reference golden.</p>
 *
 * <p>Note: the runtime {@code .claude/} at the repo root is
 * gitignored (regenerated locally), so a smoke test must read from
 * the committed golden tree to remain CI-stable.</p>
 */
@DisplayName("Rule24EvidenceTableSmokeTest — reference golden has 11 entries")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX path resolution; matches sibling smoke tests.")
class Rule24EvidenceTableSmokeTest {

    private static final String REFERENCE_GOLDEN_PATH =
            "java/src/test/resources/golden/java-spring/"
                    + ".claude/rules/24-execution-integrity.md";

    @Test
    @DisplayName("reference golden Rule 24 has ≥11 evidence-table rows")
    void smoke_referenceGolden_hasAtLeastElevenRows() throws IOException {
        Path rule = repoRoot().resolve(REFERENCE_GOLDEN_PATH);
        assertThat(rule)
                .as("reference golden Rule 24 must exist")
                .exists();

        String body = Files.readString(rule, StandardCharsets.UTF_8);
        long rowCount = body.lines()
                .filter(l -> l.startsWith("| `x-"))
                .count();

        assertThat(rowCount)
                .as("evidence table must have ≥11 data rows; found %d",
                        rowCount)
                .isGreaterThanOrEqualTo(11);
    }

    @Test
    @DisplayName("reference golden references both x-pr-watch-ci and x-dependency-audit")
    void smoke_referenceGolden_referencesNewSubSkills() throws IOException {
        Path rule = repoRoot().resolve(REFERENCE_GOLDEN_PATH);
        String body = Files.readString(rule, StandardCharsets.UTF_8);

        assertThat(body)
                .as("reference golden must list x-pr-watch-ci")
                .contains("`x-pr-watch-ci`");
        assertThat(body)
                .as("reference golden must list x-dependency-audit")
                .contains("`x-dependency-audit`");
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java")
                ? cwd.getParent()
                : cwd;
    }
}
