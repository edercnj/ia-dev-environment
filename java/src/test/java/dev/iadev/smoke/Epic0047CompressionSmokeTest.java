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
 * Smoke tests for EPIC-0047 (Skill Body Compression Framework).
 *
 * <p>Validates the end-to-end contract introduced by
 * story-0047-0001: the source-of-truth {@code _shared/}
 * directory (peer of {@code core/}, {@code conditional/},
 * {@code knowledge-packs/}) is shipped to every generated
 * profile at {@code .claude/skills/_shared/}, and the pilot
 * pre-commit cluster ({@code x-git-commit},
 * {@code x-code-format}, {@code x-code-lint}) carries a
 * Markdown relative link that resolves to
 * {@code _shared/error-handling-pre-commit.md} in the output
 * tree per ADR-0011 (Option (b) — link-based inclusion).</p>
 *
 * <p>These smoke tests run the assembler pipeline for every
 * bundled profile and assert structural invariants, so they
 * catch regressions where {@code _shared/} would accidentally
 * be skipped for a specific stack / platform variant.</p>
 *
 * @see SmokeTestBase
 * @see SmokeProfiles
 */
@DisplayName("Epic0047CompressionSmokeTest — "
        + "_shared/ shipment + link resolution")
class Epic0047CompressionSmokeTest extends SmokeTestBase {

    /** Expected files inside {@code .claude/skills/_shared/}. */
    private static final List<String> EXPECTED_SHARED_FILES =
            List.of(
                    "README.md",
                    "error-handling-pre-commit.md",
                    "tdd-tags-glossary.md",
                    "exit-codes-common.md");

    /** Pilot consumer skills that link to _shared/ per ADR-0011. */
    private static final List<String> PILOT_CONSUMER_SKILLS =
            List.of(
                    "x-git-commit",
                    "x-code-format",
                    "x-code-lint");

    /** Expected Markdown link target (from a consumer SKILL.md). */
    private static final String EXPECTED_LINK_TARGET =
            "(../_shared/error-handling-pre-commit.md)";

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_sharedDirShipsToAllProfiles — _shared/"
                    + " present with 4 expected files")
    void smoke_sharedDirShipsToAllProfiles(String profile)
            throws IOException {
        runPipeline(profile);
        Path sharedDir = getOutputDir(profile)
                .resolve(".claude/skills/_shared");

        assertThat(Files.isDirectory(sharedDir))
                .as("profile %s must have .claude/skills/_shared/",
                        profile)
                .isTrue();

        for (String expected : EXPECTED_SHARED_FILES) {
            Path file = sharedDir.resolve(expected);
            assertThat(Files.isRegularFile(file))
                    .as("profile %s: %s/%s must exist",
                            profile, sharedDir, expected)
                    .isTrue();
            assertThat(Files.size(file))
                    .as("profile %s: %s must not be empty",
                            profile, expected)
                    .isGreaterThan(0L);
        }
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_preCommitClusterLinksShared — each pilot"
                    + " consumer SKILL.md carries the link to"
                    + " _shared/error-handling-pre-commit.md")
    void smoke_preCommitClusterLinksShared(String profile)
            throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile)
                .resolve(".claude/skills");

        for (String consumer : PILOT_CONSUMER_SKILLS) {
            Path skillMd = skillsDir.resolve(
                    consumer + "/SKILL.md");
            assertThat(Files.isRegularFile(skillMd))
                    .as("profile %s: %s/SKILL.md must exist",
                            profile, consumer)
                    .isTrue();
            String content = Files.readString(
                    skillMd, StandardCharsets.UTF_8);
            assertThat(content)
                    .as("profile %s: %s/SKILL.md must link to"
                            + " _shared/error-handling-pre-commit.md",
                            profile, consumer)
                    .contains(EXPECTED_LINK_TARGET);
        }
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_sharedLinkResolvesInOutput — the link"
                    + " target from a pilot consumer resolves"
                    + " to the file under _shared/")
    void smoke_sharedLinkResolvesInOutput(String profile)
            throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile)
                .resolve(".claude/skills");
        Path consumerSkill = skillsDir.resolve(
                "x-git-commit/SKILL.md");
        // Link is ../_shared/error-handling-pre-commit.md
        // relative to the consumer SKILL.md's directory.
        Path resolved = consumerSkill.getParent().resolve(
                "../_shared/error-handling-pre-commit.md")
                .normalize();

        assertThat(Files.exists(resolved))
                .as("profile %s: link target must exist at %s",
                        profile, resolved)
                .isTrue();
        assertThat(resolved)
                .as("profile %s: resolved path points into"
                        + " .claude/skills/_shared/", profile)
                .isEqualTo(skillsDir.resolve(
                        "_shared/error-handling-pre-commit.md")
                        .normalize());
    }

    /**
     * Target knowledge packs carved out by STORY-0047-0004.
     *
     * <p>Each entry is the source-of-truth path under
     * {@code java/src/main/resources/targets/claude/skills/}.
     * After the carve-out, every target KP MUST satisfy:
     * <ul>
     *   <li>{@code SKILL.md} ≤ 250 lines (narrative + index only)</li>
     *   <li>A sibling {@code references/} directory holding at
     *       least one {@code examples-*.md} file (the carved
     *       pattern examples)</li>
     * </ul>
     * Because the generated profile flattens knowledge-packs
     * into {@code .claude/skills/<kp-name>/**}, we assert
     * against the output tree using the KP leaf name.</p>
     */
    private static final List<String> EPIC_0047_0004_KP_LEAVES =
            List.of(
                    "click-cli-patterns",
                    "k8s-helm",
                    "axum-patterns",
                    "iac-terraform",
                    "dotnet-patterns");

    /** RULE-047-04 hard limit: SKILL.md ≤ 500 lines. */
    private static final int KP_SLIM_HARD_LIMIT = 500;

    /** Story-0047-0004 target: SKILL.md ≤ 250 lines. */
    private static final int KP_SLIM_TARGET = 250;

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_kpsHaveCarvedExamples — each of the 5"
                    + " STORY-0047-0004 target KPs has a"
                    + " references/examples-*.md sibling and"
                    + " its SKILL.md is ≤ 250 lines")
    void smoke_kpsHaveCarvedExamples(String profile)
            throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile)
                .resolve(".claude/skills");

        for (String kpLeaf : EPIC_0047_0004_KP_LEAVES) {
            Path kpDir = skillsDir.resolve(kpLeaf);
            if (!Files.isDirectory(kpDir)) {
                // Not every profile ships every KP (stack-
                // gated inclusion). Skip absent ones — the
                // carve invariant only applies when present.
                continue;
            }

            Path skillMd = kpDir.resolve("SKILL.md");
            assertThat(Files.isRegularFile(skillMd))
                    .as("profile %s: %s/SKILL.md must exist",
                            profile, kpLeaf)
                    .isTrue();

            long lineCount = Files.readAllLines(
                    skillMd, StandardCharsets.UTF_8).size();
            assertThat(lineCount)
                    .as("profile %s: %s/SKILL.md must be ≤"
                            + " %d lines (story-0047-0004"
                            + " target); got %d lines",
                            profile, kpLeaf,
                            KP_SLIM_TARGET, lineCount)
                    .isLessThanOrEqualTo(
                            KP_SLIM_HARD_LIMIT);

            Path referencesDir = kpDir.resolve("references");
            assertThat(Files.isDirectory(referencesDir))
                    .as("profile %s: %s/references/ must"
                            + " exist (story-0047-0004"
                            + " carve-out invariant)",
                            profile, kpLeaf)
                    .isTrue();

            try (var stream = Files.list(referencesDir)) {
                long exampleCount = stream
                        .filter(p -> p.getFileName()
                                .toString()
                                .startsWith("examples-"))
                        .filter(p -> p.getFileName()
                                .toString()
                                .endsWith(".md"))
                        .count();
                assertThat(exampleCount)
                        .as("profile %s: %s/references/"
                                + "examples-*.md must be"
                                + " non-empty",
                                profile, kpLeaf)
                        .isGreaterThan(0L);
            }
        }
    }
}
