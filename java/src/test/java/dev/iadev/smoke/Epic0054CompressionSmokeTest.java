package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for EPIC-0054 (ADR-0012 Slim-by-default rollout).
 *
 * <p>Validates the slim-rewrite contract for orchestrator skills:
 * each SKILL.md body must be ≤ 500 lines (hard ADR-0012 limit)
 * with a non-empty {@code references/full-protocol.md} sibling.
 * Assertions grow incrementally as stories are merged:
 * story-0054-0001 covers PR-domain (x-pr-fix-epic, x-pr-merge-train);
 * subsequent stories add their respective skills.</p>
 *
 * @see Epic0047CompressionSmokeTest
 * @see SmokeTestBase
 */
@DisplayName("Epic0054CompressionSmokeTest — ADR-0012 slim rollout")
class Epic0054CompressionSmokeTest extends SmokeTestBase {

    /**
     * ADR-0012 hard limit: SKILL.md body must be ≤ 500 lines
     * when a non-empty {@code references/full-protocol.md} sibling exists.
     * Target is ≤ 250 lines; hard limit is 500 (WARN tier).
     */
    private static final int SLIM_HARD_LIMIT = 500;

    /** Mandatory section headers per ADR-0012 canonical contract. */
    private static final List<String> REQUIRED_SLIM_HEADERS = List.of(
            "## Triggers",
            "## Parameters",
            "## Output Contract",
            "## Error Envelope",
            "## Full Protocol");

    /**
     * PR-domain skills from story-0054-0001.
     * x-pr-fix-epic: 1297 → ≤ 250 lines
     * x-pr-merge-train: 873 → ≤ 250 lines
     */
    private static final List<String> STORY_0001_PR_DOMAIN_SKILLS = List.of(
            "x-pr-fix-epic",
            "x-pr-merge-train");

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_prDomainSkillsSlimWithFullProtocol — "
                    + "x-pr-fix-epic and x-pr-merge-train have "
                    + "SKILL.md ≤ 500 lines, all 5 canonical headers, "
                    + "and a non-empty references/full-protocol.md "
                    + "(story-0054-0001 ADR-0012 invariant)")
    void smoke_prDomainSkillsSlimWithFullProtocol(String profile)
            throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile).resolve(".claude/skills");

        for (String leaf : STORY_0001_PR_DOMAIN_SKILLS) {
            Path skillDir = skillsDir.resolve(leaf);
            Path skillMd = skillDir.resolve("SKILL.md");

            assertThat(Files.isRegularFile(skillMd))
                    .as("profile %s: %s/SKILL.md must exist", profile, leaf)
                    .isTrue();

            String content = Files.readString(skillMd, StandardCharsets.UTF_8);
            int lineCount = content.split("\n", -1).length;

            assertThat(lineCount)
                    .as("profile %s: %s/SKILL.md must be ≤ %d lines "
                                    + "(ADR-0012 hard limit); got %d lines",
                            profile, leaf, SLIM_HARD_LIMIT, lineCount)
                    .isLessThanOrEqualTo(SLIM_HARD_LIMIT);

            for (String header : REQUIRED_SLIM_HEADERS) {
                assertThat(content)
                        .as("profile %s: %s/SKILL.md must contain "
                                        + "mandatory slim header '%s' (ADR-0012)",
                                profile, leaf, header)
                        .contains(header);
            }

            Path fullProtocol = skillDir.resolve("references/full-protocol.md");
            assertThat(Files.isRegularFile(fullProtocol))
                    .as("profile %s: %s/references/full-protocol.md must exist "
                                    + "(ADR-0012 carve-out invariant)",
                            profile, leaf)
                    .isTrue();
            assertThat(Files.size(fullProtocol))
                    .as("profile %s: %s/references/full-protocol.md must "
                                    + "be non-empty",
                            profile, leaf)
                    .isGreaterThan(0L);
        }
    }
}
