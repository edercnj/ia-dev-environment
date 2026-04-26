package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Rule 24 "Mandatory Evidence Artifacts" table contains
 * all 11 sub-skills required by EPIC-0057 (story-0057-0001).
 *
 * <p>The table moved from 5 to 11 entries to close the EPIC-0053
 * post-mortem blind spot where {@code x-pr-watch-ci} was silently
 * skipped without any enforcement layer detecting the omission.</p>
 *
 * <p>Acceptance criteria: every regenerated profile golden contains
 * the 11 canonical entries; build fails if the table regresses.</p>
 */
@DisplayName("Rule24EvidenceTableExpansionTest — Rule 24 §32-42 has 11 entries")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "Pipeline regen relies on POSIX execute bit;"
                + " mirrors Epic0055FoundationSmokeTest gating.")
class Rule24EvidenceTableExpansionTest extends SmokeTestBase {

    private static final List<String> EXPECTED_SUBSKILLS = List.of(
            "x-internal-story-verify",
            "x-review",
            "x-review-pr",
            "x-internal-story-report",
            "x-arch-plan",
            "x-pr-watch-ci",
            "x-pr-create",
            "x-test-tdd",
            "x-git-commit",
            "x-dependency-audit",
            "x-threat-model");

    private static final String TABLE_HEADER =
            "| Sub-skill | Artifact path | Enforced by |";

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("Rule 24 evidence table contains all 11 expected sub-skills")
    void rule24_evidenceTable_containsExpectedSubSkills(String profile)
            throws IOException {
        runPipeline(profile);
        Path rule = getOutputDir(profile)
                .resolve(".claude/rules/24-execution-integrity.md");

        assertThat(rule)
                .as("profile %s: Rule 24 file must exist", profile)
                .exists();

        String body = Files.readString(rule, StandardCharsets.UTF_8);

        assertThat(body)
                .as("profile %s: table header must be present", profile)
                .contains(TABLE_HEADER);

        for (String subskill : EXPECTED_SUBSKILLS) {
            assertThat(body)
                    .as("profile %s: table must reference '%s'",
                            profile, subskill)
                    .contains("`" + subskill);
        }
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("Rule 24 evidence table has at least 11 data rows")
    void rule24_evidenceTable_hasAtLeastElevenRows(String profile)
            throws IOException {
        runPipeline(profile);
        Path rule = getOutputDir(profile)
                .resolve(".claude/rules/24-execution-integrity.md");

        String body = Files.readString(rule, StandardCharsets.UTF_8);
        long rowCount = countTableRows(body);

        assertThat(rowCount)
                .as("profile %s: table must have ≥11 data rows; found %d",
                        profile, rowCount)
                .isGreaterThanOrEqualTo(11);
    }

    private long countTableRows(String body) {
        int tableStart = body.indexOf(TABLE_HEADER);
        if (tableStart < 0) {
            return 0;
        }
        int separatorEnd = body.indexOf('\n',
                body.indexOf("| :---", tableStart)) + 1;
        int tableEnd = body.indexOf("\n\n", separatorEnd);
        if (tableEnd < 0) {
            tableEnd = body.length();
        }
        String tableBody = body.substring(separatorEnd, tableEnd);
        return tableBody.lines()
                .filter(line -> line.startsWith("| `"))
                .count();
    }
}
