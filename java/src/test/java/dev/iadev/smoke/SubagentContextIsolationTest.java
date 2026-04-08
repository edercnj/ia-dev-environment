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
                    "x-dev-epic-implement",
                    "x-dev-lifecycle",
                    "x-review",
                    "x-epic-plan");

    private Path resolveSkillPath(String skillName) {
        return Path.of(SKILLS_DIR, skillName, "SKILL.md");
    }

    private String readSkillContent(String skillName)
            throws IOException {
        Path path = resolveSkillPath(skillName);
        return Files.readString(
                path, StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("x-dev-epic-implement")
    class EpicImplement {

        @Test
        @DisplayName("Sequential dispatch prompt "
                + "contains CONTEXT ISOLATION marker")
        void sequentialPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-dev-epic-implement");
            assertThat(content)
                    .as("Section 1.4 sequential prompt"
                            + " must include "
                            + "CONTEXT ISOLATION")
                    .contains(ISOLATION_MARKER);
        }

        @Test
        @DisplayName("Conflict resolution prompt "
                + "contains CONTEXT ISOLATION marker")
        void conflictResolutionPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-dev-epic-implement");
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
                    "x-dev-epic-implement");
            assertPromptSectionContains(
                    content,
                    "Integrity Gate Validator",
                    ISOLATION_MARKER);
        }
    }

    @Nested
    @DisplayName("x-dev-lifecycle")
    class DevLifecycle {

        @Test
        @DisplayName("Implementation plan prompt "
                + "contains CONTEXT ISOLATION marker")
        void implPlanPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-dev-lifecycle");
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
                    "x-dev-lifecycle");
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
                    "x-dev-lifecycle");
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
                    "x-dev-lifecycle");
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
                    "Engineer",
                    ISOLATION_MARKER);
        }
    }

    @Nested
    @DisplayName("x-epic-plan")
    class EpicPlan {

        @Test
        @DisplayName("Story plan dispatch prompt "
                + "contains CONTEXT ISOLATION marker")
        void storyPlanPrompt_containsIsolation()
                throws IOException {
            String content = readSkillContent(
                    "x-epic-plan");
            assertThat(content)
                    .as("x-epic-plan subagent dispatch"
                            + " must include "
                            + "CONTEXT ISOLATION")
                    .contains(ISOLATION_MARKER);
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
     * isolation marker.
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
        assertThat(content)
                .as("Template with role '%s' must "
                                + "contain '%s'",
                        roleMarker, expected)
                .contains(expected);
    }
}
