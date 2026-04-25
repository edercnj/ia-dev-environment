package dev.iadev.quality;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that the 4 plan skills reference
 * {@code planning-standards-kp} after story-0056-0006.
 *
 * <p>Each skill MUST mention the KP in its Prerequisites
 * or Integration Notes so that plan authors know to
 * consult the RA9 contract before generating artifacts.</p>
 */
@DisplayName("PlanSkillsRa9ReferenceTest")
class PlanSkillsRa9ReferenceTest {

    private static final Path PLAN_SKILLS_ROOT = Path.of(
            "src", "main", "resources",
            "targets", "claude", "skills", "core", "plan");

    @ParameterizedTest(name = "{0} references planning-standards-kp")
    @ValueSource(strings = {
        "x-epic-create",
        "x-epic-decompose",
        "x-story-plan",
        "x-task-plan"
    })
    @DisplayName("planSkill_referencesKp_inPrerequisitesOrIntegration")
    void planSkill_referencesKp_inPrerequisitesOrIntegration(
            String skillName) throws IOException {
        Path skillFile = PLAN_SKILLS_ROOT
                .resolve(skillName)
                .resolve("SKILL.md")
                .toAbsolutePath();

        assertThat(skillFile)
                .as("SKILL.md for %s must exist", skillName)
                .exists();

        String content = Files.readString(
                skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("Skill %s MUST reference planning-standards-kp"
                        + " after story-0056-0006", skillName)
                .contains("planning-standards-kp");
    }

    @ParameterizedTest(name = "{0} has RA9 section guidance")
    @ValueSource(strings = {
        "x-epic-create",
        "x-epic-decompose",
        "x-story-plan",
        "x-task-plan"
    })
    @DisplayName("planSkill_hasRa9SectionGuidance")
    void planSkill_hasRa9SectionGuidance(
            String skillName) throws IOException {
        Path skillFile = PLAN_SKILLS_ROOT
                .resolve(skillName)
                .resolve("SKILL.md")
                .toAbsolutePath();

        String content = Files.readString(
                skillFile, StandardCharsets.UTF_8);

        assertThat(content)
                .as("Skill %s must mention Packages section (RA9 §2)"
                        + " or Decision Rationale (RA9 §8)", skillName)
                .satisfiesAnyOf(
                    c -> assertThat(c).contains("Packages"),
                    c -> assertThat(c).contains("Decision Rationale"),
                    c -> assertThat(c).contains("RA9")
                );
    }
}
