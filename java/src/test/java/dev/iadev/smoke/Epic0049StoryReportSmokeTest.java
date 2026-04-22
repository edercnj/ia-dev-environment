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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for STORY-0049-0015
 * (x-internal-story-report skill + template).
 *
 * <p>Validates the end-to-end contract introduced by
 * story-0049-0015. The skill is an
 * {@code x-internal-*} type ({@code visibility: internal},
 * {@code user-invocable: false}) — the output location is
 * determined by downstream stories (0049-0019) that
 * extend the generator's two-level category resolver. This
 * story delivers ONLY the source-of-truth artefacts:</p>
 * <ul>
 *   <li>{@code java/src/main/resources/targets/claude/skills
 *       /core/internal/plan/x-internal-story-report/
 *       SKILL.md}</li>
 *   <li>{@code java/src/main/resources/targets/claude/skills
 *       /core/internal/plan/x-internal-story-report/
 *       references/full-protocol.md}</li>
 *   <li>{@code java/src/main/resources/shared/templates/
 *       _TEMPLATE-STORY-COMPLETION-REPORT.md}</li>
 * </ul>
 *
 * <p>Pipeline-level assertions focus on the
 * {@code _TEMPLATE-STORY-COMPLETION-REPORT.md} file, which
 * is part of the 21-template {@link
 * dev.iadev.application.assembler.PlanTemplatesAssembler}
 * set and MUST ship to every bundled profile under
 * {@code .claude/templates/}.</p>
 *
 * @see SmokeTestBase
 * @see SmokeProfiles
 */
@DisplayName("Epic0049StoryReportSmokeTest — "
        + "x-internal-story-report source-of-truth + "
        + "template shipment")
class Epic0049StoryReportSmokeTest extends SmokeTestBase {

    private static final String SKILL_SOURCE_REL =
            "java/src/main/resources/targets/claude/skills/"
                    + "core/internal/plan/"
                    + "x-internal-story-report";

    private static final String TEMPLATE_SOURCE_REL =
            "java/src/main/resources/shared/templates/"
                    + "_TEMPLATE-STORY-COMPLETION-REPORT.md";

    private static final String TEMPLATE_NAME =
            "_TEMPLATE-STORY-COMPLETION-REPORT.md";

    private static final List<String> REQUIRED_FRONTMATTER =
            List.of(
                    "name: x-internal-story-report",
                    "visibility: internal",
                    "user-invocable: false",
                    "category: internal-plan");

    private static final List<String> REQUIRED_TEMPLATE_SECTIONS =
            List.of(
                    "## Executive Summary",
                    "## Tasks",
                    "## Pull Request",
                    "## Review Findings",
                    "## Coverage Delta");

    /**
     * Source-of-truth assertion: the SKILL.md lives at the
     * canonical path under {@code core/internal/plan/} with
     * the three-anchor frontmatter contract from Rule 22.
     */
    @Test
    @DisplayName("smoke_sourceOfTruth_skillMdExists — "
            + "SKILL.md lives at canonical path with "
            + "internal frontmatter")
    void smoke_sourceOfTruth_skillMdExists()
            throws IOException {
        Path projectRoot = resolveProjectRoot();
        Path skillMd = projectRoot
                .resolve(SKILL_SOURCE_REL)
                .resolve("SKILL.md");

        assertThat(Files.isRegularFile(skillMd))
                .as("SKILL.md must exist at %s", skillMd)
                .isTrue();

        String content = Files.readString(
                skillMd, StandardCharsets.UTF_8);
        for (String marker : REQUIRED_FRONTMATTER) {
            assertThat(content)
                    .as("SKILL.md must declare '%s'",
                            marker)
                    .contains(marker);
        }
    }

    @Test
    @DisplayName("smoke_sourceOfTruth_referencesFolderExists "
            + "— full-protocol.md present under "
            + "references/")
    void smoke_sourceOfTruth_referencesFolderExists()
            throws IOException {
        Path projectRoot = resolveProjectRoot();
        Path ref = projectRoot
                .resolve(SKILL_SOURCE_REL)
                .resolve("references/full-protocol.md");

        assertThat(Files.isRegularFile(ref))
                .as("references/full-protocol.md must "
                        + "exist at %s", ref)
                .isTrue();
        assertThat(Files.size(ref))
                .as("full-protocol.md must not be empty")
                .isGreaterThan(0L);
    }

    @Test
    @DisplayName("smoke_sourceOfTruth_templateExists — "
            + "_TEMPLATE-STORY-COMPLETION-REPORT.md "
            + "lives in shared/templates/")
    void smoke_sourceOfTruth_templateExists()
            throws IOException {
        Path projectRoot = resolveProjectRoot();
        Path template = projectRoot.resolve(
                TEMPLATE_SOURCE_REL);

        assertThat(Files.isRegularFile(template))
                .as("template must exist at %s", template)
                .isTrue();

        String content = Files.readString(
                template, StandardCharsets.UTF_8);
        for (String section : REQUIRED_TEMPLATE_SECTIONS) {
            assertThat(content)
                    .as("template must carry '%s'",
                            section)
                    .contains(section);
        }
        assertThat(content)
                .as("template must carry {{#each tasks}} "
                        + "loop for x-internal-report-write")
                .contains("{{#each tasks}}")
                .contains("{{/each}}");
        assertThat(content)
                .as("template must carry {{#each findings}} "
                        + "loop")
                .contains("{{#each findings}}");
    }

    /**
     * Pipeline-level assertion: the template is part of the
     * registered plan-template set and ships to every
     * profile under {@code .claude/templates/}.
     */
    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_templateShipsToAllProfiles — "
                    + "_TEMPLATE-STORY-COMPLETION-REPORT.md"
                    + " present under .claude/templates/")
    void smoke_templateShipsToAllProfiles(String profile)
            throws IOException {
        runPipeline(profile);
        Path templateFile = getOutputDir(profile)
                .resolve(".claude/templates/"
                        + TEMPLATE_NAME);

        assertThat(Files.isRegularFile(templateFile))
                .as("profile %s: template must exist at %s",
                        profile, templateFile)
                .isTrue();
        assertThat(Files.size(templateFile))
                .as("profile %s: template must not be empty",
                        profile)
                .isGreaterThan(0L);

        String content = Files.readString(
                templateFile, StandardCharsets.UTF_8);
        for (String section : REQUIRED_TEMPLATE_SECTIONS) {
            assertThat(content)
                    .as("profile %s: template must "
                            + "carry '%s'", profile, section)
                    .contains(section);
        }
    }

    /**
     * Resolves the project root by walking upward until the
     * pom.xml of the generator module is found. Tests run
     * from the {@code java/} sub-module working directory,
     * so the parent of {@code pom.xml} is the repo root.
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
                    && "java".equals(candidate.getFileName()
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
