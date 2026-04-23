package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for STORY-0049-0022 (planning VCS lifecycle
 * — P1-P5 versioning for {@code x-story-create},
 * {@code x-task-plan}, {@code x-story-plan}, and
 * {@code x-epic-orchestrate}). Final story of EPIC-0049.
 *
 * <p>Each of the 4 planning skills must expose the
 * canonical P1-P5 markers introduced by EPIC-0049 /
 * RULE-007:</p>
 *
 * <ol>
 *   <li>P1 — Detect Worktree Context
 *       ({@code x-git-worktree detect-context})</li>
 *   <li>P2 — Ensure epic branch
 *       ({@code x-internal-epic-branch-ensure})</li>
 *   <li>Existing phases (unchanged)</li>
 *   <li>P4 — Planning commit
 *       ({@code x-planning-commit} — or alias section
 *       when delegated to the pre-existing
 *       {@code x-git-commit} flow in
 *       {@code x-task-plan})</li>
 *   <li>P5 — Push to origin
 *       ({@code x-git-push --branch epic/...})</li>
 * </ol>
 *
 * <p>The tests assert source-of-truth presence at
 * {@code java/src/main/resources/targets/claude/skills
 * /core/plan/&lt;skill&gt;/SKILL.md}. The generated
 * {@code .claude/} outputs are locked by golden-file
 * regeneration in a separate dimension.</p>
 *
 * @see SmokeTestBase
 */
@DisplayName("PlanningVcsSmokeTest — story-0049-0022 "
        + "P1-P5 versioning across the 4 planning skills")
class PlanningVcsSmokeTest extends SmokeTestBase {

    private static final String SKILL_ROOT =
            "java/src/main/resources/targets/claude/skills/"
                    + "core/plan";

    private static final List<String> TARGET_SKILLS = List.of(
            "x-story-create",
            "x-task-plan",
            "x-story-plan",
            "x-epic-orchestrate");

    private static final List<String> P1_MARKERS = List.of(
            "Step P1",
            "x-git-worktree",
            "detect-context");

    private static final List<String> P2_MARKERS = List.of(
            "Step P2",
            "x-internal-epic-branch-ensure");

    private static final List<String> P5_MARKERS = List.of(
            "Step P5",
            "x-git-push");

    @Test
    @DisplayName("smoke_frontmatter_allFourSkillsAllowSkillTool"
            + " — Rule 13 INLINE-SKILL requires "
            + "allowed-tools: Skill")
    void smoke_frontmatter_allFourSkillsAllowSkillTool()
            throws IOException {
        Path projectRoot = resolveProjectRoot();
        for (String skill : TARGET_SKILLS) {
            Path skillMd = projectRoot
                    .resolve(SKILL_ROOT)
                    .resolve(skill)
                    .resolve("SKILL.md");
            assertThat(Files.isRegularFile(skillMd))
                    .as("SKILL.md must exist at %s",
                            skillMd)
                    .isTrue();
            String content = Files.readString(
                    skillMd, StandardCharsets.UTF_8);
            assertThat(content)
                    .as("%s frontmatter must declare "
                            + "`Skill` in allowed-tools",
                            skill)
                    .contains("Skill");
        }
    }

    @ParameterizedTest
    @MethodSource("allFourSkills")
    @DisplayName("smoke_p1_detectContextPresent")
    void smoke_p1_detectContextPresent(String skill)
            throws IOException {
        assertAllMarkersPresent(skill, P1_MARKERS);
    }

    @ParameterizedTest
    @MethodSource("allFourSkills")
    @DisplayName("smoke_p2_epicBranchEnsurePresent")
    void smoke_p2_epicBranchEnsurePresent(String skill)
            throws IOException {
        assertAllMarkersPresent(skill, P2_MARKERS);
    }

    @ParameterizedTest
    @MethodSource("allFourSkills")
    @DisplayName("smoke_p5_gitPushPresent")
    void smoke_p5_gitPushPresent(String skill)
            throws IOException {
        assertAllMarkersPresent(skill, P5_MARKERS);
    }

    @ParameterizedTest
    @MethodSource("allFourSkills")
    @DisplayName("smoke_dryRun_noopContractDocumented — "
            + "--dry-run must be reachable in each skill "
            + "and disable commit")
    void smoke_dryRun_noopContractDocumented(String skill)
            throws IOException {
        String content = readSkill(skill);
        assertThat(content)
                .as("%s must accept --dry-run flag",
                        skill)
                .contains("--dry-run");
        assertThat(content)
                .as("%s must document dry-run skipping "
                        + "commit/push", skill)
                .containsAnyOf(
                        "dry-run, skipping commit",
                        "dry-run, skipping push",
                        "no-ops");
    }

    @Test
    @DisplayName("smoke_storyPlan_delegatesTaskPlanWith"
            + "NoCommit — x-story-plan must pass "
            + "--no-commit to x-task-plan (batch contract)")
    void smoke_storyPlan_delegatesTaskPlanWithNoCommit()
            throws IOException {
        String content = readSkill("x-story-plan");
        assertThat(content)
                .as("x-story-plan Phase 4b must invoke "
                        + "x-task-plan with --no-commit "
                        + "(EPIC-0049 batch contract)")
                .contains("--task-file")
                .contains("--no-commit");
    }

    @Test
    @DisplayName("smoke_storyPlan_usesPlanningCommitBatch "
            + "— Step P4 aggregates all artifacts "
            + "(no N+1)")
    void smoke_storyPlan_usesPlanningCommitBatch()
            throws IOException {
        String content = readSkill("x-story-plan");
        assertThat(content)
                .as("x-story-plan must delegate its P4 "
                        + "commit to x-planning-commit")
                .contains("x-planning-commit");
        assertThat(content)
                .as("x-story-plan batch commit subject "
                        + "must mention 'add planning "
                        + "artifacts'")
                .contains("add planning artifacts");
    }

    @Test
    @DisplayName("smoke_epicOrchestrate_commitsPerWave — "
            + "Step P4 commits per completed wave "
            + "('planning orchestration cycle')")
    void smoke_epicOrchestrate_commitsPerWave()
            throws IOException {
        String content = readSkill("x-epic-orchestrate");
        assertThat(content)
                .as("x-epic-orchestrate wave commit "
                        + "must go through "
                        + "x-planning-commit")
                .contains("x-planning-commit");
        assertThat(content)
                .as("x-epic-orchestrate wave commit "
                        + "subject must match the "
                        + "Gherkin scenario")
                .contains(
                        "planning orchestration cycle");
        assertThat(content)
                .as("x-epic-orchestrate must propagate "
                        + "--no-commit to child "
                        + "x-story-plan invocations")
                .contains("x-story-plan")
                .contains("--no-commit");
    }

    @Test
    @DisplayName("smoke_storyCreate_commitSubjectMentions"
            + "UserStory — docs scope + add user story")
    void smoke_storyCreate_commitSubjectMentionsUserStory()
            throws IOException {
        String content = readSkill("x-story-create");
        assertThat(content)
                .as("x-story-create P4 subject must "
                        + "reference the docs scope and "
                        + "story-authoring subject")
                .contains("--scope docs")
                .contains("add user story");
    }

    @Test
    @DisplayName("smoke_taskPlan_hasPhase54CommitAlias — "
            + "P4 step is an alias over the existing "
            + "Phase 5.4 planning-status commit gate")
    void smoke_taskPlan_hasPhase54CommitAlias()
            throws IOException {
        String content = readSkill("x-task-plan");
        assertThat(content)
                .as("x-task-plan must retain the "
                        + "existing x-git-commit gate "
                        + "(story-0049-0017) under the "
                        + "P4 alias")
                .contains("x-git-commit");
        assertThat(content)
                .as("x-task-plan must explicitly "
                        + "document Step P4 (alias) so "
                        + "the P1-P5 convention is "
                        + "readable end-to-end")
                .contains("Step P4");
    }

    private void assertAllMarkersPresent(
            String skill, List<String> markers)
            throws IOException {
        String content = readSkill(skill);
        for (String marker : markers) {
            assertThat(content)
                    .as("%s must contain marker '%s'",
                            skill, marker)
                    .contains(marker);
        }
    }

    private String readSkill(String skill)
            throws IOException {
        Path projectRoot = resolveProjectRoot();
        Path skillMd = projectRoot
                .resolve(SKILL_ROOT)
                .resolve(skill)
                .resolve("SKILL.md");
        assertThat(Files.isRegularFile(skillMd))
                .as("SKILL.md must exist at %s",
                        skillMd)
                .isTrue();
        return Files.readString(
                skillMd, StandardCharsets.UTF_8);
    }

    private static Stream<String> allFourSkills() {
        return TARGET_SKILLS.stream();
    }

    /**
     * Resolves the project root by walking upward until
     * the pom.xml of the generator module is found. Tests
     * run from the {@code java/} sub-module working
     * directory, so the parent of {@code pom.xml} is the
     * repo root.
     */
    private static Path resolveProjectRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        Path candidate = cwd;
        for (int i = 0; i < 6; i++) {
            Path pomXml = candidate.resolve("pom.xml");
            Path javaSub = candidate.resolve("java");
            if (Files.isRegularFile(pomXml)
                    && Files.isDirectory(javaSub)) {
                return candidate;
            }
            if (Files.isRegularFile(pomXml)
                    && candidate.getFileName() != null
                    && "java".equals(candidate
                            .getFileName()
                            .toString())) {
                return candidate.getParent();
            }
            candidate = candidate.getParent();
            if (candidate == null) {
                break;
            }
        }
        return cwd;
    }
}
