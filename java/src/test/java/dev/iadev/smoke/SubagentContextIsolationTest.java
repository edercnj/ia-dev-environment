package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates RULE-005 enforcement: every subagent
 * prompt template in orchestrator skills must contain
 * the explicit "CONTEXT ISOLATION" instruction block.
 *
 * <p>Subagents must receive only metadata (IDs, paths,
 * flags) and never inline source code, diffs, or
 * knowledge pack content.</p>
 *
 * @see <a href="plans/epic-0030/story-0030-0005.md">
 *     story-0030-0005</a>
 */
@DisplayName("SubagentContextIsolationTest")
class SubagentContextIsolationTest {

    private static final String SKILLS_DIR =
            "src/main/resources/targets/claude"
                    + "/skills/core";

    private static final String ISOLATION_MARKER =
            "CONTEXT ISOLATION:";

    /**
     * Skills that contain subagent dispatch prompts.
     */
    private static final List<String> ORCHESTRATOR_SKILLS =
            List.of(
                    "x-epic-implement",
                    "x-story-implement",
                    "x-review",
                    "x-epic-orchestrate");

    private Path resolveSkillPath(String skillName) {
        Path flat = Path.of(
                SKILLS_DIR, skillName, "SKILL.md");
        if (Files.exists(flat)) {
            return flat;
        }
        // Hierarchical SoT (story-0036-0002): search
        // category subfolders for a matching skill.
        Path categoryMatch =
                searchCategorySubfolders(skillName);
        return categoryMatch != null ? categoryMatch : flat;
    }

    private Path searchCategorySubfolders(String skillName) {
        Path core = Path.of(SKILLS_DIR);
        if (!Files.isDirectory(core)) {
            return null;
        }
        try (var stream = Files.list(core)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(p -> p.resolve(skillName)
                            .resolve("SKILL.md"))
                    .filter(Files::exists)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private Path resolveSkillReferencePath(
            String skillName, String refFile) {
        Path skillMd = resolveSkillPath(skillName);
        return skillMd.getParent()
                .resolve("references").resolve(refFile);
    }

    private String readSkillContent(String skillName)
            throws IOException {
        Path path = resolveSkillPath(skillName);
        return Files.readString(
                path, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("x-epic-implement")
    class EpicImplement {

        @Test
        @DisplayName("Sequential dispatch prompt "
                + "contains CONTEXT ISOLATION marker")
        void sequentialPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-epic-implement");
            assertPromptSectionContains(
                    content,
                    "Subagent Dispatch",
                    ISOLATION_MARKER);
        }

        @Test
        @DisplayName("Conflict resolution prompt "
                + "contains CONTEXT ISOLATION marker")
        void conflictResolutionPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-epic-implement");
            assertPromptSectionContains(
                    content,
                    "Conflict Resolution Specialist",
                    ISOLATION_MARKER);
        }

        @Test
        @DisplayName("Integrity gate prompt "
                + "contains CONTEXT ISOLATION marker")
        void gatePrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-epic-implement");
            assertPromptSectionContains(
                    content,
                    "Integrity Gate Validator",
                    ISOLATION_MARKER);
        }
    }

    @Nested
    @DisplayName("x-story-implement")
    class DevLifecycle {

        @Test
        @DisplayName("Implementation plan prompt "
                + "contains CONTEXT ISOLATION marker")
        void implPlanPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-story-implement");
            assertPromptSectionContains(
                    content,
                    "Senior Architect",
                    ISOLATION_MARKER);
        }

        @Test
        @DisplayName("Security assessment prompt "
                + "contains CONTEXT ISOLATION marker")
        void securityPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-story-implement");
            assertPromptSectionContains(
                    content,
                    "Security Engineer",
                    ISOLATION_MARKER);
        }

        @Test
        @DisplayName("Event schema design prompt "
                + "contains CONTEXT ISOLATION marker")
        void eventSchemaPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-story-implement");
            assertPromptSectionContains(
                    content,
                    "Event Engineer",
                    ISOLATION_MARKER);
        }

        @Test
        @DisplayName("Compliance assessment prompt "
                + "contains CONTEXT ISOLATION marker")
        void compliancePrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-story-implement");
            assertPromptSectionContains(
                    content,
                    "compliance impact",
                    ISOLATION_MARKER);
        }
    }

    @Nested
    @DisplayName("x-review")
    class Review {

        @Test
        @DisplayName("Specialist review prompt "
                + "contains CONTEXT ISOLATION marker")
        void specialistPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-review");
            assertPromptSectionContains(
                    content,
                    "Parallel Reviews",
                    ISOLATION_MARKER);
        }
    }

    @Nested
    @DisplayName("x-epic-orchestrate")
    class EpicPlan {

        @Test
        @DisplayName("Story plan dispatch prompt "
                + "contains CONTEXT ISOLATION marker")
        void storyPlanPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-epic-orchestrate");
            assertPromptSectionContains(
                    content,
                    "Subagent Dispatch",
                    ISOLATION_MARKER);
        }
    }

    @Nested
    @DisplayName("Skill Invocation Enforcement")
    class SkillInvocationEnforcement {

        private static final String SKILL_INVOCATION_MARKER =
                "Skill(skill:";

        @Test
        @DisplayName("x-epic-implement prompt "
                + "invokes x-story-implement via Skill tool")
        void epicImplement_invokesLifecycleViaSkill()
                throws IOException {
            String content = readSkillContent(
                    "x-epic-implement");
            assertThat(content)
                    .as("Subagent prompt must invoke "
                            + "x-story-implement via Skill tool")
                    .contains(
                            "Skill(skill: \"x-story-implement\"");
        }

        @Test
        @DisplayName("x-epic-orchestrate prompt "
                + "invokes x-story-plan via Skill tool")
        void epicPlan_invokesStoryPlanViaSkill()
                throws IOException {
            String content = readSkillContent(
                    "x-epic-orchestrate");
            assertThat(content)
                    .as("Subagent dispatch must invoke "
                            + "x-story-plan via Skill tool")
                    .contains(
                            "Skill(skill: \"x-story-plan\"");
        }

        @Test
        @DisplayName("x-story-implement planning phases "
                + "invoke x-threat-model via Skill tool")
        void lifecycle_invokesThreatModelViaSkill()
                throws IOException {
            Path refPath = resolveSkillReferencePath(
                    "x-story-implement",
                    "planning-phases.md");
            String content = Files.readString(
                    refPath, StandardCharsets.UTF_8);
            assertThat(content)
                    .as("Phase 1E must invoke "
                            + "x-threat-model via Skill tool")
                    .contains(
                            "Skill(skill: \"x-threat-model\"");
        }
    }

    @Nested
    @DisplayName("Cross-cutting")
    class CrossCutting {

        @Test
        @DisplayName("All orchestrator skills contain "
                + "at least one CONTEXT ISOLATION marker")
        void allSkills_containIsolationMarker()
                throws IOException {
            for (String skill : ORCHESTRATOR_SKILLS) {
                String content = readSkillContent(skill);
                assertThat(content)
                        .as("Skill %s must contain "
                                        + "CONTEXT ISOLATION "
                                        + "marker",
                                skill)
                        .contains(ISOLATION_MARKER);
            }
        }
    }

    /**
     * Verifies that the section of the content
     * containing the given role also contains the
     * isolation marker within the bounded section
     * starting at the role marker.
     */
    private static void assertPromptSectionContains(
            String content,
            String roleMarker,
            String expected) {
        int roleIndex = content.indexOf(roleMarker);
        assertThat(roleIndex)
                .as("Role '%s' must exist in template",
                        roleMarker)
                .isGreaterThanOrEqualTo(0);
        int nextSectionIndex = content.indexOf(
                "\n## ", roleIndex + roleMarker.length());
        String section = nextSectionIndex < 0
                ? content.substring(roleIndex)
                : content.substring(
                        roleIndex, nextSectionIndex);
        assertThat(section)
                .as("Section with role '%s' must "
                                + "contain '%s'",
                        roleMarker, expected)
                .contains(expected);
    }
}
