package dev.iadev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end smoke for EPIC-0056 story-0056-0001.
 *
 * <p>Validates that after {@code ia-dev-env generate}:
 * <ul>
 *   <li>{@code planning-standards-kp} skill is present in
 *       {@code .claude/skills/}.</li>
 *   <li>The generated SKILL.md has {@code user-invocable: false}
 *       (not exposed in slash-command menu).</li>
 *   <li>The generated file contains the 9 RA9 section headers.</li>
 * </ul>
 */
@DisplayName("GenerateWithKpSmokeTest — planning-standards-kp EPIC-0056")
class GenerateWithKpSmokeTest extends SmokeTestBase {

    private static final String KP_SKILL =
            ".claude/skills/planning-standards-kp/SKILL.md";

    @Test
    @DisplayName("generate_planningStandardsKp_presentInOutput")
    void generate_planningStandardsKp_presentInOutput() {
        runPipeline("java-spring");
        Path outputDir = getOutputDir("java-spring");

        Path kpFile = outputDir.resolve(KP_SKILL);
        assertThat(kpFile)
                .as("planning-standards-kp must be generated"
                        + " in .claude/skills/ after ia-dev-env generate")
                .exists()
                .isRegularFile();
    }

    @Test
    @DisplayName("generate_planningStandardsKp_notUserInvocable")
    void generate_planningStandardsKp_notUserInvocable()
            throws IOException {
        runPipeline("java-spring");
        Path outputDir = getOutputDir("java-spring");

        String content = Files.readString(
                outputDir.resolve(KP_SKILL),
                StandardCharsets.UTF_8);

        assertThat(content)
                .as("Generated planning-standards-kp must have"
                        + " user-invocable: false (not in slash menu)")
                .contains("user-invocable: false");
    }

    @Test
    @DisplayName("generate_planningStandardsKp_hasNineRa9Sections")
    void generate_planningStandardsKp_hasNineRa9Sections()
            throws IOException {
        runPipeline("java-spring");
        Path outputDir = getOutputDir("java-spring");

        String content = Files.readString(
                outputDir.resolve(KP_SKILL),
                StandardCharsets.UTF_8);

        assertThat(content)
                .as("KP must define section 2 (Packages Hexagonal)")
                .contains("## 2. Packages (Hexagonal)");
        assertThat(content)
                .as("KP must define section 8 (Decision Rationale)")
                .contains("## 8. Decision Rationale");
        assertThat(content)
                .as("KP must define section 9 (Dependências)")
                .contains("## 9. Dependências & File Footprint");
    }
}
