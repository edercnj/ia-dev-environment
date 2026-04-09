package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates Progressive Skill Loading (Slim Mode)
 * for story-0030-0006.
 *
 * <p>Acceptance criteria:
 * <ul>
 *   <li>x-commit, x-format, x-lint, x-tdd have
 *       "## Slim Mode" section</li>
 *   <li>Each slim section is <= 50 lines</li>
 *   <li>Orchestrators reference slim mode when
 *       invoking chain skills</li>
 *   <li>Slim mode sections contain essential
 *       information for programmatic invocation</li>
 * </ul>
 */
@DisplayName("Slim Mode Sections (story-0030-0006)")
class SlimModeSectionTest extends SmokeTestBase {

    private static final int MAX_SLIM_LINES = 50;

    private static final String SLIM_HEADER =
            "## Slim Mode";

    private static final List<String> SLIM_SKILLS =
            List.of(
                    "x-commit",
                    "x-format",
                    "x-lint",
                    "x-tdd");

    static Stream<String> slimSkills() {
        return SLIM_SKILLS.stream();
    }

    @Nested
    @DisplayName("Slim Mode presence")
    class SlimModePresence {

        @ParameterizedTest(name = "[{0}] has slim mode")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".SlimModeSectionTest"
                        + "#slimSkills")
        @DisplayName("skill contains ## Slim Mode"
                + " section")
        void skillHasSlimMode_section_present(
                String skill) throws IOException {
            runPipeline("java-quarkus");
            String content = readSkillContent(
                    "java-quarkus", skill);
            assertThat(content)
                    .as("[%s] must contain '%s'",
                            skill, SLIM_HEADER)
                    .contains(SLIM_HEADER);
        }
    }

    @Nested
    @DisplayName("Slim Mode line count")
    class SlimModeLineCount {

        @ParameterizedTest(
                name = "[{0}] slim <= 50 lines")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".SlimModeSectionTest"
                        + "#slimSkills")
        @DisplayName("slim section is <= 50 lines")
        void slimSection_lineCount_withinLimit(
                String skill) throws IOException {
            runPipeline("java-quarkus");
            String content = readSkillContent(
                    "java-quarkus", skill);
            int lineCount = countSlimLines(content);
            assertThat(lineCount)
                    .as("[%s] slim section must be"
                                    + " <= %d lines"
                                    + " (actual: %d)",
                            skill, MAX_SLIM_LINES,
                            lineCount)
                    .isLessThanOrEqualTo(MAX_SLIM_LINES);
        }
    }

    @Nested
    @DisplayName("Slim Mode essential content")
    class SlimModeContent {

        @ParameterizedTest(
                name = "[{0}] slim has workflow")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".SlimModeSectionTest"
                        + "#slimSkills")
        @DisplayName("slim section has essential"
                + " content for invocation")
        void slimSection_content_hasEssentials(
                String skill) throws IOException {
            runPipeline("java-quarkus");
            String slim = extractSlimSection(
                    readSkillContent(
                            "java-quarkus", skill));
            assertThat(slim)
                    .as("[%s] slim must not be empty",
                            skill)
                    .isNotBlank();
        }
    }

    @Nested
    @DisplayName("Orchestrator slim references")
    class OrchestratorReferences {

        @ParameterizedTest(
                name = "[{0}] references slim mode")
        @ValueSource(strings = {
                "x-dev-lifecycle", "x-tdd"})
        @DisplayName("orchestrator references slim"
                + " mode when invoking chain skills")
        void orchestrator_referencesSlimMode(
                String orchestrator) throws IOException {
            runPipeline("java-quarkus");
            String content = readSkillContent(
                    "java-quarkus", orchestrator);
            assertThat(content)
                    .as("[%s] must reference"
                                    + " 'Slim Mode'",
                            orchestrator)
                    .containsIgnoringCase("slim mode");
        }
    }

    @Nested
    @DisplayName("Multi-profile consistency")
    class MultiProfile {

        @ParameterizedTest(name = "[{0}]")
        @MethodSource(
                "dev.iadev.smoke"
                        + ".SlimModeSectionTest"
                        + "#representativeProfiles")
        @DisplayName("all profiles have slim mode"
                + " in x-commit")
        void multiProfile_xCommitHasSlimMode(
                String profile) throws IOException {
            runPipeline(profile);
            String content = readSkillContent(
                    profile, "x-commit");
            assertThat(content)
                    .as("[%s] x-commit must contain"
                                    + " '%s'",
                            profile, SLIM_HEADER)
                    .contains(SLIM_HEADER);
        }
    }

    static Stream<String> representativeProfiles() {
        return Stream.of(
                "go-gin",
                "java-quarkus",
                "kotlin-ktor",
                "python-fastapi",
                "rust-axum",
                "typescript-nestjs");
    }

    private String readSkillContent(
            String profile, String skillName)
            throws IOException {
        return SmokeTestValidators.readSkillWithRefs(
                getOutputDir(profile), skillName);
    }

    /**
     * Extracts the "## Slim Mode" section from a
     * skill's content. Returns the section from the
     * header to the next ## header or end of file.
     */
    static String extractSlimSection(String content) {
        int start = content.indexOf(SLIM_HEADER);
        if (start < 0) {
            return "";
        }
        int sectionStart = start + SLIM_HEADER.length();
        int nextHeader = content.indexOf(
                "\n## ", sectionStart);
        if (nextHeader < 0) {
            return content.substring(sectionStart).trim();
        }
        return content.substring(
                sectionStart, nextHeader).trim();
    }

    /**
     * Counts lines in the Slim Mode section,
     * including the header line.
     */
    static int countSlimLines(String content) {
        int start = content.indexOf(SLIM_HEADER);
        if (start < 0) {
            return 0;
        }
        int nextHeader = content.indexOf(
                "\n## ", start + SLIM_HEADER.length());
        String section;
        if (nextHeader < 0) {
            section = content.substring(start);
        } else {
            section = content.substring(start, nextHeader);
        }
        return (int) section.lines().count();
    }
}
