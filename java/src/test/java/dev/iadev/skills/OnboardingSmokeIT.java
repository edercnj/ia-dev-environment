package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke boundary test for story-0040-0009 TASK-0040-0009-003: simulates a
 * contributor onboarding flow that (1) copies {@code _TEMPLATE-SKILL.md},
 * (2) performs the minimal plug-and-play substitution documented in its
 * Telemetry section, and (3) proves the resulting SKILL.md has at least
 * one functional phase.start/phase.end pair. Also caps wall-clock at
 * 30s (story §8 acceptance) to keep the test under CI budget.
 *
 * <p>The simulation is intentionally deterministic — it does not spawn a
 * Claude Code session; instead it exercises the template machinery that
 * a new skill author would exercise manually, demonstrating the edit
 * surface is trivial.
 */
class OnboardingSmokeIT {

    private static final Path TEMPLATE = Paths.get(
            "src/main/resources/shared/templates/_TEMPLATE-SKILL.md");

    private static final Pattern PHASE_START = Pattern.compile(
            "telemetry-phase\\.sh\\s+start\\s+\\S+\\s+\\S+");

    private static final Pattern PHASE_END = Pattern.compile(
            "telemetry-phase\\.sh\\s+end\\s+\\S+\\s+\\S+");

    @Test
    @DisplayName("onboarding_copyTemplateAndSubstitute_producesValidSkillFile")
    void onboarding_copyTemplateAndSubstitute_producesValidSkillFile(@TempDir Path tmp)
            throws IOException {
        long start = System.nanoTime();

        // 1. Copy the template into a fresh skill directory (contributor action)
        Path skillDir = tmp.resolve("skills/x-new-contrib-skill");
        Files.createDirectories(skillDir);
        Path skillMd = skillDir.resolve("SKILL.md");
        String templateBody = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        Files.writeString(skillMd, templateBody, StandardCharsets.UTF_8);

        // 2. Minimal substitution: pick a skill name + one phase (contributor action)
        String substituted = Files.readString(skillMd, StandardCharsets.UTF_8)
                .replace("{{SKILL_NAME}}", "x-new-contrib-skill")
                .replace("<phase-name>", "preparation");
        Files.writeString(skillMd, substituted, StandardCharsets.UTF_8);

        // 3. Assert the resulting skill has at least one functional start/end pair
        String finalBody = Files.readString(skillMd, StandardCharsets.UTF_8);

        Matcher starts = PHASE_START.matcher(finalBody);
        Matcher ends = PHASE_END.matcher(finalBody);
        assertThat(starts.find())
                .as("substituted skill must contain at least one phase.start call")
                .isTrue();
        assertThat(ends.find())
                .as("substituted skill must contain at least one phase.end call")
                .isTrue();
        assertThat(finalBody)
                .as("substituted skill must reference rule 13 for the markers contract")
                .contains("13-skill-invocation-protocol.md");

        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        assertThat(elapsedMs)
                .as("onboarding simulation must finish well within the 30s budget "
                        + "declared in story-0040-0009 §8")
                .isLessThan(30_000L);
    }

    @Test
    @DisplayName("onboarding_templateBody_containsAllThreeMarkerShapes")
    void onboarding_templateBody_containsAllThreeMarkerShapes() throws IOException {
        // Guards against regression of §3.1: template must document phase,
        // subagent and mcp shapes so contributors have a one-stop reference.
        String body = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
        List<String> required = List.of(
                "telemetry-phase.sh start",
                "telemetry-phase.sh end",
                "subagent-start",
                "subagent-end",
                "mcp-start",
                "mcp-end");
        for (String needle : required) {
            assertThat(body)
                    .as("_TEMPLATE-SKILL.md must document marker shape '%s'", needle)
                    .contains(needle);
        }
    }
}
