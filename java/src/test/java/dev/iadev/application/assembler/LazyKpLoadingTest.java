package dev.iadev.application.assembler;

import dev.iadev.testutil.SkillContentReader;
import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0030-0003 (Lazy Knowledge Pack Loading).
 *
 * <p>Validates that subagent prompts in orchestrator skills
 * reference specific KP reference files instead of entire
 * SKILL.md files with "then read its references" pattern.
 *
 * <p>Acceptance criteria:
 * <ul>
 *   <li>No prompt references "SKILL.md -> then read its
 *       references"</li>
 *   <li>Security subagent references specific files under
 *       security/references/</li>
 *   <li>Compliance subagent references specific framework
 *       files</li>
 *   <li>Max 3 KP reference reads per subagent</li>
 * </ul>
 */
@DisplayName("Lazy KP Loading (story-0030-0003)")
class LazyKpLoadingTest {

    @Nested
    @DisplayName("x-story-implement — subagent prompts")
    class LifecyclePrompts {

        @Test
        @DisplayName("Phase 1E: security subagent does not"
                + " reference security/SKILL.md")
        void lifecycle_securitySubagent_noFullSkillRef(
                @TempDir Path tempDir) throws IOException {
            String content = generateLifecycle(tempDir);

            assertThat(content).doesNotContain(
                    "knowledge/security/index.md` -> "
                            + "then read its references");
        }

        @Test
        @DisplayName("Phase 1E: security subagent references"
                + " application-security.md")
        void lifecycle_securitySubagent_refsAppSecurity(
                @TempDir Path tempDir) throws IOException {
            String content = generateLifecycle(tempDir);

            assertThat(content).contains(
                    "knowledge/security/"
                            + "application-security.md");
        }

        @Test
        @DisplayName("Phase 1E: security subagent references"
                + " security-principles.md")
        void lifecycle_securitySubagent_refsSecPrinciples(
                @TempDir Path tempDir) throws IOException {
            String content = generateLifecycle(tempDir);

            assertThat(content).contains(
                    "knowledge/security/"
                            + "security-principles.md");
        }

        @Test
        @DisplayName("Phase 1F: compliance subagent does not"
                + " reference compliance/SKILL.md with"
                + " read-all pattern")
        void lifecycle_complianceSubagent_noFullSkillRef(
                @TempDir Path tempDir) throws IOException {
            String content = generateLifecycle(tempDir);

            assertThat(content).doesNotContain(
                    "knowledge/compliance.md` -> "
                            + "then read its references");
        }

        @Test
        @DisplayName("Phase 1F: compliance subagent"
                + " references specific framework files")
        void lifecycle_complianceSubagent_refsFramework(
                @TempDir Path tempDir) throws IOException {
            String content = generateLifecycle(tempDir);

            assertThat(content).contains(
                    "knowledge/compliance.md");
        }

        @Test
        @DisplayName("No subagent prompt uses the"
                + " read-all-references pattern")
        void lifecycle_allSubagents_noReadAllPattern(
                @TempDir Path tempDir) throws IOException {
            String content = generateLifecycle(tempDir);

            assertThat(content).doesNotContain(
                    "then read its references");
        }

        /**
         * Reads the concatenated SKILL.md + references/full-protocol.md
         * body for x-story-implement.
         *
         * <p>Story-0047-0002 (flipped orientation per ADR-0012)
         * moved the detailed subagent prompts from SKILL.md to
         * references/full-protocol.md. The lazy-KP-loading invariant
         * (story-0030-0003) applies to the full prompt text wherever
         * it lives; reading both files preserves the original intent
         * of this test suite across the slim/full split.</p>
         */
        private String generateLifecycle(Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder().build();
            assembler.assemble(
                    config,
                    new TemplateEngine(),
                    outputDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-story-implement/SKILL.md");
            Path fullProtocol = outputDir.resolve(
                    "skills/x-story-implement/references/"
                            + "full-protocol.md");
            String body = Files.readString(skillMd);
            if (Files.isRegularFile(fullProtocol)) {
                body = body + "\n"
                        + Files.readString(fullProtocol);
            }
            return body;
        }
    }

    @Nested
    @DisplayName("x-story-plan — subagent prompts")
    class StoryPlanPrompts {

        @Test
        @DisplayName("Security subagent does not reference"
                + " security/SKILL.md with read-all pattern")
        void storyPlan_securitySubagent_noFullSkillRef(
                @TempDir Path tempDir) throws IOException {
            String content = generateStoryPlan(tempDir);

            assertThat(content).doesNotContain(
                    "knowledge/security/index.md` "
                            + "then read its references");
        }

        @Test
        @DisplayName("Security subagent references specific"
                + " security reference files")
        void storyPlan_securitySubagent_refsSpecific(
                @TempDir Path tempDir) throws IOException {
            String content = generateStoryPlan(tempDir);

            assertThat(content).contains(
                    "knowledge/security/"
                            + "application-security.md");
            assertThat(content).contains(
                    "knowledge/security/"
                            + "security-principles.md");
        }

        @Test
        @DisplayName("Compliance instruction references"
                + " specific framework files")
        void storyPlan_compliance_refsFramework(
                @TempDir Path tempDir) throws IOException {
            String content = generateStoryPlan(tempDir);

            assertThat(content).contains(
                    "knowledge/compliance.md");
        }

        @Test
        @DisplayName("No subagent prompt uses the"
                + " read-all-references pattern")
        void storyPlan_allSubagents_noReadAllPattern(
                @TempDir Path tempDir) throws IOException {
            String content = generateStoryPlan(tempDir);

            assertThat(content).doesNotContain(
                    "then read its references");
        }

        private String generateStoryPlan(Path tempDir)
                throws IOException {
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);
            SkillsAssembler assembler =
                    new SkillsAssembler();
            ProjectConfig config =
                    TestConfigBuilder.builder().build();
            assembler.assemble(
                    config,
                    new TemplateEngine(),
                    outputDir);
            return SkillContentReader
                    .readSkillWithReferences(outputDir, "x-story-plan");
        }
    }
}
