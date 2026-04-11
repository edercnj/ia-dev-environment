package dev.iadev.application.assembler;

import dev.iadev.testutil.TestConfigBuilder;

import dev.iadev.template.TemplateEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for story-0013-0010: x-ops-incident skill for
 * interactive incident response with severity checklists.
 *
 * <p>Validates that the x-ops-incident skill template is
 * generated correctly with proper frontmatter, severity
 * definitions, communication templates, workflow steps,
 * and error handling.</p>
 */
@DisplayName("x-ops-incident Skill")
class IncidentSkillTest {

    @Nested
    @DisplayName("Claude SKILL.md — Frontmatter")
    class ClaudeFrontmatter {

        @Test
        @DisplayName("x-ops-incident SKILL.md exists after"
                + " assembly")
        void assemble_incident_skillMdExists(
                @TempDir Path tempDir)
                throws IOException {
            Path outputDir = generateOutput(tempDir);
            Path skillMd = outputDir.resolve(
                    "skills/x-ops-incident/SKILL.md");
            assertThat(skillMd).exists();
        }

        @Test
        @DisplayName("frontmatter contains name:"
                + " x-ops-incident")
        void assemble_incident_hasName(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("name: x-ops-incident");
        }

        @Test
        @DisplayName("frontmatter contains"
                + " user-invocable: true")
        void assemble_incident_hasUserInvocable(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).satisfiesAnyOf(
                    c -> assertThat(c).contains(
                            "user-invocable: true"),
                    c -> assertThat(c).contains(
                            "user-invocable: \"true\""));
        }

        @Test
        @DisplayName("frontmatter contains argument-hint"
                + " with SEV levels")
        void assemble_incident_hasArgumentHint(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("argument-hint:")
                    .contains("SEV1")
                    .contains("SEV2")
                    .contains("SEV3")
                    .contains("SEV4");
        }

        @Test
        @DisplayName("frontmatter contains allowed-tools")
        void assemble_incident_hasAllowedTools(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("allowed-tools:");
        }

        @Test
        @DisplayName("frontmatter contains description"
                + " with incident response")
        void assemble_incident_hasDescription(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("description:")
                    .contains("incident");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Severity Definitions")
    class SeverityDefinitions {

        @Test
        @DisplayName("contains SEV1 Critical definition")
        void assemble_incident_hasSev1(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SEV1")
                    .contains("Critical");
        }

        @Test
        @DisplayName("contains SEV2 High definition")
        void assemble_incident_hasSev2(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SEV2")
                    .contains("High");
        }

        @Test
        @DisplayName("contains SEV3 Medium definition")
        void assemble_incident_hasSev3(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SEV3")
                    .contains("Medium");
        }

        @Test
        @DisplayName("contains SEV4 Low definition")
        void assemble_incident_hasSev4(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("SEV4")
                    .contains("Low");
        }

        @Test
        @DisplayName("contains response time requirements")
        void assemble_incident_hasResponseTimes(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("15 min")
                    .contains("30 min");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Workflow")
    class Workflow {

        @Test
        @DisplayName("contains 6-step workflow")
        void assemble_incident_hasWorkflowSteps(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("CLASSIFY")
                    .contains("LOAD")
                    .contains("GUIDE")
                    .contains("COMMUNICATE")
                    .contains("POSTMORTEM")
                    .contains("TRACK");
        }

        @Test
        @DisplayName("contains Detection Triage"
                + " Mitigation Resolution flow")
        void assemble_incident_hasResponseFlow(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Detection")
                    .contains("Triage")
                    .contains("Mitigation")
                    .contains("Resolution");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Communication"
            + " Templates")
    class CommunicationTemplates {

        @Test
        @DisplayName("contains status page template")
        void assemble_incident_hasStatusPage(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Status Page");
        }

        @Test
        @DisplayName("contains Slack template")
        void assemble_incident_hasSlack(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Slack");
        }

        @Test
        @DisplayName("contains email template")
        void assemble_incident_hasEmail(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("Email");
        }

        @Test
        @DisplayName("contains update frequency"
                + " per severity")
        void assemble_incident_hasUpdateFrequency(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("30 min")
                    .contains("1 hour")
                    .contains("4 hours");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Integration Notes")
    class IntegrationNotes {

        @Test
        @DisplayName("references sre-engineer agent")
        void assemble_incident_refsSreEngineer(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("sre-engineer");
        }

        @Test
        @DisplayName("references sre-practices KP")
        void assemble_incident_refsSrePracticesKp(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("sre-practices");
        }

        @Test
        @DisplayName("references postmortem template")
        void assemble_incident_refsPostmortemTemplate(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("postmortem");
        }
    }

    @Nested
    @DisplayName("Claude SKILL.md — Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("contains invalid severity message")
        void assemble_incident_hasInvalidSeverity(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content).contains(
                    "Invalid severity");
        }

        @Test
        @DisplayName("contains severity omitted"
                + " interactive flow")
        void assemble_incident_hasSeverityOmitted(
                @TempDir Path tempDir)
                throws IOException {
            String content =
                    generateClaudeContent(tempDir);
            assertThat(content)
                    .contains("impact");
        }
    }

    @Nested
    @DisplayName("SkillGroupRegistry —"
            + " git-troubleshooting Group")
    class RegistryGitTroubleshootingGroup {

        @Test
        @DisplayName("git-troubleshooting group"
                + " contains x-ops-incident")
        void register_gitTroubleshooting_containsIncident() {
            assertThat(SkillGroupRegistry.SKILL_GROUPS
                    .get("git-troubleshooting"))
                    .contains("x-ops-incident");
        }
    }

    private Path generateOutput(Path tempDir)
            throws IOException {
        Path outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        SkillsAssembler assembler =
                new SkillsAssembler();
        assembler.assemble(
                TestConfigBuilder.minimal(),
                new TemplateEngine(), outputDir);
        return outputDir;
    }

    private String generateClaudeContent(Path tempDir)
            throws IOException {
        Path outputDir = generateOutput(tempDir);
        return Files.readString(
                outputDir.resolve(
                        "skills/x-ops-incident/SKILL.md"),
                StandardCharsets.UTF_8);
    }

}
