package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Final smoke test for EPIC-0039 story-0039-0015 — validates the
 * consolidated output of the golden regeneration pass across all 17
 * profiles (plus the two {@code platform-claude-code} variants) after
 * all 14 preceding stories merged.
 *
 * <p>This smoke test exists to give EPIC-0039 a dedicated "canary" that
 * fails fast if any of the consolidated outputs drift, independently of
 * the fine-grained golden tests that run per-profile. It covers the four
 * deliverables enumerated in the story's §3.4-3.5 and §7 Gherkin
 * acceptance criteria:</p>
 *
 * <ol>
 *   <li>All 17 profiles plus the {@code go-gin/platform-claude-code}
 *       and {@code java-spring/platform-claude-code} variants ship the
 *       new {@code x-release} references directory with
 *       {@code interactive-flow-walkthrough.md}.</li>
 *   <li>All covered profiles and variants ship the three new plan
 *       templates ({@code _TEMPLATE-EPIC.md},
 *       {@code _TEMPLATE-STORY.md},
 *       {@code _TEMPLATE-IMPLEMENTATION-MAP.md}).</li>
 *   <li>The {@code x-release} SKILL.md in every covered profile /
 *       variant mentions all five epic-wide feature keywords
 *       (auto-detect, smart resume, telemetry, Phase 13, pre-flight).</li>
 *   <li>The CHANGELOG {@code [Unreleased]} section narrates every
 *       EPIC-0039 story that produced changelog-worthy changes —
 *       {@code story-0039-0001} and {@code story-0039-0003} through
 *       {@code story-0039-0015}. Story {@code story-0039-0002} is
 *       intentionally absent: it was a non-functional refactor with
 *       no user-visible surface (see CHANGELOG for rationale).</li>
 * </ol>
 *
 * <p>All reads use UTF-8 and strictly relative paths; no mutation is
 * performed.</p>
 */
@DisplayName("EPIC-0039 final — smoke (story-0039-0015)")
class Epic0039FinalSmokeTest {

    private static final List<String> PROFILES = List.of(
            "go-gin",
            "java-quarkus",
            "java-spring",
            "java-spring-clickhouse",
            "java-spring-cqrs-es",
            "java-spring-elasticsearch",
            "java-spring-event-driven",
            "java-spring-fintech-pci",
            "java-spring-hexagonal",
            "java-spring-neo4j",
            "kotlin-ktor",
            "python-click-cli",
            "python-fastapi",
            "python-fastapi-timescale",
            "rust-axum",
            "typescript-commander-cli",
            "typescript-nestjs");

    private static final List<String> PLATFORM_CLAUDE_CODE_VARIANTS =
            List.of(
                    "go-gin/platform-claude-code",
                    "java-spring/platform-claude-code");

    private static List<String> allCoverageTargets() {
        List<String> targets = new ArrayList<>(
                PROFILES.size() + PLATFORM_CLAUDE_CODE_VARIANTS.size());
        targets.addAll(PROFILES);
        targets.addAll(PLATFORM_CLAUDE_CODE_VARIANTS);
        return targets;
    }

    private static final List<String> NEW_TEMPLATES = List.of(
            "_TEMPLATE-EPIC.md",
            "_TEMPLATE-STORY.md",
            "_TEMPLATE-IMPLEMENTATION-MAP.md");

    private static final List<String> SKILL_FEATURE_MARKERS = List.of(
            "auto-detect",
            "Smart Resume",
            "telemetry",
            "Phase 13",
            "PRE-FLIGHT");

    private static final List<String> CHANGELOG_STORY_MARKERS = List.of(
            "story-0039-0001",
            "story-0039-0003",
            "story-0039-0004",
            "story-0039-0005",
            "story-0039-0006",
            "story-0039-0007",
            "story-0039-0008",
            "story-0039-0009",
            "story-0039-0010",
            "story-0039-0011",
            "story-0039-0012",
            "story-0039-0013",
            "story-0039-0014",
            "story-0039-0015");

    @Test
    @DisplayName(
            "all 17 profiles + 2 variants ship x-release walkthrough")
    void smoke_allProfilesHaveWalkthroughReference() {
        Path goldenRoot = goldenRoot();
        for (String profile : allCoverageTargets()) {
            Path walkthrough = goldenRoot.resolve(profile)
                    .resolve(".claude/skills/x-release/references"
                            + "/interactive-flow-walkthrough.md");
            assertThat(walkthrough)
                    .as("walkthrough missing in profile %s", profile)
                    .exists()
                    .isRegularFile();
            long size = sizeOf(walkthrough);
            assertThat(size)
                    .as("walkthrough unexpectedly small in %s", profile)
                    .isGreaterThan(2000L);
        }
    }

    @Test
    @DisplayName(
            "all 17 profiles + 2 variants ship 3 new plan templates")
    void smoke_allProfilesHaveNewTemplates() {
        Path goldenRoot = goldenRoot();
        for (String profile : allCoverageTargets()) {
            for (String template : NEW_TEMPLATES) {
                Path path = goldenRoot.resolve(profile)
                        .resolve(".claude/templates")
                        .resolve(template);
                assertThat(path)
                        .as("template %s missing in profile %s",
                                template, profile)
                        .exists()
                        .isRegularFile();
            }
        }
    }

    @Test
    @DisplayName(
            "all 17 profiles + 2 variants x-release SKILL.md "
                    + "mention 5 features")
    void smoke_allProfilesSkillMentionFeatures() {
        Path goldenRoot = goldenRoot();
        for (String profile : allCoverageTargets()) {
            Path skill = goldenRoot.resolve(profile)
                    .resolve(".claude/skills/x-release/SKILL.md");
            assertThat(skill)
                    .as("x-release SKILL missing in %s", profile)
                    .exists();
            String content = readUtf8(skill);
            for (String marker : SKILL_FEATURE_MARKERS) {
                assertThat(content)
                        .as("feature '%s' missing in %s SKILL.md",
                                marker, profile)
                        .containsIgnoringCase(marker);
            }
        }
    }

    @Test
    @DisplayName(
            "CHANGELOG [Unreleased] narrates S01 + S03..S15")
    void smoke_changelogCoversAllStories() {
        Path repoRoot = repoRoot();
        Path changelog = repoRoot.resolve("CHANGELOG.md");
        assertThat(changelog).exists().isRegularFile();
        String content = readUtf8(changelog);
        String unreleased = extractUnreleasedSection(content);
        assertThat(unreleased)
                .as("CHANGELOG [Unreleased] section appears empty")
                .isNotBlank();
        for (String marker : CHANGELOG_STORY_MARKERS) {
            assertThat(unreleased)
                    .as("CHANGELOG [Unreleased] missing %s", marker)
                    .contains(marker);
        }
    }

    @Test
    @DisplayName(
            "goldens ship 3 new templates via PlanTemplatesAssembler")
    void smoke_goldensShipNewTemplatesForEveryProfile() {
        Path goldenRoot = goldenRoot();
        for (String profile : allCoverageTargets()) {
            Path templatesDir = goldenRoot.resolve(profile)
                    .resolve(".claude/templates");
            assertThat(templatesDir)
                    .as("templates dir missing for %s", profile)
                    .isDirectory();
            for (String template : NEW_TEMPLATES) {
                Path path = templatesDir.resolve(template);
                long size = sizeOf(path);
                assertThat(size)
                        .as("template %s empty in profile %s",
                                template, profile)
                        .isPositive();
            }
        }
    }

    // ----- helpers -----

    private static Path goldenRoot() {
        return repoRoot()
                .resolve("java/src/test/resources/golden");
    }

    private static Path repoRoot() {
        Path workingDir = Path.of("").toAbsolutePath();
        Path candidate = workingDir;
        while (candidate != null) {
            if (Files.isDirectory(candidate.resolve(
                    "java/src/test/resources/golden"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException(
                "repo root not found from " + workingDir);
    }

    private static String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "cannot read " + path, e);
        }
    }

    private static long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "cannot stat " + path, e);
        }
    }

    private static String extractUnreleasedSection(String content) {
        int start = content.indexOf("## [Unreleased]");
        if (start < 0) {
            return "";
        }
        int next = content.indexOf("\n## [", start + 1);
        if (next < 0) {
            return content.substring(start);
        }
        return content.substring(start, next);
    }
}
