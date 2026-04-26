package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
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
 * Smoke test for the EPIC-0057 story-0057-0004 retrofit: verifies
 * that the 6 canonical orchestrators carry the
 * {@code MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24)} marker
 * around the previously-prose sub-skill invocations identified in
 * the EPIC-0053 post-mortem.
 *
 * <p>Reads from the canonical reference golden ({@code java-spring}
 * profile) — independent of pipeline regeneration; runs in the
 * standard smoke suite.</p>
 */
@DisplayName("MandatoryMarkersSmokeTest — 6 canonical orchestrators carry MANDATORY markers")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX path resolution; mirrors sibling smoke tests.")
class MandatoryMarkersSmokeTest {

    private static final String GOLDEN_BASE =
            "java/src/test/resources/golden/java-spring/.claude/skills";

    static Stream<RetrofitTarget> targets() {
        return Stream.of(
                new RetrofitTarget("x-story-implement",
                        "Rule 24"),
                new RetrofitTarget("x-task-implement",
                        "Rule 24"),
                new RetrofitTarget("x-release",
                        "Rule 24"),
                new RetrofitTarget("x-epic-implement",
                        "Rule 24"),
                new RetrofitTarget("x-owasp-scan",
                        "Rule 24"),
                new RetrofitTarget("x-review",
                        "Rule 24"));
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("targets")
    @DisplayName("orchestrator carries the MANDATORY marker (Rule 24)")
    void orchestrator_carriesMandatoryMarker(RetrofitTarget t)
            throws IOException {
        Path skill = repoRoot()
                .resolve(GOLDEN_BASE)
                .resolve(t.skillName())
                .resolve("SKILL.md");
        assertThat(skill)
                .as("golden SKILL.md for %s must exist", t.skillName())
                .exists();

        String body = Files.readString(skill, StandardCharsets.UTF_8);
        assertThat(body)
                .as("%s must declare MANDATORY TOOL CALL marker referencing %s",
                        t.skillName(), t.ruleRef())
                .containsPattern(
                        "MANDATORY TOOL CALL — NON-NEGOTIABLE \\("
                                + java.util.regex.Pattern.quote(t.ruleRef())
                                + "[^\\)]*\\)");
    }

    @Test
    @DisplayName("x-review marker covers the specialist invocation block")
    void xReview_markerPrecedesSpecialistBlock() throws IOException {
        Path skill = repoRoot()
                .resolve(GOLDEN_BASE)
                .resolve("x-review/SKILL.md");
        String body = Files.readString(skill, StandardCharsets.UTF_8);

        int markerIdx = body.indexOf("MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 24)");
        int firstSpecialistIdx = body.indexOf("Skill(skill: \"x-review-qa\"");
        assertThat(markerIdx)
                .as("MANDATORY marker must be present in x-review")
                .isGreaterThanOrEqualTo(0);
        assertThat(firstSpecialistIdx)
                .as("specialist x-review-qa invocation must be present")
                .isGreaterThan(markerIdx);
    }

    @Test
    @DisplayName("all 6 retrofitted goldens count at least 1 NON-NEGOTIABLE marker")
    void all_sixGoldens_carryAtLeastOneMarker() throws IOException {
        List<String> skillNames = targets()
                .map(RetrofitTarget::skillName)
                .toList();

        for (String name : skillNames) {
            Path skill = repoRoot()
                    .resolve(GOLDEN_BASE)
                    .resolve(name)
                    .resolve("SKILL.md");
            String body = Files.readString(skill, StandardCharsets.UTF_8);
            long markerCount = body.lines()
                    .filter(l -> l.contains(
                            "MANDATORY TOOL CALL — NON-NEGOTIABLE"))
                    .count();
            assertThat(markerCount)
                    .as("%s must carry at least 1 MANDATORY marker",
                            name)
                    .isGreaterThanOrEqualTo(1);
        }
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java")
                ? cwd.getParent()
                : cwd;
    }

    private record RetrofitTarget(String skillName, String ruleRef) {
        @Override
        public String toString() {
            return skillName;
        }
    }
}
